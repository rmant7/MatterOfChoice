from flask import Flask, render_template, url_for, send_from_directory, request, jsonify, session
from utils import gen_cases, create_db # added the import for create_db function
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
        session.pop('conversation_history', None)
        return jsonify({"message": "Session cleared."}), 200

    logger.debug("generate_cases route entered")
    data = request.get_json()
    language = data.get('language')
    sex = data.get('sex')
    age = data.get('age')
    subject = data.get('subject')
    user_response = data.get('user_response') # Retrieve user response

    if not all([language, sex, age, subject]):  # Make subject required
        return jsonify({"error": "language, sex, age and subject are required fields."}), 400

    try:
        age = int(age)
    except (TypeError, ValueError):
        return jsonify({"error": "age must be a valid integer."}), 400

    output_dir = BASE_DIR / 'output' / 'game'
    output_dir.mkdir(parents=True, exist_ok=True)

    conversation_history = session.get('conversation_history', [])

    try:
        case_data, conversation_history = gen_cases(language, sex, age, output_dir, subject, conversation_history=conversation_history + ([user_response] if user_response is not None else []))
        session['conversation_history'] = conversation_history

        if case_data:
            return jsonify({'data': case_data}), 200
        else:
            return jsonify({'message': 'Conversation ended'}), 200  # Indicate no more cases
    except Exception as e:
        logger.exception(f"An unexpected error occurred: {e}")
        return jsonify({"error": "An unexpected error occurred"}), 500




@app.route('/submit_answers', methods=['POST'])
def submit_answers():
    return jsonify({"message": "Response recorded"}), 200


if __name__ == '__main__':
    app.run(debug=True)

