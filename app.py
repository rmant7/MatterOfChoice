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

app = Flask(__name__)
app.config['TEMPLATES_AUTO_RELOAD'] = True  #For development reload the template
app.secret_key = os.urandom(24) # Don't forget this for sessions
app.config['SECRET_KEY'] = 'your_secret_key'

client = InferenceClient("black-forest-labs/FLUX.1-dev", token="your_huggingface_token")

# Enable `str` function in Jinja2 templates
app.jinja_env.globals.update(str=str, time=time)

# File paths
USER_DATA_FILE = os.path.join('data', 'users.json')
SCENARIOS_FILE = os.path.join('data', 'scenarios.json')
# Ensure the images folder exists




# Helper functions to interact with JSON files
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


def generate_image_for_user(prompt, user, scenario_id):
    """
    Generate a unique image dynamically as a base64 string for a user (with user details) and scenario.
    """
    user_id = user['id']
    username = user['username']
    gender = user['gender']
    age = user['age']

    print(f"Generating image for user ID '{user_id}', username '{username}' (gender: {gender}, age: {age}), and scenario '{scenario_id}' dynamically...")
    
    # Include user details in the unique prompt
    unique_prompt = f"user-{user_id}-{username}-{gender}-{age}-scenario-{scenario_id}: {prompt}"

    max_retries = 20
    retry_delay = 5

    for attempt in range(max_retries):
        try:
            # Generate image using a unique prompt
            image = client.text_to_image(unique_prompt)
            
            # Encode the image as a base64 string
            buffered = BytesIO()
            image.save(buffered, format="PNG")
            image_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

            print(f"Image generated successfully for user ID '{user_id}', username '{username}', and scenario '{scenario_id}'.")
            return f"data:image/png;base64,{image_base64}"
        except Exception as e:
            print(f"Attempt {attempt + 1} failed for user ID '{user_id}', username '{username}', and scenario '{scenario_id}': {e}")
            if attempt + 1 == max_retries:
                raise RuntimeError("Image generation failed after multiple attempts.")
            time.sleep(retry_delay)


BASE_DIR = Path(__file__).resolve().parent


# Configure logging
# Create a logger instance
logger = logging.getLogger('my_app') #name your logger for your app
logger.setLevel(logging.DEBUG)  # Set the logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)

# Create a file handler to write logs to a file
file_handler = logging.FileHandler('flask_app.log')  # Specify the log file name
file_handler.setLevel(logging.DEBUG)

# Create a console handler to output logs to the console
console_handler = logging.StreamHandler()  # streamhandler directs to sys.stderr by default
console_handler.setLevel(logging.DEBUG)

# Create a formatter for log messages and set level
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
file_handler.setFormatter(formatter)
console_handler.setFormatter(formatter)

# Add the handlers to the logger
logger.addHandler(file_handler)
logger.addHandler(console_handler)


logger.debug("Logger configured. Starting app...")


@app.route('/index')  #Serve the index.html file when you go to the root path.
def serve_index():
    return render_template('index2.html')



# Routes
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

        # Generate a unique auto-incremented ID
        if users:
            new_id = max(user['id'] for user in users) + 1
        else:
            new_id = 1  # Start with ID 1 for the first user

        # Create a new user
        new_user = {
            'id': new_id,  # Assign the auto-incremented ID
            'username': username,
            'gender': gender,
            'age': int(age),
            'score': 0,
            'wisdom_level': 1,
            'current_scenario': 1
        }
        users.append(new_user)
        write_json(USER_DATA_FILE, users)

        # Store the username in the session and redirect to dashboard
        session['id'] = new_id
        session['username'] = username
        return redirect(url_for('dashboard'))
    
    return render_template('register.html')


@app.route('/dashboard', methods=['GET', 'POST'])
def dashboard():
    users = read_json(USER_DATA_FILE)
    scenarios = read_json(SCENARIOS_FILE)
    id = session.get('id')
    #username = session.get('username')

    if not id:
        return redirect(url_for('register'))

    user = next((user for user in users if user['id'] == id), None)
    if not user:
        return redirect(url_for('register'))

    # Handle scenario choices
    if request.method == 'POST' and 'choice_index' in request.form:
        choice_index = int(request.form['choice_index'])
        current_scenario_id = user['current_scenario']
        scenario = next((s for s in scenarios if s['id'] == current_scenario_id), None)
        if scenario:
            choice = scenario['choices'][choice_index]
            user['score'] += choice['score_change']
            user['wisdom_level'] += choice['wisdom_change']

        next_scenario_id = user['current_scenario'] + 1
        if next_scenario_id > len(scenarios):
            write_json(USER_DATA_FILE, users)
            return render_template('game_over.html', user=user)
        user['current_scenario'] = next_scenario_id
        write_json(USER_DATA_FILE, users)

    # Generate a dynamic image for the current scenario
    current_scenario_id = user['current_scenario']
    scenario = next((s for s in scenarios if s['id'] == current_scenario_id), None)

    generated_image_data = None
    if scenario:
        prompt = scenario['imageDescription']
        generated_image_data = generate_image_for_user(prompt, user, current_scenario_id)

    return render_template('dashboard.html', user=user, scenario=scenario, generated_image_data=generated_image_data)




