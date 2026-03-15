"""
PythonAnywhere WSGI entry point.

In the PythonAnywhere Web tab set:
  Source code:   /home/<username>/MatterOfChoice
  Working dir:   /home/<username>/MatterOfChoice
  WSGI file:     /home/<username>/MatterOfChoice/wsgi.py
  Python version: 3.x (match your venv)

The variable must be named 'application' for WSGI servers to pick it up.
"""
import sys
import os

# Ensure the project directory is on the path when PythonAnywhere runs this.
project_home = os.path.dirname(os.path.abspath(__file__))
if project_home not in sys.path:
    sys.path.insert(0, project_home)

from app import app as application  # noqa: F401 – required by WSGI spec
