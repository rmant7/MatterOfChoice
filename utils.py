# your_flask_app/utils.py
import logging
import json
from pathlib import Path
from datetime import datetime
import ast
import re
import google.generativeai as genai  # Import Gemini module
from dotenv import load_dotenv  # Import dotenv for loading .env file
import os
from openai import OpenAI

try:
    # Newer mistralai SDK
    from mistralai import Mistral as MistralClient
except Exception:
    try:
        # Older mistralai SDK
        from mistralai.client import MistralClient  # type: ignore
    except Exception:
        MistralClient = None

# Load environment variables

import uuid  # Import uuid for unique ID generation

import json
from pathlib import Path






load_dotenv()

# Load the API key from environment variables
GENAI_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GENAI_API_KEY:
    raise ValueError("GOOGLE_API_KEY is not set in the .env file.")
genai.configure(api_key=GENAI_API_KEY)

MISTRAL_API_KEY = os.getenv('MISTRAL_API_KEY')
XAI_API_KEY = os.getenv('XAI_API_KEY')

# Define the base directory for the Flask app
BASE_DIR = Path(__file__).resolve().parent

# Load prompts from JSON file
PROMPTS_FILE = BASE_DIR / "prompts.json"
if not PROMPTS_FILE.exists():
    raise FileNotFoundError(f"Prompts file not found at {PROMPTS_FILE}")

with open(PROMPTS_FILE, 'r', encoding='utf-8') as f:
    prompts = json.load(f)

timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
utils_logger = logging.getLogger('my_app')

# Directory to save generated cases and images
output_path = BASE_DIR / 'output/game'
output_path.mkdir(parents=True, exist_ok=True)

# Function to get a response from Gemini
def get_response_gemini(prompt: str) -> str:
    model_names = [
        'gemini-2.5-flash',
        'gemini-flash-latest',
    ]

    for model_name in model_names:
        try:
            model = genai.GenerativeModel(model_name)
            response = model.generate_content(
                prompt,
                request_options={"timeout": 60}
            )

            # Prefer the stable SDK accessor when available.
            content = (getattr(response, "text", None) or "").strip()
            if content:
                return content

            # Fallback: safely inspect candidate parts when response.text is empty.
            candidates = getattr(response, "candidates", None) or []
            for candidate in candidates:
                candidate_content = getattr(candidate, "content", None)
                parts = getattr(candidate_content, "parts", None) or []
                for part in parts:
                    part_text = (getattr(part, "text", None) or "").strip()
                    if part_text:
                        return part_text

            prompt_feedback = getattr(response, "prompt_feedback", None)
            block_reason = getattr(prompt_feedback, "block_reason", None) if prompt_feedback else None
            finish_reasons = [str(getattr(c, "finish_reason", "unknown")) for c in candidates]
            utils_logger.warning(
                "Gemini returned empty content. "
                f"model={model_name}, block_reason={block_reason}, finish_reasons={finish_reasons}, "
                f"prompt_preview={prompt[:180]!r}"
            )
        except Exception as err:
            utils_logger.exception(
                "Error generating response from Gemini. "
                f"model={model_name}, error={err}, timeout=60s, prompt_preview={prompt[:180]!r}"
            )

    return ""


# Function to get a response from Mistral
def get_response_mistral(prompt: str) -> str:
    if not MISTRAL_API_KEY:
        utils_logger.error("MISTRAL_API_KEY is not set.")
        return ""
    if MistralClient is None:
        utils_logger.error("Mistral SDK is not installed or import path is incompatible.")
        return ""

    client = MistralClient(api_key=MISTRAL_API_KEY)
    model_names = ['mistral-small-latest', 'mistral-medium-latest']
    for model_name in model_names:
        try:
            # Support both SDK styles:
            # - New: client.chat.complete(...)
            # - Old: client.chat(...)
            if hasattr(client, "chat") and hasattr(client.chat, "complete"):
                response = client.chat.complete(
                    model=model_name,
                    messages=[{"role": "user", "content": prompt}],
                    timeout_ms=60000
                )
            else:
                response = client.chat(
                    model=model_name,
                    messages=[{"role": "user", "content": prompt}]
                )
            content = (response.choices[0].message.content or "").strip()
            if content:
                return content
            utils_logger.warning(f"Mistral returned empty content. model={model_name}")
        except Exception as err:
            utils_logger.exception(
                f"Error generating response from Mistral. model={model_name}, error={err}"
            )
    return ""


