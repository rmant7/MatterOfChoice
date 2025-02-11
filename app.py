from flask import Flask, render_template, request, redirect, url_for, session
import os
from dotenv import load_dotenv
import json
import requests
import time
from huggingface_hub import InferenceClient
from io import BytesIO
import base64
from PIL import Image

app = Flask(__name__)
load_dotenv()

# Load environment variables
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY')
huggingface_token = os.getenv('HUGGINGFACE_TOKEN')
# Initialize the image generation client using Stable Diffusion
image_client = InferenceClient("stabilityai/stable-diffusion-3.5-large-turbo", token=huggingface_token)
# We'll use the Gemini API (via requests) for text generation

# Enable `str` in Jinja2 templates
app.jinja_env.globals.update(str=str, time=time)

# User data file
USER_DATA_FILE = os.path.join('data', 'users.json')

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

def generate_scenario(user):
    """
    Generates a unique AI-powered scenario using the Gemini API.
    """
    # Replace with your actual Gemini API key
    gemini_api_key = os.getenv('Gemini_Api_Key')
    
    # Construct the API endpoint URL (verify with Gemini docs)
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_api_key}"
    
    # Build the prompt using the user's details
    prompt = f"""
    Create a short, moral, positive consciousness dilemma for a {user['age']} year-old, {user['gender']} named {user['username']}.
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
        
        # Extract generated text from the first candidate.
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
        if not raw_text:
            print("No generated text found in candidate:", candidate)
            return {"error": "No generated content."}
        
        # Remove markdown formatting if present
        if raw_text.startswith("```json"):
            raw_text = raw_text[len("```json"):].strip()
        if raw_text.endswith("```"):
            raw_text = raw_text[:-3].strip()
        
        print("Cleaned generated text:")
        print(raw_text)
        
        # Parse the cleaned text as JSON
        scenario = json.loads(raw_text)
        return scenario

    except Exception as e:
        print(f"Error generating scenario via Gemini: {e}")
        return {"error": "Failed to generate scenario."}

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
            image_bytes = image_client.text_to_image(prompt=unique_prompt)
            print("Raw image response type:", type(image_bytes))
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
        
        if not age.isdigit() or int(age) <= 0:
            return render_template('register.html', error="Age must be a positive number.")
        if any(user['username'] == username for user in users):
            return render_template('register.html', error="Username already exists!")
        
        new_id = max(user['id'] for user in users) + 1 if users else 1
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
        session.permanent = True
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
    
    # Check if 10 scenarios have been played
    if user.get('current_scenario', 1) > 10:
        return render_template('game_over.html', user=user)

    if request.method == 'POST' and 'choice_index' in request.form:
        # Retrieve the scenario JSON from the hidden form field
        scenario_json = request.form.get('scenario_json')
        print("Received scenario_json:", scenario_json)
        try:
            scenario = json.loads(scenario_json)
        except Exception as e:
            print("Error parsing scenario JSON from form:", e)
            scenario = None

        # Retrieve the choice index from the submitted form
        choice_index = request.form.get('choice_index')
        if scenario and choice_index is not None:
            try:
                choice_index = int(choice_index)
                print("Before update: Score:", user['score'], "Wisdom Level:", user['wisdom_level'])
                choice = scenario['choices'][choice_index]
                # [UPDATED] Convert values to int before adding
                user['score'] += int(choice['score_change'])
                user['wisdom_level'] += int(choice['wisdom_change'])
                print("After update: Score:", user['score'], "Wisdom Level:", user['wisdom_level'])
            except Exception as e:
                print("Error processing choice:", e)

        # Increment the scenario counter
        user['current_scenario'] = user.get('current_scenario', 1) + 1
        write_json(USER_DATA_FILE, users)
        # Reload updated user data
        users = read_json(USER_DATA_FILE)
        user = next((u for u in users if u['id'] == user_id), None)
        
        # If more than 10 scenarios, end the game
        if user['current_scenario'] == 10:
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

if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    app.run(debug=True)
