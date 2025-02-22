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
from utils import gen_cases, create_db, get_response_gemini  # keep these for other endpoints
from image import get_info_from_image  # for image analysis

from dotenv import load_dotenv
load_dotenv()


from flask import Flask, render_template, url_for, send_from_directory, request, jsonify, session, redirect, url_for, session
from utils import gen_cases, create_db, get_response_gemini  # added the import for get_response_gemini function
import sqlalchemy
from pathlib import Path
import json
import os
import logging
import time
from huggingface_hub import InferenceClient
from io import BytesIO
import base64
from PIL import Image
from image import get_info_from_image  # Import the function from image.py

app = Flask(__name__)
app.config['TEMPLATES_AUTO_RELOAD'] = True  # For development: auto-reload templates
# Use a fixed secret key (or load from environment) for sessions
app.secret_key = os.getenv("SECRET_KEY", "your_secret_key")
app.jinja_env.globals.update(str=str, time=time)

BASE_DIR = Path(__file__).resolve().parent

# File paths
USER_DATA_FILE = BASE_DIR / "data/users.json"
# NOTE: Removed SCENARIOS_FILE since scenarios will now be generated dynamically.

# Initialize the image generation client using Stable Diffusion.
# (Uses HUGGINGFACE_TOKEN to match code 1's environment variable naming.)
client = InferenceClient("stabilityai/stable-diffusion-3.5-large-turbo", token=os.getenv("HUGGINGFACE_API_KEY"))

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
def generate_scenario(user):
    """
    Generates a unique AI-powered scenario using the Gemini API.
    """
    gemini_api_key = os.getenv('GOOGLE_API_KEY')
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_api_key}"
    
    prompt = f"""
    Create a short, moral, positive consciousness dilemma for a {user['age']}-year-old, {user['gender']} named {user['username']}.
    Provide 4 choices with different outcomes affecting "wisdom_change" and "score_change".
    Format:
    {{
        "id": <scenario_id>,
        "description": "Scenario text",
        "imageDescription": "Short prompt for image",
        "choices": [
             {{"text": "Choice 1", "outcome": "Result 1", "wisdom_change": 3, "score_change": 15}},
             {{"text": "Choice 2", "outcome": "Result 2", "wisdom_change": 2, "score_change": 10}},
             {{"text": "Choice 3", "outcome": "Result 3", "wisdom_change": 1, "score_change": 5}},
             {{"text": "Choice 4", "outcome": "Result 4", "wisdom_change": -1, "score_change": -5}}
        ]
    }}
    """
    
    payload = {
        "contents": [{
            "parts": [{"text": prompt}]
        }]
    }
    
    headers = {"Content-Type": "application/json"}
    
    try:
        response = requests.post(url, headers=headers, json=payload)
        response.raise_for_status()
        response_data = response.json()
        print("Full Gemini API response:")
        print(json.dumps(response_data, indent=2))
        
        candidates = response_data.get("candidates", [])
        if not candidates:
            print("No candidates found in response.")
            return {"error": "No generated content."}
        
        candidate = candidates[0]
        parts = candidate.get("content", {}).get("parts", [])
        if not parts:
            print("No parts found in candidate:", candidate)
            return {"error": "No generated content."}
        
        raw_text = parts[0].get("text", "")
        # Remove markdown formatting if present
        if raw_text.startswith("```json"):
            raw_text = raw_text[len("```json"):].strip()
        if raw_text.endswith("```"):
            raw_text = raw_text[:-3].strip()
        
        print("Cleaned generated text:")
        print(raw_text)
        
        scenario = json.loads(raw_text)
        return scenario

    except Exception as e:
        print(f"Error generating scenario via Gemini: {e}")
        return {"error": "Failed to generate scenario."}

# ----------------------------
# New dynamic image generator (adapted from code 1)
# ----------------------------
# Helper function to generate an image using the generated question text.
import time
import base64
from io import BytesIO
from PIL import Image

