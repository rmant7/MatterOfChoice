# # your_flask_app/utils.py
# import logging
# import json
# from pathlib import Path
# from datetime import datetime
# import ast
# import google.generativeai as genai  # Import Gemini module
# from dotenv import load_dotenv  # Import dotenv for loading .env file
# import os

# # Load environment variables

# import uuid  # Import uuid for unique ID generation

# import json
# from pathlib import Path






# load_dotenv()

# # Load the API key from environment variables
# GENAI_API_KEY = os.getenv('GOOGLE_API_KEY')
# if not GENAI_API_KEY:
#     raise ValueError("GOOGLE_API_KEY is not set in the .env file.")
# genai.configure(api_key=GENAI_API_KEY)

# # Define the base directory for the Flask app
# BASE_DIR = Path(__file__).resolve().parent

# # Load prompts from JSON file
# PROMPTS_FILE = BASE_DIR / "prompts.json"
# if not PROMPTS_FILE.exists():
#     raise FileNotFoundError(f"Prompts file not found at {PROMPTS_FILE}")

# with open(PROMPTS_FILE, 'r', encoding='utf-8') as f:
#     prompts = json.load(f)

# timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
# logger = lambda msg: print(f"{datetime.now()} - {msg}")  # Simple logger

# # Directory to save generated cases and images
# output_path = BASE_DIR / 'output/game'
# output_path.mkdir(parents=True, exist_ok=True)

# # Function to get a response from Gemini
# def get_response_gemini(prompt: str) -> str:
#     try:
#         # logger(f"Generating response for prompt: {prompt[:50]}...")
#         model = genai.GenerativeModel('gemini-2.5-flash')  # Update the model name
#         response = model.generate_content(prompt)

#         if response and response._result.candidates:
#             content = response._result.candidates[0].content.parts[0].text.strip()
#             # logger(f"Received response: {content[:5000]}...")
#             return content
#         else:
#             logger("Empty or invalid response from Gemini.")
#             return ""
#     except Exception as err:
#         logger(f"Error generating response: {err}")
#         return ""

# # Function to clean the response from code block formatting
# def clean_response(response: str) -> str:
#     if response.startswith("```python") and response.endswith("```"):
#         return response[len("```python"): -len("```")].strip()
#     return response

# # Function to extract the list from the cleaned response
# def extract_list(code: str) -> str:
#     try:
#         code = code.replace("\'", "\\'")  # Replacing every \' with \\'
#         tree = ast.parse(code)
#         for node in ast.walk(tree):
#             if isinstance(node, (ast.Assign, ast.Expr)):  # Handle both named and unnamed lists
#                 if isinstance(node.value, ast.List):
#                     extracted_list = ast.literal_eval(node.value)  # safely convert the list
#                     return json.dumps(extracted_list)  # Convert to JSON
#         return None
#     except (SyntaxError, ValueError, TypeError) as e:
#         logger(f"Error parsing or evaluating Python code: {e}")
#         logger(f"Problematic code snippet: {code}")
#         return None


# def gen_cases(language: str, difficulty: str, age: int, output_dir: Path, subject: str, question_type: str, subtype: str, conversation_data=None, sex: str = 'unspecified'):
#     logger = logging.getLogger('my_app')
#     # logger.debug(f"gen_cases function called with parameters: language={language}, age={age}, subject={subject}, difficulty={difficulty}, question_type={question_type}, subtype={subtype}, sex={sex}, conversation_data={conversation_data}")

#     try:
#         if conversation_data is None:
#             if question_type == 'behavioral':
#                 prompt = f"""{prompts['cases']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
#             elif question_type == 'study':
#                 prompt = f"""{prompts['study']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
#             elif question_type == 'hiring':
#                 prompt = f"""{prompts['hiring']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
#             # logger.debug(f"Initial prompt generated: {prompt}")
#         else:
#             # conversation_data now has the structure: { "data": { "cases": [ ... ] } }
#             previous_cases = conversation_data.get('data', {}).get('cases', [])
#             if not isinstance(previous_cases, list) or not previous_cases:
#                 logger.warning("No previous cases found in conversation data. Returning None.")
#                 return None, conversation_data

