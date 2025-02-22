import io
import textwrap
import requests
import google.generativeai as genai
from PIL import Image
import os
from dotenv import load_dotenv

def to_markdown(text):
    """
    Converts plain text to Markdown format.
    """
    text = text.replace('•', '  *')
    return textwrap.indent(text, '> ', predicate=lambda _: True)



load_dotenv()

# Load the API key from environment variables
GENAI_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GENAI_API_KEY:
    raise ValueError("GOOGLE_API_KEY is not set in the .env file.")
genai.configure(api_key=GENAI_API_KEY)


def get_info_from_image(image_file, prompt):
    # Configure Google API
    genai.configure(api_key=GENAI_API_KEY)
    model = genai.GenerativeModel('gemini-1.5-flash')

    # Open image
    img = Image.open(image_file)

    # Generate content
    response = model.generate_content(
        [
            prompt, 
            img
        ], 
        stream=True
    )
    response.resolve()

    # Extract waste type from response text
    waste_type = extract_response(response.text)
    return waste_type

def extract_response(text):
    return text
