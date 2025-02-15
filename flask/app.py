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
def generate_image_for_scenario(image_description, user):
    """
    Generates an AI-powered image dynamically based on the scenario.
    """
    print(f"Generating image for user '{user['username']}' using scenario description...")
    unique_prompt = f"{image_description}, highly detailed, for a {user['age']}-year-old {user['gender']}."
    
    max_retries = 5
    retry_delay = 5
    
    for attempt in range(max_retries):
        try:
            image_bytes = client.text_to_image(prompt=unique_prompt)
            if isinstance(image_bytes, Image.Image):
                image = image_bytes
            else:
                image = Image.open(BytesIO(image_bytes))
            buffered = BytesIO()
            image.save(buffered, format="PNG")
            image_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
            print(f"✅ Image generated successfully for user '{user['username']}'.")
            return f"data:image/png;base64,{image_base64}"
        except Exception as e:
            print(f"❌ Attempt {attempt + 1} failed: {e}")
            if attempt + 1 == max_retries:
                raise RuntimeError("❌ Image generation failed after multiple attempts.")
            time.sleep(retry_delay)
            retry_delay *= 1.5

# ----------------------------
# Configure logging
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

# The remaining routes (generate_cases, submit_answers, analysis, reset, converse, analyze-image)
# are left unchanged as they pertain to additional functionality.
# -------------------------------------------
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
    question_type = data.get('question_type')
    sub_type = data.get('sub_type')
    role = data.get('role', 'default_role')
    sex = data.get('sex', 'unspecified')

    user_answer = data.get('answers')

    if not all([language, subject, difficulty, question_type, sub_type]):
        return jsonify({"error": "language, subject, difficulty, question_type, and sub_type are required."}), 400
    if age is not None:
        try:
            age = int(age)
        except ValueError:
            return jsonify({"error": "Invalid age (must be an integer)."}), 400

    output_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
    output_dir = output_filepath.parent
    output_dir.mkdir(parents=True, exist_ok=True)

    turn = session.get('turn', 1)

    if turn > 6:
        turn = 1
        session['turn'] = 1
        return jsonify({"message": "CONGRATULATIONS YOU FINISHED THE GAME"}), 200

    try:
        if turn == 1:
            case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, sex=sex)
            if case_data is None:
                return jsonify({"error": "Failed to generate initial case."}), 500
            session['turn'] = 2
            with open(analysis_filepath, 'w') as f:
                json.dump({'cases': [case_data]}, f, indent=4)
        else:
            if user_answer is None:
                return jsonify({"error": "User answer is required."}), 400
            try:
                user_answer = int(user_answer)
                with open(output_filepath, 'r') as f:
                    conversation_data = json.load(f)

                if 'data' not in conversation_data or 'options' not in conversation_data['data']:
                    return jsonify({"error": "Invalid conversation data."}), 500
                options_length = len(conversation_data['data']['options'])
                if not 1 <= user_answer <= options_length:
                    return jsonify({"error": "Invalid user answer."}), 400

                with open(analysis_filepath, 'r') as f:
                    analysis_data = json.load(f)
                current_case = analysis_data['cases'][-1]
                current_case['answer'] = user_answer
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)

                conversation_data['data']['user_answer'] = user_answer
                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, conversation_data, sex=sex)
                if case_data is None:
                    return jsonify({"error": "Failed to generate case."}), 500
                session['turn'] = min(turn + 1, 7)

                analysis_data['cases'].append(case_data)
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)
            except (FileNotFoundError, json.JSONDecodeError, ValueError, KeyError) as e:
                return jsonify({"error": f"Error processing user response: {e}"}), 500

        return jsonify({'data': case_data}), 200
    except Exception as e:
        logger.exception(f"An unexpected error occurred: {e}")
        return jsonify({"error": "An unexpected error occurred."}), 500

@app.route('/submit_answers', methods=['POST'])
def submit_answers():
    return jsonify({"message": "Response recorded"}), 200

@app.route('/analysis', methods=['POST'])
def analysis():
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'
    if not analysis_filepath.exists():
        return jsonify({"error": "Analysis file not found."}), 400

    with open(analysis_filepath, 'r') as f:
        analysis_data = json.load(f)
    data = request.get_json()

    role = data.get('role', 'default_role')
    question_type = data.get('question_type')
    if question_type is None:
        return jsonify({"error": "Question type data is missing in the request."}), 400

    analysis_data['role'] = role
    analysis_data['question_type'] = question_type

    analysis_data_str = json.dumps(analysis_data)

    if question_type == 'behavioral':
        prompt = f"Analyze the following data, considering you play the role of a '{role}' to the player, to determine the player's character traits and behavior patterns based on their choices. As you can see, we have cases and options, and each case has an answer. The answer refers to the player's choice for the case among the options provided. Provide a detailed analysis in a structured format that can be easily parsed:\n\n{analysis_data_str} Return your analysis in the same language the data uses."
    elif question_type == 'study':
        prompt = f"Analyze the following data, considering you play the role of a '{role}' to the player, to determine the player's knowledge acquisition and learning patterns based on their choices. As you can see, we have study-based questions and options, and each question has an answer. The answer refers to the player's choice for the question among the options provided. Provide a detailed analysis in a structured format that can be easily parsed:\n\n{analysis_data_str} Return your analysis in the same language the data uses."
    elif question_type == 'hiring':
        prompt = f"Analyze the following data, considering you play the role of a '{role}' to the player, to determine the player's suitability for a job based on their choices. As you can see, we have hiring-based questions and options, and each question has an answer. The answer refers to the player's choice for the question among the options provided. Provide a detailed analysis in a structured format that can be easily parsed:\n\n{analysis_data_str} Return your analysis in the same language the data uses."

    try:
        response = get_response_gemini(prompt)
        if analysis_filepath.exists():
            analysis_filepath.unlink()
        return jsonify({"analysis": response}), 200
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

if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    app.run(debug=True)