#             # Construct a clear textual representation of all previous cases
#             previous_turn_summary = ""
#             for case in previous_cases:
#                 user_choice = case.get('user_answer')
#                 previous_case = case.get('case', "Unknown Case")
#                 previous_options = case.get('options', [])
#                 previous_turn_summary += f"Previous Case: {previous_case}\nThe person was presented with the following options:\n"
#                 for option in previous_options:
#                     previous_turn_summary += f"- Option {option['number']}: {option['option']}\n"
#                 previous_turn_summary += f"\nThe person selected option {user_choice}.\n\n"

#             if question_type == 'behavioral':
#                 prompt = f"""{prompts['cases']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
#             elif question_type == 'study':
#                 prompt = f"""{prompts['study']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
#             elif question_type == 'hiring':
#                 prompt = f"""{prompts['hiring']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
#             logger.debug(f"Follow-up prompt generated: {prompt}")

#         response = get_response_gemini(prompt)
#         # logger.debug(f"Gemini response: {response[:200]}...")

#         if not response:
#             logger.error("Gemini API returned an empty response.")
#             return None, conversation_data

#         cleaned_response = clean_response(response)
#         list_content = extract_list(cleaned_response)
#         # logger.debug(f"Extracted list content: {list_content}")

#         if list_content:
#             try:
#                 parsed = json.loads(list_content)
#                 # Do not force a single case—keep all cases returned.
#                 if not isinstance(parsed, list):
#                     parsed = [parsed]
#                 required_keys = ['case', 'options', 'optimal']
#                 new_cases = []
#                 for case in parsed:
#                     if not all(key in case for key in required_keys):
#                         logger.error(f"Missing required keys in parsed response: {required_keys}")
#                         return None, conversation_data
#                     # Generate a unique case ID
#                     case_id = str(uuid.uuid4())
#                     case_data = {
#                         'case_id': case_id,
#                         'case': case['case'], 
#                         'optimal': case['optimal'], 
#                         'options': []
#                     }
#                     for option_data in case['options']:
#                         option_id = str(uuid.uuid4())
#                         option_item = {**option_data, 'option_id': option_id}
#                         case_data['options'].append(option_item)
#                     new_cases.append(case_data)

#                 # print(f"new cases lenth: ", len(new_cases))
#                 max = 3
#                 attempts = 0

#                 while attempts < 1 and len(new_cases) < max:
#                     response = get_response_gemini(prompt)
#                     cleaned_response = clean_response(response)
#                     list_content = extract_list(cleaned_response)
#                     # logger.debug(f"Extracted list content: {list_content}")
#                     attempts += 1
#                     if list_content:
#                             parsed = json.loads(list_content)
#                             # Do not force a single case—keep all cases returned.
#                             if not isinstance(parsed, list):
#                                 parsed = [parsed]
#                             required_keys = ['case', 'options', 'optimal']
#                             for case in parsed:
#                                 if not all(key in case for key in required_keys):
#                                     logger.error(f"Missing required keys in parsed response: {required_keys}")
#                                     return None, conversation_data
#                                 # Generate a unique case ID
#                                 case_id = str(uuid.uuid4())
#                                 case_data = {
#                                     'case_id': case_id,
#                                     'case': case['case'], 
#                                     'optimal': case['optimal'], 
#                                     'options': []
#                                 }
#                                 for option_data in case['options']:
#                                     option_id = str(uuid.uuid4())
#                                     option_item = {**option_data, 'option_id': option_id}
#                                     case_data['options'].append(option_item)
#                                 new_cases.append(case_data)

#                 # Save conversation data to JSON file with the structure: { "data": { "cases": [ ... ] } }
#                 conversation_filepath = output_dir / "conversation.json"
#                 conversation_data = {'data': {'cases': new_cases}}
#                 with open(conversation_filepath, 'w') as f:
#                     json.dump(conversation_data, f, indent=4)
#                 logger.debug(f"Conversation data saved to {conversation_filepath}")

#                 return new_cases, conversation_data
#             except json.JSONDecodeError as e:
#                 logger.error(f"Failed to parse JSON response: {e}")
#                 return None, conversation_data
#         else:
#             logger.error("No valid list content found in response.")
#             return None, conversation_data

#     except Exception as e:
#         logger.exception(f"An unexpected error occurred in gen_cases: {e}")
#         return None, conversation_data
# your_flask_app/utils.py
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
        
   