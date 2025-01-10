from flask import Flask, render_template, url_for, send_from_directory, request, jsonify, session
from utils import gen_cases, create_db, get_response_gemini  # added the import for get_response_gemini function
import sqlalchemy
from pathlib import Path
import json
import os
import logging

app = Flask(__name__)
app.config['TEMPLATES_AUTO_RELOAD'] = True  #For development reload the template
app.secret_key = os.urandom(24) # Don't forget this for sessions

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


@app.route('/')  #Serve the index.html file when you go to the root path.
def serve_index():
    return render_template('index.html')


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
    app.run(debug=True)
