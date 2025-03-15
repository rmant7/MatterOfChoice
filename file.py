
@app.route('/generate_cases', methods=['POST', 'GET'])
def generate_cases():
    user_id = request.form.get('user_id')
    if not user_id:
        return jsonify({"error": "User ID is required."}), 400

    user_data_file = BASE_DIR / f"data/{user_id}_users.json"
    output_filepath = BASE_DIR / f'output/{user_id}/game/conversation.json'
    analysis_filepath = BASE_DIR / f'output/{user_id}/game/analysis.json'
    output_dir = output_filepath.parent
    output_dir.mkdir(parents=True, exist_ok=True)

    turn = session.get(f'{user_id}_turn', 1)

    if request.method == 'GET':
        session.pop(f'{user_id}_turn', None)
        return jsonify({"message": "Session cleared."}), 200

    logger.debug("generate_cases route entered")
    data = request.get_json()
    language = data.get('language')
    age = data.get('age', None)
    subject = data.get('subject')
    difficulty = data.get('difficulty')
    question_type = data.get('question_type')  # Parameter for question type
    sub_type = data.get('sub_type')            # Parameter for sub type
    role = data.get('role', None)     # Optional role parameter with a default value
    sex = data.get('sex', 'unspecified')         # Optional sex parameter with a default value

    user_answer = data.get('answers')

    if not all([language, subject, difficulty, question_type, sub_type]):
        return jsonify({"error": "language, subject, difficulty, question_type, and sub_type are required."}), 400

    if turn > 4:
        turn = 1
        session[f'{user_id}_turn'] = 1
        return jsonify({"message": "CONGRATULATIONS YOU FINISHED THE GAME"}), 200

    try:
        if turn == 1:
            case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, sex=sex)
            max = 5
            attempts = 1

            while attempts < max and case_data is None:
                attempts += 1
                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, sex=sex)
                
            if case_data is None:
                return jsonify({"error": "Failed to generate initial case."}), 500

            session[f'{user_id}_turn'] = 2
            # Initialize analysis.json with the first set of cases (each case has no answer yet)
            with open(analysis_filepath, 'w') as f:
                json.dump({'cases': case_data}, f, indent=4)
        else:
            if user_answer is None:
                return jsonify({"error": "User answer is required."}), 400
            # Expecting user_answer as a list of answers (one per case)
            if not isinstance(user_answer, list):
                return jsonify({"error": "User answers must be provided as a list."}), 400

            try:
                with open(output_filepath, 'r') as f:
                    conversation_data = json.load(f)
                # Use the structure: conversation_data = { "data": { "cases": [ ... ] } }
                all_cases = conversation_data.get('data', {}).get('cases', [])
                num_answers = len(user_answer)
                if num_answers > len(all_cases):
                    return jsonify({"error": "The number of answers provided does not match the number of cases."}), 400

                # Assume the current set of cases are the last num_answers entries
                current_cases = all_cases[-num_answers:]
                if len(user_answer) != len(current_cases):
                    return jsonify({"error": "The number of answers provided does not match the number of cases."}), 400

                # Update each current case with its corresponding answer (as a single number)
                for i, case in enumerate(current_cases):
                    case['user_answer'] = user_answer[i]

                # Replace the current cases in all_cases with the updated ones
                all_cases[-num_answers:] = current_cases
                conversation_data['data']['cases'] = all_cases

                # Update analysis.json so that each of the last num_answers cases gets its answer
                with open(analysis_filepath, 'r') as f:
                    analysis_data = json.load(f)
                # Here we assume that analysis_data['cases'] is a list of cases
                for i in range(num_answers):
                    analysis_data['cases'][-num_answers + i]['answer'] = user_answer[i]
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)

                case_data, conversation_data = gen_cases(language, difficulty, age, output_dir, subject, question_type, sub_type, conversation_data, sex=sex)
                if case_data is None:
                    return jsonify({"error": "Failed to generate case."}), 500
                session[f'{user_id}_turn'] = min(turn + 1, 7)

                # *** FIX: Use extend instead of append to add new cases individually ***
                analysis_data['cases'].extend(case_data)
                with open(analysis_filepath, 'w') as f:
                    json.dump(analysis_data, f, indent=4)
            except (FileNotFoundError, json.JSONDecodeError, ValueError, KeyError) as e:
                return jsonify({"error": f"Error processing user response: {e}"}), 500

        # Within your /generate_cases route, after generating case_data:
        for case in case_data:
            question_prompt = case.get("case")
            if question_prompt and question_type != 'study':
                try:
                    # generate_image_for_question returns a data URL (e.g., "data:image/png;base64,...")
                    generated_image_data = generate_image_for_question(question_prompt)
                    # Attach the data URL directly to the case data
                    case['generated_image_data'] = generated_image_data
                except Exception as e:
                    logger.exception(f"Image generation from question failed: {e}")
                    case['generated_image_data'] = None
            else:
                case['generated_image_data'] = None

        return jsonify({'data': case_data}), 200
    except Exception as e:
        logger.exception(f"An unexpected error occurred: {e}")
        return jsonify({"error": "An unexpected error occurred."}), 500