# Function to get a response from Grok (xAI)
def get_response_grok(prompt: str) -> str:
    if not XAI_API_KEY:
        utils_logger.error("XAI_API_KEY is not set.")
        return ""
    client = OpenAI(
        api_key=XAI_API_KEY,
        base_url="https://api.x.ai/v1",
        timeout=15
    )
    model_names = ['grok-3', 'grok-3-mini', 'grok-2-1212']
    for model_name in model_names:
        try:
            response = client.chat.completions.create(
                model=model_name,
                messages=[{"role": "user", "content": prompt}]
            )
            content = (response.choices[0].message.content or "").strip()
            if content:
                return content
            utils_logger.warning(f"Grok returned empty content. model={model_name}")
        except Exception as err:
            utils_logger.exception(
                f"Error generating response from Grok. model={model_name}, error={err}"
            )
    return ""


# Dispatcher: route to the correct provider based on the model parameter
def get_response(prompt: str, model: str = 'gemini') -> str:
    if model == 'mistral':
        response = get_response_mistral(prompt)
        if response:
            return response
        utils_logger.warning("Mistral failed or returned empty response. Falling back to Grok.")
        return get_response_grok(prompt)
    elif model == 'grok':
        response = get_response_grok(prompt)
        if response:
            return response
        # If Grok fails (e.g., no credits/quota), gracefully fail over to Mistral.
        utils_logger.warning("Grok failed or returned empty response. Falling back to Mistral.")
        return get_response_mistral(prompt)

    # Default Gemini path with automatic provider fallback for quota/timeouts.
    response = get_response_gemini(prompt)
    if response:
        return response
    utils_logger.warning("Gemini failed or returned empty response. Falling back to Grok.")

    response = get_response_grok(prompt)
    if response:
        return response

    utils_logger.warning("Grok failed or returned empty response. Falling back to Mistral.")
    return get_response_mistral(prompt)


# Function to clean the response from code block formatting
def clean_response(response: str) -> str:
    if not response:
        return ""

    # If the model wraps output in fenced code blocks, prefer the first fenced block.
    fence_match = re.search(r"```(?:python|json)?\s*(.*?)\s*```", response, flags=re.DOTALL | re.IGNORECASE)
    if fence_match:
        return fence_match.group(1).strip()
    return response.strip()

# Function to extract the list from the cleaned response
def extract_list(code: str) -> str:
    if not code:
        return None

    candidates = []
    raw = code.strip()
    candidates.append(raw)

    # Try parsing the fenced code body if present.
    fence_match = re.search(r"```(?:python|json)?\s*(.*?)\s*```", raw, flags=re.DOTALL | re.IGNORECASE)
    if fence_match:
        candidates.append(fence_match.group(1).strip())

    # Try parsing the first bracketed list section when prose surrounds it.
    first_bracket = raw.find("[")
    last_bracket = raw.rfind("]")
    if first_bracket != -1 and last_bracket != -1 and last_bracket > first_bracket:
        candidates.append(raw[first_bracket:last_bracket + 1].strip())

    for candidate in candidates:
        if not candidate:
            continue

        # First try strict JSON.
        try:
            parsed = json.loads(candidate)
            if isinstance(parsed, list):
                return json.dumps(parsed)
        except (json.JSONDecodeError, TypeError):
            pass

        # Then try Python literal list syntax.
        try:
            parsed = ast.literal_eval(candidate)
            if isinstance(parsed, list):
                return json.dumps(parsed)
        except (SyntaxError, ValueError, TypeError):
            pass

    utils_logger.error("Could not parse list from model response.")
    utils_logger.error(f"Problematic code snippet: {raw[:1200]}")
    return None


