from flask import Flask, render_template, request, jsonify, session, redirect, url_for
import os
import json
import logging
import time
import requests
from huggingface_hub import InferenceClient
from pathlib import Path
from io import BytesIO
import base64
from PIL import Image
from utils import gen_cases, get_response_gemini  # keep these for other endpoints
from image import get_info_from_image  # for image analysis
import time
from dotenv import load_dotenv
import threading
load_dotenv()

from flask import Flask, render_template, request, jsonify, session, redirect, url_for
import os
import json
import logging
import time
import requests
from huggingface_hub import InferenceClient
from pathlib import Path
from io import BytesIO
import base64
from PIL import Image
from utils import gen_cases, get_response_gemini
from image import get_info_from_image
from dotenv import load_dotenv
import threading
import uuid
from threading import Lock

load_dotenv()

app = Flask(__name__)
app.config['TEMPLATES_AUTO_RELOAD'] = True  # For development: auto-reload templates
# Use a fixed secret key (or load from environment) for sessions
app.secret_key = os.getenv("SECRET_KEY", "your_secret_key")
app.jinja_env.globals.update(str=str, time=time)

BASE_DIR = Path(__file__).resolve().parent

# File paths
# NOTE: Removed SCENARIOS_FILE since scenarios will now be generated dynamically.

# Initialize the image generation client using Stable Diffusion.
# (Uses HUGGINGFACE_TOKEN to match code 1's environment variable naming.)
client = InferenceClient("stabilityai/stable-diffusion-3.5-large-turbo", token=os.getenv("HUGGINGFACE_API_KEY"))

# In-memory storage for generated images
image_storage = {}

# ----------------------------
# Helper functions for JSON I/O
# ----------------------------
def read_json(file_path):
    if not os.path.exists(file_path):
        with open(file_path, 'w') as file:
            json.dump([], file)
    with open(file_path, 'r') as file:
        return json.load(file)

def write_json(file_path, data):
    with open(file_path, 'w') as file:
        json.dump(data, file, indent=4)
    print(f"Updated JSON file: {file_path}")

# ----------------------------
# New dynamic scenario generator (adapted from code 1)
# ----------------------------
# New dynamic image generator (adapted from code 1)
# ----------------------------
# Helper function to generate an image using the generated question text.
import time
import base64
from io import BytesIO
from PIL import Image


def generate_image_for_question(question_text):
    instructions = [
    "The image must not contain words",
    "The image should be a comic style image"
    ]

    unique_prompt = f"{question_text}, *{', *'.join(instructions)}"

    max_retries = 10
    retry_delay = 5  # Initial delay in seconds

    for attempt in range(max_retries):  # Use range to create an iterable sequence
        try:
            print(f"Attempt {attempt + 1}: Generating image with prompt: {unique_prompt}")
            image_bytes = client.text_to_image(prompt=unique_prompt)

            # Handle different formats of returned images
            if isinstance(image_bytes, Image.Image):
                image = image_bytes
            else:
                image = Image.open(BytesIO(image_bytes))

            # Convert image to base64
            buffered = BytesIO()
            image.save(buffered, format="PNG")
            image_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
            return f"data:image/png;base64,{image_base64}"
        except Exception as e:
            if hasattr(e, 'response') and e.response is not None and e.response.status_code == 429:
                print("Rate limit reached. Waiting for 60 seconds before retrying.")
                time.sleep(60)
                continue
            print(f"Attempt {attempt + 1} failed: {e}")
            time.sleep(retry_delay)
            retry_delay *= 1.5

    print("Image generation failed after multiple attempts.")
    return None  # Ensures function only returns None if all retries fail

def generate_image_background(case_id, question_prompt):
    try:
        generated_image_data = generate_image_for_question(question_prompt)
        image_storage[case_id] = generated_image_data
    except Exception as e:
        logger.exception(f"Image generation failed for case {case_id}: {e}")
        image_storage[case_id] = None

# ----------------------------
logger = logging.getLogger('my_app')
logger.setLevel(logging.DEBUG)

file_handler = logging.FileHandler('flask_app.log')
file_handler.setLevel(logging.DEBUG)

console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)

formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
file_handler.setFormatter(formatter)
console_handler.setFormatter(formatter)

logger.addHandler(file_handler)
logger.addHandler(console_handler)

logger.debug("Logger configured. Starting app...")

# ----------------------------
# Routes
# ----------------------------

import uuid

def generate_user_id():
    return str(uuid.uuid4())

@app.route('/cases')
def cases():
    user_id = generate_user_id()
    session['user_id'] = user_id
    return render_template('index2.html')

