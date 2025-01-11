from flask import Flask, render_template, request, redirect, url_for, session
import os
import json

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your_secret_key'


# Enable `str` function in Jinja2 templates
app.jinja_env.globals.update(str=str)


# File paths
USER_DATA_FILE = os.path.join('data', 'users.json')
SCENARIOS_FILE = os.path.join('data', 'scenarios.json')

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
    print(f"Updated JSON file: {file_path}")  # Debugging line


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

        # Create a new user
        new_user = {
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
        session['username'] = username
        return redirect(url_for('dashboard'))
    
    return render_template('register.html')


@app.route('/dashboard', methods=['GET', 'POST'])
def dashboard():
    users = read_json(USER_DATA_FILE)
    scenarios = read_json(SCENARIOS_FILE)
    username = session.get('username')

    if not username:
        return redirect(url_for('register'))

    # Get the current user
    user = next((user for user in users if user['username'] == username), None)
    if not user:
        return redirect(url_for('register'))

    # Handle choice submission
    if request.method == 'POST':
        choice_index = int(request.form['choice_index'])
        current_scenario_id = user['current_scenario']
        scenario = next((s for s in scenarios if s['id'] == current_scenario_id), None)
        if scenario:
            choice = scenario['choices'][choice_index]
            user['score'] += choice['score_change']
            user['wisdom_level'] += choice['wisdom_change']

        # Move to the next scenario
        next_scenario_id = user['current_scenario'] + 1
        if next_scenario_id > len(scenarios):
            # Save the final user data
            write_json(USER_DATA_FILE, users)
            return render_template('game_over.html', user=user)  # Pass updated user data
        user['current_scenario'] = next_scenario_id

        # Save updates
        write_json(USER_DATA_FILE, users)

    # Load the current scenario
    current_scenario_id = user['current_scenario']
    scenario = next((s for s in scenarios if s['id'] == current_scenario_id), None)

    return render_template('dashboard.html', user=user, scenario=scenario)


if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    read_json(USER_DATA_FILE)
    read_json(SCENARIOS_FILE)
    app.run(debug=True)