def gen_cases(language: str, difficulty: str, age: int, output_dir: Path, subject: str, question_type: str, subtype: str, conversation_data=None, sex: str = 'unspecified', model: str = 'gemini'):
    logger = logging.getLogger('my_app')
    logger.debug(f"gen_cases function called with parameters: language={language}, age={age}, subject={subject}, difficulty={difficulty}, question_type={question_type}, subtype={subtype}, sex={sex}, conversation_data={conversation_data}")

    try:
        if conversation_data is None:
            if question_type == 'behavioral':
                prompt = f"""{prompts['cases']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
            elif question_type == 'study':
                prompt = f"""{prompts['study']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
            elif question_type == 'hiring':
                prompt = f"""{prompts['hiring']} Respond in {language}. The content should be appropriate for a person aged {age} and the subject/theme used should be {subject}. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}."""
            logger.debug(f"Initial prompt generated: {prompt}")
        else:
            # conversation_data now has the structure: { "data": { "cases": [ ... ] } }
            previous_cases = conversation_data.get('data', {}).get('cases', [])
            if not isinstance(previous_cases, list) or not previous_cases:
                logger.warning("No previous cases found in conversation data. Returning None.")
                return None, conversation_data

            # Construct a clear textual representation of all previous cases
            previous_turn_summary = ""
            for case in previous_cases:
                user_choice = case.get('user_answer')
                previous_case = case.get('case', "Unknown Case")
                previous_options = case.get('options', [])
                previous_turn_summary += f"Previous Case: {previous_case}\nThe person was presented with the following options:\n"
                for option in previous_options:
                    previous_turn_summary += f"- Option {option['number']}: {option['option']}\n"
                previous_turn_summary += f"\nThe person selected option {user_choice}.\n\n"

            if question_type == 'behavioral':
                prompt = f"""{prompts['cases']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
            elif question_type == 'study':
                prompt = f"""{prompts['study']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
            elif question_type == 'hiring':
                prompt = f"""{prompts['hiring']} The person's previous responses were as follows:\n{previous_turn_summary}For the case, ask another question with the SAME STRUCTURE based on their previous responses in {language} appropriate for the age {age} with the theme '{subject}'. Set the difficulty of the content to {difficulty}. The person is {sex}. The subtype is {subtype}.And Make sure to randomize the position of the optimal option on each case cases should not be having the optimal option at the same number. PLACE THE OPTIMAL OPTION AT ANY RANDOM POINT BETWEEN OPTTIONS 1 TO 8 MAKE SURE TO RANDOMIZE"""
            logger.debug(f"Follow-up prompt generated: {prompt}")

        response = get_response(prompt, model)
        logger.debug(f"Model response: {response[:200]}...")

        if not response:
            logger.error(f"{model} API returned an empty response.")
            return None, conversation_data

        cleaned_response = clean_response(response)
        list_content = extract_list(cleaned_response)
        logger.debug(f"Extracted list content: {list_content}")

        if list_content:
            try:
                parsed = json.loads(list_content)
                # Do not force a single case—keep all cases returned.
                if not isinstance(parsed, list):
                    parsed = [parsed]
                required_keys = ['case', 'options', 'optimal']
                new_cases = []
                for case in parsed:
                    if not all(key in case for key in required_keys):
                        logger.error(f"Missing required keys in parsed response: {required_keys}")
                        return None, conversation_data
                    # Generate a unique case ID
                    case_id = str(uuid.uuid4())
                    case_data = {
                        'case_id': case_id,
                        'case': case['case'], 
                        'optimal': case['optimal'], 
                        'options': []
                    }
                    for option_data in case['options']:
                        option_id = str(uuid.uuid4())
                        option_item = {**option_data, 'option_id': option_id}
                        case_data['options'].append(option_item)
                    new_cases.append(case_data)

                print(f"new cases length: {len(new_cases)}")
                max = 3
                attempts = 0

                while attempts < 1 and len(new_cases) < max:
                    response = get_response(prompt, model)
                    cleaned_response = clean_response(response)
                    list_content = extract_list(cleaned_response)
                    logger.debug(f"Extracted list content: {list_content}")
                    attempts += 1
                    if list_content:
                            parsed = json.loads(list_content)
                            # Do not force a single case—keep all cases returned.
                            if not isinstance(parsed, list):
                                parsed = [parsed]
                            required_keys = ['case', 'options', 'optimal']
                            for case in parsed:
                                if not all(key in case for key in required_keys):
                                    logger.error(f"Missing required keys in parsed response: {required_keys}")
                                    return None, conversation_data
                                # Generate a unique case ID
                                case_id = str(uuid.uuid4())
                                case_data = {
                                    'case_id': case_id,
                                    'case': case['case'], 
                                    'optimal': case['optimal'], 
                                    'options': []
                                }
                                for option_data in case['options']:
                                    option_id = str(uuid.uuid4())
                                    option_item = {**option_data, 'option_id': option_id}
                                    case_data['options'].append(option_item)
                                new_cases.append(case_data)

                # Save conversation data to JSON file with the structure: { "data": { "cases": [ ... ] } }
                conversation_filepath = output_dir / "conversation.json"
                conversation_data = {'data': {'cases': new_cases}}
                with open(conversation_filepath, 'w') as f:
                    json.dump(conversation_data, f, indent=4)
                logger.debug(f"Conversation data saved to {conversation_filepath}")

                return new_cases, conversation_data
            except json.JSONDecodeError as e:
                logger.error(
                    "Failed to parse JSON response: "
                    f"{e}. list_content_preview={list_content[:300]!r}"
                )
                return None, conversation_data
        else:
            logger.error(
                "No valid list content found in response. "
                f"cleaned_response_preview={cleaned_response[:300]!r}"
            )
            return None, conversation_data

    except Exception as e:
        logger.exception(f"An unexpected error occurred in gen_cases: {e}")
        return None, conversation_data


