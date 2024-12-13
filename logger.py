# logger.py
import logging

def logger_setup(filename):
    logger = logging.getLogger(filename)
    logger.setLevel(logging.INFO)
    handler = logging.FileHandler(f"{filename}.log")
    handler.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
    logger.addHandler(handler)
    return logger
