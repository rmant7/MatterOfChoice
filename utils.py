import logging
import json
from pathlib import Path
from datetime import datetime
import ast
import google.generativeai as genai  # Import Gemini module
from dotenv import load_dotenv  # Import dotenv for loading .env file
import os
import uuid  # Import uuid for unique ID generation

# Load environment variables
load_dotenv()

# Load the API key from environment variables
GENAI_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GENAI_API_KEY:
    raise ValueError("GOOGLE_API_KEY is not set in the .env file.")
genai.configure(api_key=GENAI_API_KEY)

# Define the base directory for the Flask app
BASE_DIR = Path(__file__).resolve().parent

# Load prompts from JSON file
PROMPTS_FILE = BASE_DIR / "prompts.json"
if not PROMPTS_FILE.exists():
    raise FileNotFoundError(f"Prompts file not found at {PROMPTS_FILE}")

with open(PROMPTS_FILE, 'r', encoding='utf-8') as f:
    prompts = json.load(f)

timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
logger = lambda msg: print(f"{datetime.now()} - {msg}")  # Simple logger

# Directory to save generated cases and images
output_path = BASE_DIR / 'output/game'
output_path.mkdir(parents=True, exist_ok=True)


def get_response_gemini(prompt: str) -> str:
    """
    Generates a response from the Gemini API using the recommended and robust method.
    """
    try:
        # FIX: Use a valid and current model name. 'gemini-1.5-flash-latest' is fast and capable.
        model = genai.GenerativeModel('gemini-2.5-flash') 
        response = model.generate_content(prompt)

        # Use the public `response.text` attribute to safely get the full text.
        if response and hasattr(response, 'text') and response.text:
            content = response.text.strip()
            return content
        else:
            # This can happen if the response was blocked for safety reasons.
            logger("Received an empty or blocked response from Gemini.")
            logger(f"Prompt that may have caused blocking: {prompt[:300]}...")
            if response and hasattr(response, 'prompt_feedback'):
                logger(f"Response details: {response.prompt_feedback}")
            return ""
    except Exception as err:
        logger(f"Error generating response from Gemini: {err}")
        return ""

# The rest of your utils.py file can remain as is.
# ... (clean_response, extract_list, gen_cases functions)
# ... (rest of your utils.py file) ...
# The extract_list function from the previous answer is still correct and should be kept.
def clean_response(response: str) -> str:
    """
    Cleans the response from markdown code block formatting (e.g., ```python or ```json).
    """
    if response.startswith("```") and response.endswith("```"):
        # Find the first newline to remove the language specifier (e.g., ```python)
        first_line_end = response.find('\n')
        if first_line_end != -1:
            # Strip the language line and the final ```
            return response[first_line_end + 1: -len("```")].strip()
        else: # Handle case with no language specifier, e.g. ```[...]```
            return response[len("```"): -len("```")].strip()
    return response.strip()

def extract_list(response: str) -> str:
    """
    Extracts a Python list literal from a string, safely evaluates it,
    and returns it as a JSON string. This is robust against responses
    that include surrounding code, text, or comments.
    """
    try:
        # Find the start of the first list and the end of the last list.
        # This correctly handles nested lists by finding the outermost container.
        start_index = response.find('[')
        end_index = response.rfind(']')

        if start_index == -1 or end_index == -1 or end_index < start_index:
            logger(f"Could not find a valid list structure '[]' in the response.")
            return None

        # Extract the substring that looks like a list
        list_str = response[start_index : end_index + 1]
        
        # Use ast.literal_eval for safe evaluation of the string.
        try:
            evaluated_list = ast.literal_eval(list_str)
            if isinstance(evaluated_list, list):
                return json.dumps(evaluated_list)  # Convert to a JSON string for consistency
            else:
                logger(f"Evaluated literal is not a list, but a {type(evaluated_list)}")
                return None
        except (ValueError, SyntaxError, TypeError, MemoryError) as e:
            logger(f"Error parsing list literal with ast.literal_eval: {e}")
            logger(f"Problematic list snippet: {list_str[:500]}...")
            return None

    except Exception as e:
        logger(f"An unexpected error occurred in extract_list: {e}")
        return None



#
# =======================================================================================
# === FIX: Drastically speed up the gen_cases function ==================================
# =======================================================================================
#
def gen_cases(language: str, difficulty: str, age: int, output_dir: Path, subject: str, question_type: str, subtype: str, conversation_data=None, sex: str = 'unspecified'):
    """
    Generates a full set of cases in a SINGLE API call for maximum speed.
    """
    log = logging.getLogger('my_app') # Use the app's logger for consistency
    num_cases_to_generate = 3 # Define how many cases we want in one go.

    try:
        # Instruction to add to the prompt to get a full list in one shot.
        generation_instruction = f"Generate a JSON list containing exactly {num_cases_to_generate} unique cases that follow the required structure."

        # Simplified prompt building
        prompt_key_map = {
            'behavioral': 'cases',
            'study': 'study',
            'hiring': 'hiring'
        }
        prompt_template = prompts.get(prompt_key_map.get(question_type))
        if not prompt_template:
            log.error(f"Invalid question_type: {question_type}. No prompt found.")
            return None, conversation_data
        
        # Build the final prompt with the clear instruction for multiple cases.
        prompt = f"""{prompt_template} {generation_instruction} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
        log.debug(f"Generating {num_cases_to_generate} cases with a single API call.")

        # --- SINGLE, EFFICIENT API CALL ---
        response = get_response_gemini(prompt)

        if not response:
            log.error("Gemini API returned an empty response.")
            return None, conversation_data

        cleaned_response = clean_response(response)
        list_content = extract_list(cleaned_response)

        if list_content:
            parsed = json.loads(list_content)
            if not isinstance(parsed, list):
                parsed = [parsed] # Handle if it only returns one case for some reason

            # Ensure we don't have more cases than requested.
            if len(parsed) > num_cases_to_generate:
                log.warning(f"Gemini returned {len(parsed)} cases, truncating to {num_cases_to_generate}.")
                parsed = parsed[:num_cases_to_generate]

            # --- NO MORE SLOW WHILE LOOP! ---
            new_cases = []
            for case_item in parsed:
                if not all(key in case_item for key in ['case', 'options', 'optimal']):
                    log.warning(f"Skipping malformed case from Gemini: {case_item}")
                    continue
                
                case_id = str(uuid.uuid4())
                case_data = {
                    'case_id': case_id,
                    'case': case_item['case'], 
                    'optimal': case_item['optimal'], 
                    'options': []
                }
                for option_data in case_item['options']:
                    option_id = str(uuid.uuid4())
                    option_item = {**option_data, 'option_id': option_id}
                    case_data['options'].append(option_item)
                new_cases.append(case_data)

            # Note: We are not saving the conversation file here anymore.
            # The background job in app.py will handle that.
            return new_cases, conversation_data
        else:
            log.error("No valid list content found in Gemini response after cleaning.")
            return None, conversation_data

    except Exception as e:
        log.exception(f"An unexpected error occurred in gen_cases: {e}")
        return None, conversation_data
        
   