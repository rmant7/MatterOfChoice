import json
from pathlib import Path
from datetime import datetime
import requests
from io import BytesIO
from PIL import Image
import os
import ast
import google.generativeai as genai  # Import Gemini module
from dotenv import load_dotenv  # Import dotenv for loading .env file
load_dotenv()

# Initialize Gemini API
genai.configure(api_key=os.getenv('GOOGLE_API_KEY'))  # Replace with your actual API key

# Prompts data (previously in the .json file)
prompts = {
    "roles": [
        "friends and classmates, teachers and educators",
        "parents, brothers and sisters, grandmothers, grandfathers",
        "animals and insects",
        "grandmothers, grandfathers, elderly people",
        "strangers, unfamiliar people, passersby, strangers",
        "firefighter, police officer, doctor, salesperson",
        "relatives, neighbors, acquaintances"
    ],
    "cases": (
        "Reset your memory of the previous request. You are developing scenarios for an educational game for preschool children aged 5-6. "
        "This game teaches the child how to react appropriately to various life situations and make appropriate behavioral decisions. "
        "Your task is to compile a list of 250 different life situations that can occur on the street, at school, at home, on the playground, in the store, in class, and so on. For each situation, provide a few different behavioral options, evaluate each option based on health, money, and friendship (on a 5-point scale), and indicate the optimal option. "  # Key change: explicitly request options and evaluations
        "Format the answer as a Python list of dictionaries, where each dictionary represents a case and has the following keys: 'case' for the situation description, 'options' which is a list of option dictionaries, and 'optimal' for the number of the optimal option. Each option dictionary should have the keys 'number', 'option', 'health', 'money', and 'friends'. "  # Specify the expected format clearly
        "Do not repeat situations. Here's an example: "
        "[{'case': 'You find a lost toy on the playground. What do you do?', 'options': [{'number': 1, 'option': 'Keep the toy for yourself.', 'health': 0, 'money': 0, 'friends': -2}, {'number': 2, 'option': 'Give the toy to a teacher.', 'health': 2, 'money': 0, 'friends': 4}], 'optimal': '2'}]"  # Provide a clear example in the expected format
        "Use double quotes (\") for all string literals in the Python code.  " # Explicit instruction

    ),
    "cases_2": (
        "Create a list of 10 unique life situations that a child might encounter in various places. "
        "Each situation should present a challenge that the child might face and should not contain any possible solutions. "
        "The goal is diversity and stimulating the child's thinking about appropriate behavioral solutions. Format the answer as a Python list. "
        "Here is an example answer with different scenarios: [\"You are walking down the street with your friend and see someone throwing trash on the sidewalk. What will you do?\", "
        "\"You went to the toy store and saw that the very toy you have wanted for a long time is for sale. But you don't have enough money to buy it. What will you do?\", "
        "\"You are playing in the sandbox with your friends and making a sandcastle. But then another child comes and starts breaking your castle. What will you do?\"]"
    ),
    "options": (
        "You are developing scenarios for an educational game for children aged 5-6. This game teaches the child to cope with challenges in various life situations and make optimal behavioral decisions. "
        "Your task: 1) compile a list of different behavior options for the child for the situation: '''{case}'''. 2) evaluate each of the options on a 5-point scale according to three criteria: health, money, friendship. "
        "Ratings can be negative. 3) indicate the optimal behavior option in this situation. Format the answer as a Python dictionary. "
        "Here is an example answer: {{\"case\":{case}, \"options\":[{{\"number\":1,\"option\":option_text_1, \"health\": 1, \"money\": 0, \"friends\": 4}}, "
        "{{\"number\":2, \"option\":option_text_2, \"health\":-2, \"money\":-3, \"friends\":3}}, {{\"number\":3, \"option\":option_text_3, \"health\":0, \"money\":5, \"friends\":-5}}, "
        "{{\"number\":4, \"option\":option_text_4, \"health\":2, \"money\":1, \"friends\":4}}, {{\"number\":5, \"option\":option_text_5, \"health\":0, \"money\":2, \"friends\":0}}], \"optimal\":\"3\"}}"
    ),
    "image": "create a realistic picture for the following situation: '{case}'",
    "image_en": "Create an image showing the following situation: '{case}'"
}

timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
logger = lambda msg: print(f"{datetime.now()} - {msg}")  # Simple logger

# Directory to save generated cases and images
output_path = Path('./output/game')
output_path.mkdir(parents=True, exist_ok=True)

# Function to get a response from Gemini
def get_response_gemini(prompt: str) -> str:
    try:
        logger(f"Generating response for prompt: {prompt[:50]}...")
        model = genai.GenerativeModel('gemini-1.5-pro-001')
        response = model.generate_content(prompt)

        if response and response._result.candidates:
            content = response._result.candidates[0].content.parts[0].text.strip()
            logger(f"Received response: {content[:50]}...")
            return content
        else:
            logger("Empty or invalid response from Gemini.")
            return ""
    except Exception as err:
        logger(f"Error generating response: {err}")
        return ""

# Function to clean the response from code block formatting
def clean_response(response: str) -> str:
    if response.startswith("```python") and response.endswith("```"):
        return response[len("```python"): -len("```")].strip()
    return response

# Function to extract the list from the cleaned response
def extract_list(code: str) -> str:
    try:
        code = code.replace("\'", "\\'")  # Replacing every \' with \\'
        tree = ast.parse(code)
        for node in ast.walk(tree):
            if isinstance(node, (ast.Assign, ast.Expr)):  # Handle both named and unnamed lists
                if isinstance(node.value, ast.List):
                    # Convert the list *directly* to JSON. Don't evaluate as Python first.
                    extracted_list = ast.literal_eval(node.value)  # safely convert the list
                    return json.dumps(extracted_list) # convert to json using dumps.
        return None

    except (SyntaxError, ValueError, TypeError) as e:
        logger(f"Error parsing or evaluating Python code: {e}")
        logger(f"Problematic code snippet: {code}")
        return None

# Generate cases for different scenarios
def gen_cases():
    logger("Starting to generate cases...")

    for i, option in enumerate(prompts['roles'], start=1):
        prompt = prompts['cases']  # No need to format this string
        try:
            response = get_response_gemini(prompt)
            if not response:
                continue  # Skip if response is empty

            logger(f"Raw response for option {i}: {response}")  # Log the raw response for debugging

            cleaned_response = clean_response(response)
            logger(f"Cleaned response for option {i}: {cleaned_response[:50]}...")

            list_content = extract_list(cleaned_response)
            logger(f"Extracted list for option {i}: {list_content[:50]}...")

            try:
                parsed = json.loads(list_content)
                logger(f"Successfully parsed response for option {i}...")
            except json.JSONDecodeError as e:
                logger(f"Error parsing JSON response for option {i}: {e}")
                continue

            cases = {'option': option, 'cases': parsed}
            case_file = output_path / f"option_{i}.json"
            with open(case_file, 'w', encoding='utf-8') as f:  # Use UTF-8 encoding
                json.dump(cases, f, indent=4, ensure_ascii=False)
            logger(f"Saved case {i} to {case_file}")
        except Exception as err:
            logger(f"Error processing case {i}: {err}")
            continue

# Generate images for cases
def gen_image_cases():
    image_output_path = Path('./output/images')
    image_output_path.mkdir(parents=True, exist_ok=True)
    logger("Generating images for cases...")

    for i, option in enumerate(prompts['roles'], start=1):
        prompt = prompts['image'].format(case=option)
        try:
            logger(f"Generating image for option {i}...")
            response = get_response_gemini(prompt)

            img_data = requests.get(response).content
            image = Image.open(BytesIO(img_data))
            image_path = image_output_path / f"option_{i}.png"
            image.save(image_path)
            logger(f"Image for option {i} saved at {image_path}")
        except Exception as err:
            logger(f"Error generating image for option {i}: {err}")

# Entry point for the script
if __name__ == "__main__":
    gen_cases()
    # gen_image_cases()