def generate_image_for_question(question_text):
    unique_prompt = question_text  # Use the generated question text as prompt
    max_retries = 10
    retry_delay = 5  # Initial delay in seconds

    for attempt in range(max_retries):
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
            # Handle rate limit separately
            if hasattr(e, 'response') and e.response is not None and e.response.status_code == 429:
                print("Rate limit reached. Waiting for 60 seconds before retrying.")
                time.sleep(60)
                continue  # Retry without returning anything
            
            # Handle other exceptions with increasing delay
            print(f"Attempt {attempt + 1} failed: {e}")
            time.sleep(retry_delay)
            retry_delay *= 1.5

    print("Image generation failed after multiple attempts.")
    return None  # Ensures function only returns None if all retries fail

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

@app.route('/cases')
def cases():
    return render_template('index2.html')

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/register', methods=['GET', 'POST'])
def register():
    users = read_json(USER_DATA_FILE)
    if request.method == 'POST':
        username = request.form['username']
        gender = request.form['gender']
        age = request.form['age']

        # Validate input
        if not age.isdigit() or int(age) <= 0:
            return render_template('register.html', error="Age must be a positive number.")
        
        if any(user['username'] == username for user in users):
            return render_template('register.html', error="Username already exists!")

        new_id = max([user['id'] for user in users], default=0) + 1
        new_user = {
            'id': new_id,
            'username': username,
            'gender': gender,
            'age': int(age),
            'score': 0,
            'wisdom_level': 0,
            'current_scenario': 1  # Start at scenario 1
        }
        users.append(new_user)
        write_json(USER_DATA_FILE, users)
        session['id'] = new_id
        session['username'] = username
        return redirect(url_for('dashboard'))
    
    return render_template('register.html')

@app.route('/dashboard', methods=['GET', 'POST'])
def dashboard():
    users = read_json(USER_DATA_FILE)
    user_id = session.get('id')
    if not user_id:
        return redirect(url_for('register'))
    
    user = next((u for u in users if u['id'] == user_id), None)
    if not user:
        return redirect(url_for('register'))
    
    # End the game after 10 scenarios
    if user.get('current_scenario', 1) > 10:
        return render_template('game_over.html', user=user)
    
    if request.method == 'POST' and 'choice_index' in request.form:
        scenario_json = request.form.get('scenario_json')
        print("Received scenario_json:", scenario_json)
        try:
            scenario = json.loads(scenario_json)
        except Exception as e:
            print("Error parsing scenario JSON from form:", e)
            scenario = None

        choice_index = request.form.get('choice_index')
        if scenario and choice_index is not None:
            try:
                choice_index = int(choice_index)
                print("Before update: Score:", user['score'], "Wisdom Level:", user['wisdom_level'])
                choice = scenario['choices'][choice_index]
                user['score'] += int(choice['score_change'])
                user['wisdom_level'] += int(choice['wisdom_change'])
                print("After update: Score:", user['score'], "Wisdom Level:", user['wisdom_level'])
            except Exception as e:
                print("Error processing choice:", e)

        # Increment scenario counter
        user['current_scenario'] = user.get('current_scenario', 1) + 1
        write_json(USER_DATA_FILE, users)
        
        # Check for game over
        if user['current_scenario'] > 10:
            return render_template('game_over.html', user=user)
        
        # Generate a new scenario for the next round
        print("Generating new scenario dynamically...")
        scenario = generate_scenario(user)
        if not scenario or 'error' in scenario:
            return render_template('dashboard.html', user=user, error="Failed to generate a scenario.", scenario=None)
        
        generated_image_data = None
        if 'imageDescription' in scenario:
            try:
                generated_image_data = generate_image_for_scenario(scenario['imageDescription'], user)
            except RuntimeError as e:
                print(f"Image generation failed: {e}")
        
        return render_template('dashboard.html', user=user, scenario=scenario, generated_image_data=generated_image_data)
    
    # GET request: generate a new scenario
    print("Generating new scenario dynamically...")
    scenario = generate_scenario(user)
    if not scenario or 'error' in scenario:
        return render_template('dashboard.html', user=user, error="Failed to generate a scenario.", scenario=None)
    
    generated_image_data = None
    if 'imageDescription' in scenario:
        try:
            generated_image_data = generate_image_for_scenario(scenario['imageDescription'], user)
        except RuntimeError as e:
            print(f"Image generation failed: {e}")
    
    return render_template('dashboard.html', user=user, scenario=scenario, generated_image_data=generated_image_data)