@app.route('/casesv2')
def cases_v2():
    user_id = generate_user_id()
    session['user_id'] = user_id
    return render_template('cases_v2.html')

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/v2/')
def index_v2():
    return render_template('index_v2.html')

from uuid import uuid4

def ensure_user_id():
    if 'user_id' not in session:
        session['user_id'] = str(uuid4())
    return session['user_id']



#
# =======================================================================================
# === FIX: Drastically speed up the gen_cases function ==================================
# =======================================================================================

JOBS = {}
JOBS_LOCK = Lock()

def run_case_generation_job(job_id, user_id, language, difficulty, age, output_dir, subject, question_type, subtype, sex):
    """This function runs in a background thread and does the slow work."""
    logger.info(f"Starting background job {job_id} for user {user_id}")
    try:
        case_data, _ = gen_cases(language, difficulty, age, output_dir, subject, question_type, subtype, sex=sex)
        
        if case_data:
            # Add turn and other info after generation
            turn = 1 # For now, we assume all background jobs are for the first turn.
                     # This can be made more complex if needed.
            for case in case_data:
                case['turn'] = turn
                case['user_answer'] = None
            
            # Save the final data to the analysis file
            analysis_filepath = output_dir / "analysis.json"
            analysis_data = {'cases': case_data}
            with open(analysis_filepath, 'w') as f:
                json.dump(analysis_data, f, indent=4)
            
            logger.info(f"Job {job_id} completed successfully.")
            with JOBS_LOCK:
                JOBS[job_id]['status'] = 'complete'
                JOBS[job_id]['result'] = case_data
        else:
            raise ValueError("gen_cases returned no data.")

    except Exception as e:
        logger.error(f"Job {job_id} failed: {e}", exc_info=True)
        with JOBS_LOCK:
            JOBS[job_id]['status'] = 'failed'
            JOBS[job_id]['error'] = str(e)


# --- OLD /generate_cases IS REPLACED BY THESE TWO NEW ENDPOINTS ---

@app.route('/start_case_generation', methods=['POST'])
def start_case_generation():
    """
    INSTANTLY responds to the client, starting the slow work in the background.
    """
    user_id = ensure_user_id()
    data = request.get_json()
    # ... (your existing data validation from generate_cases)
    language = data.get('language')
    age = data.get('age')
    subject = data.get('subject')
    # ... etc.
    
    output_dir = BASE_DIR / f'output/{user_id}/game'
    output_dir.mkdir(parents=True, exist_ok=True)
    
    job_id = str(uuid.uuid4())
    
    with JOBS_LOCK:
        JOBS[job_id] = {'status': 'pending', 'result': None}
    
    # Start the background thread
    thread_args = (job_id, user_id, data.get('language'), data.get('difficulty'), data.get('age'), output_dir, data.get('subject'), data.get('question_type'), data.get('sub_type'), data.get('sex', 'unspecified'))
    thread = threading.Thread(target=run_case_generation_job, args=thread_args)
    thread.daemon = True # Allows app to exit even if threads are running
    thread.start()
    
    logger.info(f"Dispatched job {job_id} for user {user_id}. Responding to client immediately.")
    return jsonify({"job_id": job_id}), 202 # 202 Accepted: The request is accepted for processing.

@app.route('/get_job_status/<job_id>', methods=['GET'])
def get_job_status(job_id):
    """
    Client polls this endpoint to check if the background job is done.
    """
    with JOBS_LOCK:
        job = JOBS.get(job_id)

    if job is None:
        return jsonify({"error": "Job not found"}), 404
    
    if job['status'] == 'complete':
        # Clean up the job entry after it's been retrieved
        with JOBS_LOCK:
            # pop to remove it, so we don't store old jobs forever
            final_job_data = JOBS.pop(job_id, None) 
        return jsonify(final_job_data)
    
    return jsonify(job)


@app.route('/get_image/<case_id>', methods=['GET'])
def get_image(case_id):
    image_data = image_storage.get(case_id)
    return jsonify({"generated_image_data": image_data}), 200

@app.route('/reset', methods=['POST'])
def reset():
    # user_id = session.get('user_id')
    user_id = ensure_user_id()
    if not user_id:
        return jsonify({"error": "User ID is required."}), 400

    session[f'{user_id}_turn'] = 1
    conversation_filepath = BASE_DIR / f'output/{user_id}/game/conversation.json'
    analysis_filepath = BASE_DIR / f'output/{user_id}/game/analysis.json'
    user_id = session.get('user_id')
    if not user_id:
        return jsonify({"error": "User ID is required."}), 400

    session[f'{user_id}_turn'] = 1
    conversation_filepath = BASE_DIR / f'output/{user_id}/game/conversation.json'
    analysis_filepath = BASE_DIR / f'output/{user_id}/game/analysis.json'
    if conversation_filepath.exists():
        conversation_filepath.unlink()
    if analysis_filepath.exists():
        analysis_filepath.unlink()
    return jsonify({"message": "Session reset and files deleted."}), 200

