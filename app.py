from flask import Flask, render_template, request, redirect, url_for, session
import os
import json
import time
from huggingface_hub import InferenceClient
from io import BytesIO
import base64
from PIL import Image

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your_secret_key'

client = InferenceClient("black-forest-labs/FLUX.1-dev", token="your_huggingface_token(api_key)")

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

if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    read_json(SCENARIOS_FILE)
    app.run(debug=True)