@app.route('/generate_cases', methods=['POST', 'GET'])
def generate_cases():
    if request.method == 'GET':
        session.pop('turn', None)
        return jsonify({"message": "Session cleared."}), 200

    logger.debug("generate_cases route entered")
    data = request.get_json()
    language = data.get('language')
    age = data.get('age', None)
    subject = data.get('subject')
    difficulty = data.get('difficulty')
    question_type = data.get('question_type')  # Parameter for question type
    sub_type = data.get('sub_type')            # Parameter for sub type
    role = data.get('role', None)     # Optional role parameter with a default value
    sex = data.get('sex', 'unspecified')         # Optional sex parameter with a default value

    user_answer = data.get('answers')

    if not all([language, subject, difficulty, question_type, sub_type]):
        return jsonify({"error": "language, subject, difficulty, question_type, and sub_type are required."}), 400
    # if age is not None:
    #     try:
    #         age = int(age)
    #     except ValueError:
    #         return jsonify({"error": "Invalid age (must be an integer)."}), 400

    output_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
    output_dir = output_filepath.parent
    output_dir.mkdir(parents=True, exist_ok=True)

    turn = session.get('turn', 1)

    if turn > 4:
        turn = 1
        session['turn'] = 1
        return jsonify({"message": "CONGRATULATIONS YOU FINISHED THE GAME"}), 200

    try:
        if turn == 1:
            case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, sex=sex)
            max = 5
            attempts = 1

            while attempts < max and case_data is None:
                attempts += 1
                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, sex=sex)
                
            if case_data is None:
                    return jsonify({"error": "Failed to generate initial case."}), 500

            session['turn'] = 2
            # Initialize analysis.json with the first set of cases (each case has no answer yet)
            with open(analysis_filepath, 'w') as f:
                json.dump({'cases': case_data}, f, indent=4)
        else:
            if user_answer is None:
                return jsonify({"error": "User answer is required."}), 400
            # Expecting user_answer as a list of answers (one per case)
            if not isinstance(user_answer, list):
                return jsonify({"error": "User answers must be provided as a list."}), 400

            try:
                with open(output_filepath, 'r') as f:
                    conversation_data = json.load(f)
                # Use the structure: conversation_data = { "data": { "cases": [ ... ] } }
                all_cases = conversation_data.get('data', {}).get('cases', [])
                num_answers = len(user_answer)
                if num_answers > len(all_cases):
                    return jsonify({"error": "The number of answers provided does not match the number of cases."}), 400

                # Assume the current set of cases are the last num_answers entries
                current_cases = all_cases[-num_answers:]
                if len(user_answer) != len(current_cases):
                    return jsonify({"error": "The number of answers provided does not match the number of cases."}), 400

                # Update each current case with its corresponding answer (as a single number)
                for i, case in enumerate(current_cases):
                    case['user_answer'] = user_answer[i]

                # Replace the current cases in all_cases with the updated ones
                all_cases[-num_answers:] = current_cases
                conversation_data['data']['cases'] = all_cases

                # Update analysis.json so that each of the last num_answers cases gets its answer
                with open(analysis_filepath, 'r') as f:
                    analysis_data = json.load(f)
                # Here we assume that analysis_data['cases'] is a list of cases
                for i in range(num_answers):
                    analysis_data['cases'][-num_answers + i]['answer'] = user_answer[i]
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)

                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, conversation_data, sex=sex)
                if case_data is None:
                    return jsonify({"error": "Failed to generate case."}), 500
                session['turn'] = min(turn + 1, 7)

                # *** FIX: Use extend instead of append to add new cases individually ***
                analysis_data['cases'].extend(case_data)
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)
            except (FileNotFoundError, json.JSONDecodeError, ValueError, KeyError) as e:
                return jsonify({"error": f"Error processing user response: {e}"}), 500

        # Within your /generate_cases route, after generating case_data:
        for case in case_data:
            question_prompt = case.get("case")
            if question_prompt:
                   try:
                      # generate_image_for_question returns a data URL (e.g., "data:image/png;base64,...")
                      generated_image_data = generate_image_for_question(question_prompt)
                      # Attach the data URL directly to the case data
                      case['generated_image_data'] = generated_image_data
                   except Exception as e:
                           logger.exception(f"Image generation from question failed: {e}")
                           case['generated_image_data'] = None
        
            else:
                   case['generated_image_data'] = None


        return jsonify({'data': case_data}), 200
    except Exception as e:
        logger.exception(f"An unexpected error occurred: {e}")
        return jsonify({"error": "An unexpected error occurred."}), 500