@app.route('/generate_cases', methods=['POST', 'GET'])
def generate_cases():
    if request.method == 'GET':
        session.pop('turn', None)
        return jsonify({"message": "Session cleared."}), 200

    logger.debug("generate_cases route entered")
    data = request.get_json()
    language = data.get('language')
    age = data.get('age', None)  # Optional age parameter
    subject = data.get('subject')
    difficulty = data.get('difficulty')
    question_type = data.get('question_type')  # New parameter for question type
    role = data.get('role', 'default_role')  # Optional role parameter with a default value
    sex = data.get('sex', 'unspecified')  # Optional sex parameter with a default value

    user_answer = data.get('answers')

    if not all([language, subject, difficulty, question_type]):
        return jsonify({"error": "language, subject, difficulty, and question_type are required."}), 400
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
            case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sex=sex)
            if case_data is None:
                return jsonify({"error": "Failed to generate initial case."}), 500
            session['turn'] = 2
            # Initialize analysis.json with the first case
            with open(analysis_filepath, 'w') as f:
                json.dump({'cases': [case_data]}, f, indent=4)
        else:
            if user_answer is None:
                return jsonify({"error": "Please Pick one Of the Options Above."}), 400
            try:
                user_answer = int(user_answer)
                with open(output_filepath, 'r') as f:
                    conversation_data = json.load(f)

                # Validate user answer
                if 'data' not in conversation_data or 'options' not in conversation_data['data']:
                    return jsonify({"error": "Invalid conversation data."}), 500
                options_length = len(conversation_data['data']['options'])
                if not 1 <= user_answer <= options_length:
                    return jsonify({"error": "Invalid user answer."}), 400

                # Add user's answer to the current case in analysis.json
                with open(analysis_filepath, 'r') as f:
                    analysis_data = json.load(f)
                current_case = analysis_data['cases'][-1]
                current_case['answer'] = user_answer
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)

                conversation_data['data']['user_answer'] = user_answer
                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, conversation_data, sex=sex)
                if case_data is None:
                    return jsonify({"error": "Failed to generate case."}), 500
                session['turn'] = min(turn + 1, 7)

                # Append the new case to analysis.json
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

    # Get the role and question_type data from the form
    role = data.get('role', 'default_role')  # Optional role parameter with a default value
    question_type = data.get('question_type')  # Get the 'question_type' from form data

    if question_type is None:  # Handle missing question_type data
        return jsonify({"error": "Question type data is missing in the request."}), 400

    # Incorporate role and question_type into analysis_data (adjust as needed)
    analysis_data['role'] = role  # Add the role to the analysis data
    analysis_data['question_type'] = question_type  # Add the question_type to the analysis data

    # Convert the analysis data to a string to send to Gemini
    analysis_data_str = json.dumps(analysis_data)

    # Create the prompt for Gemini, including the role and question_type
    if question_type == 'behavioral':
        prompt = f"Analyze the following data, considering you play the role of a '{role}' to the player, to determine the player's character traits and behavior patterns based on their choices. As you can see, we have cases and options, and each case has an answer. The answer refers to the player's choice for the case among the options provided. Provide a detailed analysis in a structured format that can be easily parsed:\n\n{analysis_data_str} Return your analysis in the same language the data uses."
    elif question_type == 'study':
        prompt = f"Analyze the following data, considering you play the role of a '{role}' to the player, to determine the player's knowledge acquisition and learning patterns based on their choices. As you can see, we have study-based questions and options, and each question has an answer. The answer refers to the player's choice for the question among the options provided. Provide a detailed analysis in a structured format that can be easily parsed:\n\n{analysis_data_str} Return your analysis in the same language the data uses."

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

    # Define the file paths
    conversation_filepath = BASE_DIR / 'output' / 'game' / 'conversation.json'
    analysis_filepath = BASE_DIR / 'output' / 'game' / 'analysis.json'

    # Delete the files if they exist
    if conversation_filepath.exists():
        conversation_filepath.unlink()
    if analysis_filepath.exists():
        analysis_filepath.unlink()

    return jsonify({"message": "Session reset and files deleted."}), 200


if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    read_json(SCENARIOS_FILE)
    app.run(debug=True)