@app.route('/converse', methods=['POST'])
def converse():
    data = request.get_json()
    try:
        input_text = data.get('text')
        # print(f"Input text: {input_text}")
        response = get_response_gemini(input_text)
        return jsonify({'response': response}), 200
    except Exception as e:
        logger.exception(f"An error occurred while conversing with Gemini: {e}")
        return jsonify({'error': str(e)}), 500

@app.route('/analyze-image', methods=['POST'])
def analyze_image():
    try:
        print("Request form data:", request.form)
        print("Request files:", request.files)
        if 'image' not in request.files:
            return jsonify({'error': 'No image file in request.files'}), 400
        image_file = request.files['image']
        if image_file.filename == '':
            return jsonify({'error': 'No selected file'}), 400
        input_text = request.form.get('prompt', '')
        response = get_info_from_image(image_file, input_text)
        return jsonify({'response': response}), 200
    except Exception as e:
        logger.exception(f"Error: {e}")
        return jsonify({'error': str(e)}), 500


@app.route('/analysis', methods=['POST'])
def analysis():
    user_id = ensure_user_id()
    # user_id = session.get('user_id')
    if not user_id:
        return jsonify({"error": "User ID is required."}), 400

    conversation_filepath = BASE_DIR / f'output/{user_id}/game/conversation.json'
    analysis_filepath = BASE_DIR / f'output/{user_id}/game/analysis.json'

    if not analysis_filepath.exists():
        return jsonify({"error": "Analysis file not found."}), 400

    with open(analysis_filepath, 'r') as f:
        analysis_data = json.load(f)
    data = request.get_json()

    analysis_cases = analysis_data.get('cases', [])
    analysis_data['cases'] = analysis_cases

    role = data.get('role', None)
    question_type = data.get('question_type')
    language = data.get('language')
    if question_type is None:
        return jsonify({"error": "Question type data is missing in the request."}), 400

    if question_type == 'behavioral':
        judgement_aspect = "behavioral tendencies"
    elif question_type == 'study':
        judgement_aspect = "knowledge and learning style"
    elif question_type == 'hiring':
        judgement_aspect = "suitability for the job"
    else:
        return jsonify({"error": "Invalid question_type"}), 400

    analysis_data['role'] = role
    analysis_data['question_type'] = question_type
    analysis_data['language'] = language
    prompt_cases =  [case for case in analysis_data['cases'] if 'user_answer' in case and case['user_answer']]

    analysis_data_str = json.dumps(prompt_cases, indent=4)

    prompt_template = """Analyze the following data. You are a '{role}'.  The data contains a series of cases, each with a question, options, and the player's chosen answer (indicated by the 'user_answer' key). The 'optimal' key indicates the correct option.  Determine the language used in the data and STRICTLY PROVIDE YOUR ANALYSIS IN THE LANGUAGE {language}.Do not analyze unanswered cases.  Format your response as JSON:

{{
  "overall_judgement": "A concise summary of the player's overall {judgement_aspect}.",
  "cases": [
    {{
      "case_description": "The case description.",
      "player_choice": "The player's selected option in words.",
      "optimal_choice": "The optimal option in words.",
      "analysis": "A detailed analysis of the player's choice, including reasoning and implications."
    }}
  ]
}}

Data: {analysis_data_str}"""

    prompt = prompt_template.format(
        role=role,
        judgement_aspect=judgement_aspect,
        analysis_data_str=analysis_data_str,
        language=language
    )

    max_attempts = 5
    attempt = 0
    while attempt < max_attempts:
        try:
            response_analysis = get_response_gemini(prompt)
            parsed_analysis = parse_json_response(response_analysis)

            if not isinstance(parsed_analysis, dict):
                raise ValueError("Analysis response is not a dictionary")

            if 'overall_judgement' not in parsed_analysis:
                raise ValueError("Missing overall_judgement in analysis")

            if 'cases' not in parsed_analysis or not isinstance(parsed_analysis['cases'], list):
                raise ValueError("Missing or invalid cases array in analysis")

            session['turn'] = 1
            return jsonify(parsed_analysis), 200

        except (json.JSONDecodeError, ValueError) as e:
            attempt += 1
            logger.error(f"Attempt {attempt} failed to parse Gemini response as JSON: {e}")
            if attempt >= max_attempts:
                return jsonify({"error": "Failed to parse analysis response after multiple attempts"}), 500

    return jsonify({"error": "An unexpected error occurred."}), 500