@app.route('/submit_answers', methods=['POST'])
def submit_answers():
    return jsonify({"message": "Response recorded"}), 200





@app.route('/analysis', methods=['POST'])
def analysis():
    conversation_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
    
    if not analysis_filepath.exists():
        return jsonify({"error": "Analysis file not found."}), 400

    with open(analysis_filepath, 'r') as f:
        analysis_data = json.load(f)
    data = request.get_json()

    # Filter out cases that have an 'answer'
    answered_cases = [case for case in analysis_data['cases'] if 'answer' in case]
    if not answered_cases:
        return jsonify({"error": "No answered cases found for analysis."}), 400

    # Only include answered cases in the analysis data
    analysis_data['cases'] = answered_cases

    role = data.get('role', None)
    question_type = data.get('question_type')
    if question_type is None:
        return jsonify({"error": "Question type data is missing in the request."}), 400

    analysis_data['role'] = role
    analysis_data['question_type'] = question_type
    analysis_data_str = json.dumps(analysis_data, indent=4)

    prompt_template = """Analyze the following data. You are a '{role}'.  The data contains a series of cases, each with a question, options, and the player's chosen answer (indicated by the 'answer' key). The 'optimal' key indicates the correct option.  Determine the language used in the data and STRICTLY PROVIDE YOUR ANALYSIS IN THE LANGUAGE USED.  Format your response as JSON:

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

    if question_type == 'behavioral':
        judgement_aspect = "behavioral tendencies"
    elif question_type == 'study':
        judgement_aspect = "knowledge and learning style"
    elif question_type == 'hiring':
        judgement_aspect = "suitability for the job"
    else:
        return jsonify({"error": "Invalid question_type"}), 400

    prompt = prompt_template.format(
        role=role, 
        judgement_aspect=judgement_aspect, 
        analysis_data_str=analysis_data_str
    )

    try:
        response = get_response_gemini(prompt)

        # Reset session turn and delete the files if they exist.
        session['turn'] = 1
        if conversation_filepath.exists():
            conversation_filepath.unlink()
        if analysis_filepath.exists():
            analysis_filepath.unlink()

        # Send Gemini's analysis along with the answered cases as performance data.
        return jsonify({
            "analysis": response,
            "performance": answered_cases
        }), 200

    except Exception as e:
        logger.exception(f"An error occurred while getting the analysis from Gemini: {e}")
        return jsonify({"error": "An error occurred while getting the analysis from Gemini."}), 500





@app.route('/reset', methods=['POST'])
def reset():
    session['turn'] = 1
    conversation_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
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
        print(f"Input text: {input_text}")
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





@app.route('/submit_responses', methods=['POST'])
def submit_responses():
    """
    This endpoint accepts a set of answers and immediately triggers analysis.
    The client can submit one or more answers (as a list) that will be applied
    to the current unanswered cases in the JSON files. If more answers are sent
    than available cases, an error is returned.
    """
    data = request.get_json()
    user_answers = data.get('answers')
    role = data.get('role')
    question_type = data.get('question_type')
    sub_type = data.get('sub_type')

    # Validate the basic payload.
    if not user_answers or not isinstance(user_answers, list):
        return jsonify({"error": "User answers must be provided as a non-empty list."}), 400
    if not role or not question_type or not sub_type:
        return jsonify({"error": "role, question_type, and sub_type are required."}), 400

    # Define file paths.
    conversation_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
    
    if not conversation_filepath.exists() or not analysis_filepath.exists():
        return jsonify({"error": "Required data files not found."}), 400

    # Load the existing conversation data.
    try:
        with open(conversation_filepath, 'r') as f:
            conversation_data = json.load(f)
    except Exception as e:
        return jsonify({"error": f"Error reading conversation data: {str(e)}"}), 500

    # Load the analysis data.
    try:
        with open(analysis_filepath, 'r') as f:
            analysis_data = json.load(f)
    except Exception as e:
        return jsonify({"error": f"Error reading analysis data: {str(e)}"}), 500

    # Retrieve all cases from the conversation JSON.
    all_cases = conversation_data.get('data', {}).get('cases', [])
    # Determine unanswered cases (those without a 'user_answer' key).
    unanswered_cases = [case for case in all_cases if 'user_answer' not in case]

    # If the user has provided more answers than available unanswered cases, error out.
    if len(user_answers) > len(unanswered_cases):
        return jsonify({"error": "The number of answers provided exceeds the number of unanswered cases."}), 400

    # Update the first N unanswered cases with the provided answers.
    answers_to_apply = iter(user_answers)
    for idx in range(len(all_cases)):
        if 'user_answer' not in all_cases[idx]:
            try:
                all_cases[idx]['user_answer'] = next(answers_to_apply)
            except StopIteration:
                break
    conversation_data['data']['cases'] = all_cases

    # Similarly update the analysis JSON.
    analysis_cases = analysis_data.get('cases', [])
    answers_to_apply = iter(user_answers)
    for idx in range(len(analysis_cases)):
        if 'answer' not in analysis_cases[idx]:
            try:
                analysis_cases[idx]['answer'] = next(answers_to_apply)
            except StopIteration:
                break
    analysis_data['cases'] = analysis_cases

    # Save the updated JSON files.
    try:
        with open(conversation_filepath, 'w') as f:
            json.dump(conversation_data, f, indent=4)
        with open(analysis_filepath, 'w') as f:
            json.dump(analysis_data, f, indent=4)
    except Exception as e:
        return jsonify({"error": f"Error updating files: {str(e)}"}), 500

    # Prepare for analysis: filter to only include answered cases.
    answered_cases = [case for case in analysis_data.get('cases', []) if 'answer' in case]
    if not answered_cases:
        return jsonify({"error": "No answered cases found for analysis."}), 400
    analysis_data['cases'] = answered_cases

    # Build the analysis prompt.
    prompt_template = """Analyze the following data. You are a '{role}'.  The data contains a series of cases, each with a question, options, and the player's chosen answer (indicated by the 'answer' key). The 'optimal' key indicates the correct option.  Determine the language used in the data and STRICTLY PROVIDE YOUR ANALYSIS IN THE LANGUAGE USED.  Format your response as JSON:

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

    if question_type == 'behavioral':
        judgement_aspect = "behavioral tendencies"
    elif question_type == 'study':
        judgement_aspect = "knowledge and learning style"
    elif question_type == 'hiring':
        judgement_aspect = "suitability for the job"
    else:
        return jsonify({"error": "Invalid question_type"}), 400

    analysis_data_str = json.dumps(analysis_data, indent=4)
    prompt = prompt_template.format(
        role=role,
        judgement_aspect=judgement_aspect,
        analysis_data_str=analysis_data_str
    )

    # Trigger the analysis.
    try:
        response_analysis = get_response_gemini(prompt)

        # Reset or clean up the stored files (optional).
        session['turn'] = 1

        if conversation_filepath.exists():
            conversation_filepath.unlink()
        if analysis_filepath.exists():
            analysis_filepath.unlink()

        return jsonify({
            "analysis": response_analysis,
            "performance": answered_cases
        }), 200
    except Exception as e:
        logger.exception(f"Error during analysis: {e}")
        return jsonify({"error": "An error occurred while processing analysis."}), 500





if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    app.run(debug=True)
