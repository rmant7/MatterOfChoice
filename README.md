
---

# Matter of Choice API

This API offers two primary interaction methods:  
1. Via a Flask development server.  
2. By directly executing the `game.py` script.  

## Project Setup

Before using either method, follow these steps to set up your environment:

### 1. **Create a Virtual Environment**  
Run the following command to create a virtual environment:  
```bash
python -m venv .venv
```

### 2. **Activate the Virtual Environment**  
- **Windows:**  
  ```bash
  .venv\Scripts\activate
  ```
- **macOS/Linux:**  
  ```bash
  source .venv/bin/activate
  ```

### 3. **Install Dependencies**  
Install all required packages by running:  
```bash
pip install -r requirements.txt
```

### 4. **Set Up the Gemini API Key**  
- Create a `.env` file in the root of the project.  
- Add your Gemini API key to the file as follows:  
  ```
  GEMINI_API_KEY=<YOUR_GEMINI_API_KEY>
  ```

> **Note:** The `.env` file is included in `.gitignore` and will not be committed to version control.

---

## Usage

You can interact with the API using one of the following methods:

### **Method 1: Flask Development Server**  
This method provides a web interface for interacting with the API.

1. **Navigate to the Flask directory:**  
   ```bash
   cd flask
   ```

2. **Run the Flask app:**  
   ```bash
   python app.py
   ```

3. **Access the API:**  
   Open your web browser and navigate to:  
   [http://127.0.0.1:5000/](http://127.0.0.1:5000/)  
   (or the address specified in the console output).

---

### **Method 2: Running `game.py`**  
This method allows direct execution of the core game logic.

1. **Stay in the project root directory.**

2. **Run the script:**  
   ```bash
   python game.py
   ```  
   This will execute the game logic as defined in `game.py`.

---

## Live Demo  

You can access the live demo of the application here:  
[https://moc.pythonanywhere.com/](https://moc.pythonanywhere.com/)

---

## Further Information  

For detailed information about specific functionalities, refer to the documentation provided with Google.

---

This README provides a structured starting point for interacting with the **Matter of Choice API**. Choose the method that best suits your needs and explore the functionality offered by each approach.

--- 