@app.route('/submit_responses', methods=['POST'])
def submit_responses():
    user_id = session.get('user_id')
    if not user_id:
        return jsonify({"error": "User ID is required."}), 400

    data = request.get_json()
    user_answers = data.get('answers', {})  # Expect a dictionary
    role = data.get('role')
    question_type = data.get('question_type')
    sub_type = data.get('sub_type')
    language = data.get('language')

    if not role or not question_type or not sub_type:
        return jsonify({"error": "role, question_type, and sub_type are required."}), 400

    analysis_filepath = BASE_DIR / f'output/{user_id}/game/analysis.json'

    if not analysis_filepath.exists():
        return jsonify({"error": "Required data file not found."}), 400

    try:
        with open(analysis_filepath, 'r') as f:
            analysis_data = json.load(f)

        cases = analysis_data.get('cases', [])
        for case_id, user_answer in user_answers.items():
            found = False
            for case in cases:
                if case['case_id'] == case_id:
                    case['user_answer'] = user_answer  # Update 'answer' field
                    found = True
                    break
            if not found:
                logger.warning(f"Case ID {case_id} not found in analysis data.")

        with open(analysis_filepath, 'w') as f:
            json.dump(analysis_data, f, indent=4)

        prompt_cases =  [case for case in analysis_data['cases'] if 'user_answer' in case and case['user_answer']]

        if question_type == 'behavioral':
            judgement_aspect = "behavioral tendencies"
        elif question_type == 'study':
            judgement_aspect = "knowledge and learning style"
        elif question_type == 'hiring':
            judgement_aspect = "suitability for the job"
        else:
            return jsonify({"error": "Invalid question_type"}), 400

        analysis_data_str = json.dumps(prompt_cases, indent=4)
        prompt = f"""Analyze the following data. You are a '{role}'.  The data contains a series of cases, each with a question, options, and the player's chosen answer (indicated by the 'user_answer' key). The 'optimal' key indicates the correct option.  Determine the language used in the data and STRICTLY PROVIDE YOUR ANALYSIS IN THE LANGUAGE {language}.DO NOT ANALYZE ANY CASE WHERE THE USER DID NOT ANSWER THE QUESTION JUST LEAVE IT OUT OF THE JSON AND FOCUS ONLY ON ANSWERED ONES.  Format your response as JSON:

{{
    "overall_judgement": "A concise summary of the player's overall {judgement_aspect}.",
    "cases": [
        {{
            "case_description": "The case description.",
            "player_choice": "The player's selected option in words.",
            "optimal_choice": "The optimal option in words.",
            "analysis": "A detailed analysis of the player's choice, including reasoning and implications."
        }}
    ]
}}

Data: {analysis_data_str}"""

        max_attempts = 5
        attempt = 0
        while attempt < max_attempts:
            try:
                response_analysis = get_response_gemini(prompt)
                parsed_analysis = parse_json_response(response_analysis)

                if not isinstance(parsed_analysis, dict):
                    raise ValueError("Analysis response is not a dictionary")

                if 'overall_judgement' not in parsed_analysis:
                    raise ValueError("Missing overall_judgement in analysis")

                if 'cases' not in parsed_analysis or not isinstance(parsed_analysis['cases'], list):
                    raise ValueError("Missing or invalid cases array in analysis")

                return jsonify(parsed_analysis), 200

            except (json.JSONDecodeError, ValueError) as e:
                attempt += 1
                logger.error(f"Attempt {attempt} failed to parse Gemini response as JSON: {e}")
                if attempt >= max_attempts:
                    return jsonify({"error": "Failed to parse analysis response after multiple attempts"}), 500

        return jsonify({"error": "An unexpected error occurred."}), 500

    except Exception as e:
        logger.exception(f"Error during analysis: {e}")
        return jsonify({"error": "An error occurred while processing analysis."}), 500






def parse_json_response(response):
    """
    Attempts to clean and parse a JSON response that may be slightly unstructured.
    This includes stripping code block markers and extraneous text.
    """
    # First, strip whitespace
    cleaned = response.strip()
    # Remove common code block markers
    if cleaned.startswith("```"):
        lines = cleaned.splitlines()
        # Remove the first line if it starts with ```
        if lines[0].startswith("```"):
            lines = lines[1:]
        # Remove the last line if it ends with ```
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        cleaned = "\n".join(lines).strip()

    # Attempt to extract substring that starts with '{' and ends with the last '}'
    start_index = cleaned.find('{')
    end_index = cleaned.rfind('}')
    if start_index != -1 and end_index != -1 and end_index > start_index:
        cleaned = cleaned[start_index:end_index+1]

    # Final attempt to parse JSON
    return json.loads(cleaned)





if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    app.run(debug=True)
