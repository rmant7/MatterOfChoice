# your_flask_app/utils.py
import sqlalchemy
import logging
import json
from pathlib import Path
from datetime import datetime
import requests
from io import BytesIO
from PIL import Image
import ast
import google.generativeai as genai  # Import Gemini module
from dotenv import load_dotenv  # Import dotenv for loading .env file
import os
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
# Load environment variables

import uuid  # Import uuid for unique ID generation





Base = declarative_base() #SQLAlchemy Base


#Define your SQLAlchemy models here:
class TopLevelOption(Base):
    __tablename__ = 'top_level_options'
    id = sqlalchemy.Column(sqlalchemy.Integer, primary_key=True)
    text = sqlalchemy.Column(sqlalchemy.String(255))
    cases = relationship("Case", backref="top_level_option")

    def __repr__(self):
        return f"<TopLevelOption(text='{self.text}')>"

class Case(Base):
    __tablename__ = 'cases'
    id = sqlalchemy.Column(sqlalchemy.Integer, primary_key=True)
    top_level_option_id = sqlalchemy.Column(sqlalchemy.Integer, sqlalchemy.ForeignKey('top_level_options.id'))
    text = sqlalchemy.Column(sqlalchemy.Text)
    optimal_option = sqlalchemy.Column(sqlalchemy.Integer)
    options = relationship("CaseOption", backref="case")

    def __repr__(self):
        return f"<Case(text='{self.text[:20]}...')>"

class CaseOption(Base):
    __tablename__ = 'case_options'
    id = sqlalchemy.Column(sqlalchemy.Integer, primary_key=True)
    case_id = sqlalchemy.Column(sqlalchemy.Integer, sqlalchemy.ForeignKey('cases.id'))
    number = sqlalchemy.Column(sqlalchemy.Integer)
    text = sqlalchemy.Column(sqlalchemy.String(255))
    health = sqlalchemy.Column(sqlalchemy.Integer)
    wealth = sqlalchemy.Column(sqlalchemy.Integer)
    relationships = sqlalchemy.Column(sqlalchemy.Integer)
    happiness = sqlalchemy.Column(sqlalchemy.Integer)
    knowledge = sqlalchemy.Column(sqlalchemy.Integer)
    karma = sqlalchemy.Column(sqlalchemy.Integer)
    time_management = sqlalchemy.Column(sqlalchemy.Integer)
    environmental_impact = sqlalchemy.Column(sqlalchemy.Integer)
    personal_growth = sqlalchemy.Column(sqlalchemy.Integer)
    social_responsibility = sqlalchemy.Column(sqlalchemy.Integer)

    def __repr__(self):
        return f"<CaseOption(text='{self.text}')>"


# Function to create the database tables
def create_db(engine):
    Base.metadata.create_all(engine)



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

# Function to get a response from Gemini
def get_response_gemini(prompt: str) -> str:
    try:
        logger(f"Generating response for prompt: {prompt[:50]}...")
        model = genai.GenerativeModel('gemini-2.0-flash-exp')  # Update the model name
        response = model.generate_content(prompt)

        if response and response._result.candidates:
            content = response._result.candidates[0].content.parts[0].text.strip()
            logger(f"Received response: {content[:5000]}...")
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
                    extracted_list = ast.literal_eval(node.value)  # safely convert the list
                    return json.dumps(extracted_list)  # Convert to JSON
        return None
    except (SyntaxError, ValueError, TypeError) as e:
        logger(f"Error parsing or evaluating Python code: {e}")
        logger(f"Problematic code snippet: {code}")
        return None



def gen_cases(language: str, sex: str, age: int, output_dir: Path, subject: str, conversation_data=None):
    logger = logging.getLogger('my_app')
    logger.debug(f"gen_cases function called with parameters: language={language}, sex={sex}, age={age}, subject={subject}, conversation_data={conversation_data}")

    try:
        if conversation_data is None:
            prompt = f"""{prompts['cases']} Respond in {language}. The content should be appropriate for a {sex} child aged {age} and the subject/theme used should be {subject}."""
            logger.debug(f"Initial prompt generated: {prompt}")
        else:
            previous_response = conversation_data.get('data', {})
            previous_turn = conversation_data.get('data', {})
            user_choice = previous_turn.get('user_answer')
            previous_case = previous_turn.get('case', "Unknown Case")
            previous_options = previous_turn.get('options', [])
            if user_choice is None:
                logger.warning("User choice is missing from conversation data. Returning None.")
                return None, conversation_data

            try:
                user_choice = int(user_choice)
            except ValueError as e:
                logger.error(f"Invalid user_choice type: {e}")
                return None, conversation_data

            # Construct a clear textual representation of the previous turn
            previous_turn_summary = f"Previous Case: {previous_case}\nThe child was presented with the following options:\n"
            for option in previous_options:
                previous_turn_summary += f"- Option {option['number']}: {option['option']}\n"
            previous_turn_summary += f"\nThe child selected option {user_choice}."




            prompt = f"""{prompts['cases']} The child's previous response was {previous_turn_summary} for case. In the role of a psychologist for this child, ask another question SAME STRUCTURE based on their previous response  in {language} appropriate for a {sex} child aged {age} with the theme '{subject}'."""
            logger.debug(f"Follow-up prompt generated: {prompt}")


        response = get_response_gemini(prompt)
        logger.debug(f"Gemini response: {response[:200]}...")

        if not response:
            logger.error("Gemini API returned an empty response.")
            return None, conversation_data

        cleaned_response = clean_response(response)
        list_content = extract_list(cleaned_response)
        logger.debug(f"Extracted list content: {list_content}")

        if list_content:
            try:
                parsed = json.loads(list_content)
                if isinstance(parsed, list):
                    parsed = parsed[0]
                required_keys = ['case', 'options', 'optimal']
                if not all(key in parsed for key in required_keys):
                    logger.error(f"Missing required keys in parsed response: {required_keys}")
                    return None, conversation_data

                case_data = {'case': parsed['case'], 'optimal': parsed['optimal'], 'options': []}
                for option_data in parsed['options']:
                    option_id = str(uuid.uuid4())
                    option_item = {**option_data, 'option_id': option_id}
                    case_data['options'].append(option_item)
                case_data['case_id'] = str(uuid.uuid4())

                # Save conversation data to JSON file
                conversation_filepath = output_dir / "conversation.json"
                conversation_data = {'data': case_data}
                with open(conversation_filepath, 'w') as f:
                    json.dump(conversation_data, f, indent=4)
                logger.debug(f"Conversation data saved to {conversation_filepath}")
                return case_data, conversation_data

            except json.JSONDecodeError as e:
                logger.exception(f"Error decoding JSON response: {e}, Raw Response: {response}")
                return None, conversation_data
            except Exception as e:
                logger.exception(f"An unexpected error occurred while processing response: {e}")
                return None, conversation_data

        else:
            logger.error("extract_list returned None or empty string.")
            return None, conversation_data
    except Exception as e:
        logger.exception(f"An unexpected error occurred in gen_cases: {e}")
        return None, conversation_data


# Generate images for cases
def gen_image_cases():
    image_output_path = BASE_DIR / 'output/images'
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
