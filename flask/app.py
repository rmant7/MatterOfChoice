from flask import Flask, render_template, url_for, send_from_directory, request, jsonify, session
from utils import gen_cases, create_db # added the import for create_db function
import sqlalchemy
from pathlib import Path
import json
import os
app = Flask(__name__)
app.config['TEMPLATES_AUTO_RELOAD'] = True  #For development reload the template
BASE_DIR = Path(__file__).resolve().parent


@app.route('/')  #Serve the index.html file when you go to the root path.
def serve_index():
    return render_template('index.html')



@app.route('/generate_cases', methods=['POST', 'GET'])
def generate_cases():
    if request.method == 'GET':
        session.pop('conversation_history', None)
        return jsonify({"message": "Session cleared."}), 200

    try:
        data = request.get_json()
        language = data.get('language')
        sex = data.get('sex')
        age = data.get('age')
        subject = data.get('subject')
        user_response = data.get('user_response')

        if not all([language, sex, age, subject]):
            return jsonify({"error": "language, sex, age, and subject are required fields."}), 400

        try:
            age = int(age)
        except (TypeError, ValueError):
            return jsonify({"error": "age must be a valid integer."}), 400

        conversation_history = session.get('conversation_history', [])

        output_dir = BASE_DIR / 'output' / 'game'
        output_dir.mkdir(parents=True, exist_ok=True)

        try:
            case_data, conversation_history = gen_cases(language, sex, age, output_dir, subject, conversation_history=conversation_history + ([user_response] if user_response is not None else []))
            session['conversation_history'] = conversation_history

            if case_data:
                return jsonify({'data': case_data}), 200
            else:
                return jsonify({'message': 'Conversation ended'}), 200
        except Exception as e:
            return jsonify({'error': str(e)}), 500
    except Exception as e:
        return jsonify({"error": f"An unexpected error occurred: {str(e)}"}), 500





@app.route('/submit_answers', methods=['POST'])
def submit_answers():
    return jsonify({"message": "Response recorded"}), 200


if __name__ == '__main__':
    app.run(debug=True)

