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

# Load prompts from JSON file
with open('prompts.json', 'r', encoding='utf-8') as f:
    prompts = json.load(f)

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
def gen_cases(language: str, sex: str, age:int):
    logger("Starting to generate cases...")
    for i, option in enumerate(prompts['roles'], start=1):
        prompt = f"""{prompts['cases']}  Respond in {language}. The content should be appropriate for a {sex} child aged {age}."""
        try:
            response = get_response_gemini(prompt)
            if not response:
                continue
            logger(f"Raw response for option {i}: {response}")
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
            with open(case_file, 'w', encoding='utf-8') as f:
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
    gen_cases(language="english", sex="male", age=8) #Example usage.  Change as needed.
    # gen_image_cases()
