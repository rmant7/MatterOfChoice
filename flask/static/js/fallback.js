// const form = document.getElementById('generateForm');
// const responseContainer = document.getElementById('response-container');
// const loadingSpinner = document.getElementById('loading-spinner');
// const caseForm = document.getElementById('caseForm');
// const resetButton = document.getElementById('resetButton'); // Get the reset button

// // Define subtypes for each question type
// const subtypes = {
//     study: [
//         { value: 'subject_mastery', text: 'Subject Mastery' },
//         { value: 'critical_thinking', text: 'Critical Thinking' },
//         { value: 'practical_application', text: 'Practical Application' }
//     ],
//     behavioral: [
//         { value: 'interpersonal_skills', text: 'Interpersonal Skills' },
//         { value: 'ethical_dilemmas', text: 'Ethical Dilemmas' },
//         { value: 'stress_management', text: 'Stress Management' }
//     ],

//     hiring: [
//         { value: 'technical_skills', text: 'Technical Skills' },
//         { value: 'behavioral_interview', text: 'Behavioral Interview' },
//         { value: 'situational_judgment', text: 'Situational Judgment' }
//     ]
// };

// // Add event listener to update the subtype dropdown based on the selected question type
// const questionTypeSelect = document.getElementById('question_type');
// const subTypeSelect = document.getElementById('sub_type');

// questionTypeSelect.addEventListener('change', (event) => {
//     const selectedType = event.target.value;
//     updateSubTypeOptions(selectedType);
// });

// function updateSubTypeOptions(selectedType) {
//     // Clear existing options
//     subTypeSelect.innerHTML = '';

//     // Populate new options based on the selected question type
//     if (subtypes[selectedType]) {
//         subtypes[selectedType].forEach(subtype => {
//             const option = document.createElement('option');
//             option.value = subtype.value;
//             option.text = subtype.text;
//             subTypeSelect.appendChild(option);
//         });
//     }
// }

// // Initialize the subtype options based on the default selected question type
// updateSubTypeOptions(questionTypeSelect.value);

// form.addEventListener('submit', async (event) => {
//     event.preventDefault();

//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     const formData = new FormData(form);
//     const data = {};
//     formData.forEach((value, key) => data[key] = value);

//     try {
//         const response = await fetch('/generate_cases', {
//             method: 'POST',
//             headers: { 'Content-Type': 'application/json' },
//             body: JSON.stringify(data)
//         });
//         handleGenerateCasesResponse(response);
//     } catch (error) {
//         displayError({ error: error.message });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// });

// caseForm.addEventListener('submit', async (event) => {
//     event.preventDefault();

//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     const formData = new FormData(form);
//     const data = {};
//     formData.forEach((value, key) => data[key] = value);

//     const caseElements = caseForm.querySelectorAll('.case-container');
//     const selectedOption = caseElements[0].querySelector('input[type="radio"]:checked');

//     if (!selectedOption) {
//         alert('Please select an option.');
//         loadingSpinner.classList.add('hidden');
//         return; // Prevent submission
//     }

//     // Get the selected option's index (1-based)
//     const selectedOptionIndex = Array.from(caseElements[0].querySelectorAll('input[type="radio"]')).findIndex(radio => radio.checked) + 1;
//     data.answers = selectedOptionIndex; // Send the selected option index as the answer

//     try {
//         const response = await fetch('/generate_cases', {
//             method: 'POST',
//             headers: { 'Content-Type': 'application/json' },
//             body: JSON.stringify(data)
//         });
//         handleGenerateCasesResponse(response);
//     } catch (error) {
//         displayError({ error: error.message });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// });

// // Add event listener for the reset button
// resetButton.addEventListener('click', async () => {
//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     try {
//         const response = await fetch('/reset', {
//             method: 'POST'
//         });

//         if (!response.ok) {
//             const errorData = await response.json();
//             displayError(errorData);
//             return;
//         }

//         const jsonData = await response.json();
//         alert(jsonData.message); // Display reset confirmation message
//         startNewGame(); // Reset the game state

//         // Reload the page after reset
//         window.location.reload();
//     } catch (error) {
//         displayError({ error: error.message });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// });

// function startNewGame() {
// }

// // ... (rest of the code remains unchanged) ...

// async function submitAnswers(answers) {
//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     try {
//         // Simulate a successful response
//         const data = {
//             total_score: calculateTotalScore(answers), // You'll need to implement this function
//             max_total_score: calculateMaxTotalScore(answers), // You'll need to implement this function
//             results: answers.map(answer => ({
//                 case_id: answer.case_id,
//                 score: calculateScoreForAnswer(answer), // You'll need to implement this function
//                 max_score: calculateMaxScoreForAnswer(answer) // You'll need to implement this function
//             }))
//         };

//         displayResults(data);
//         responseContainer.classList.add('success');


//     } catch (error) {
//         // This would handle errors in your score calculation logic, if any.
//         displayError({ error: error.message });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// }

// // Placeholder functions - you'll need to define the actual scoring logic.
// function calculateTotalScore(answers) {
//     // Replace with your actual score calculation
//     return answers.length * 5; // Example: 5 points per answer
// }


// function calculateMaxTotalScore(answers) {
//     // Replace with your actual max score calculation
//     return answers.length * 10; // Example: 10 max points per answer
// }

// function calculateScoreForAnswer(answer) {
//     // Replace with your actual score calculation for a single answer
//     return Math.floor(Math.random() * 6); // Example: random score between 0 and 5
// }

// function calculateMaxScoreForAnswer(answer) {
//      // Replace with your actual max score calculation for a single answer
//     return 10; // Example: 10 max points per answer
// }

// function displayResults(data) {
//     responseContainer.innerHTML = `
//         <h2>Results</h2>
//         <div class="result-summary">
//             <p><strong>Your Score:</strong> ${data.total_score}</p>
//             <p><strong>Max Possible Score:</strong> ${data.max_total_score}</p>
//         </div>
//         <div class="result-details">
//     `;

//     data.results.forEach(result => {
//         responseContainer.innerHTML += `
//             <div class="case-result">
//                 <p><strong>Case ID:</strong> ${result.case_id}</p>
//                 <p><strong>Your Score:</strong> ${result.score}</p>
//                 <p><strong>Max Score:</strong> ${result.max_score}</p>
//             </div>
//         `;
//     });

//     responseContainer.innerHTML += `</div>`;

//     // Add New Game button
//     const newGameButton = document.createElement('button');
//     newGameButton.id = 'newGameButton';
//     newGameButton.textContent = 'New Game';
//     newGameButton.addEventListener('click', startNewGame);
//     responseContainer.appendChild(newGameButton);
// }








































// // Global variables for managing the current batch of cases
// let currentCaseIndex = 0;       // Index of the case currently being shown
// let casesBatch = [];            // Array to store the current 3 cases
// let answersBatch = [];          // Array to store the user's answers for the current batch
// let batchCounter = 0;           // Counter for the number of batches received

// // Called when a new set of cases is received from the backend
// async function handleGenerateCasesResponse(response) {
//     if (!response.ok) {
//         const errorData = await response.json();
//         displayError(errorData);
//         return; // Stop if there's an error
//     }
    
//     const jsonData = await response.json();

//     // Check if the game is finished
//     if (jsonData.message && jsonData.message === "CONGRATULATIONS YOU FINISHED THE GAME") {
//         displayCongratulations(jsonData.message);
//         return;
//     }

//     // Robust check for data structure
//     if (!jsonData.data) {
//         displayError({ error: "Server response missing 'data' field." });
//         return;
//     }

//     // Increment batch counter for each new batch received
//     batchCounter++;

//     // The API now returns an array of cases.
//     casesBatch = Array.isArray(jsonData.data) ? jsonData.data : [jsonData.data];
    
//     // Save the batch to localStorage (optional)
//     localStorage.setItem('casesBatch', JSON.stringify(casesBatch));
    
//     // Reset index and the answers array
//     currentCaseIndex = 0;
//     answersBatch = new Array(casesBatch.length); // Pre-size the array

//     // Display the first case of the new batch
//     displayCurrentCase();
// }

// // Displays only the current case from the batch
// function displayCurrentCase() {
//     caseForm.innerHTML = ''; // Clear previous content

//     if (casesBatch.length === 0 || currentCaseIndex >= casesBatch.length) {
//         displayError({ error: "No case available." });
//         return;
//     }

//     const caseData = casesBatch[currentCaseIndex];
//     const caseElement = createCaseElement(caseData);
//     caseForm.appendChild(caseElement);
    
//     // If this is not the first batch, add the Analyze Results button above the submit button
//     if (currentCaseIndex > 1) {
//         const analyzeButton = document.createElement('button');
//         const midanalysisButton = document.createElement('button');
//         midanalysisButton.type = 'button';
//         midanalysisButton.id = 'midanalysisButton';
//         midanalysisButton.classList.add('midanalyze-button');
//         midanalysisButton.innerText = 'Immediate Analysis';
//         caseForm.appendChild(midanalysisButton);
//         attachMidAnalysisListener();

//         analyzeButton.type = 'button';
//         analyzeButton.id = 'analyzeButton';
//         analyzeButton.classList.add('analyze-button');
//         analyzeButton.innerText = 'Analyze Results';
//         caseForm.appendChild(analyzeButton);
//         attachAnalyzeButtonListener();
//     }

//     // Create a submit button for this single case
//     const submitButton = document.createElement('button');
//     submitButton.type = 'button';
//     submitButton.classList.add('submit-button');
//     submitButton.innerText = 'Submit Answer';
//     submitButton.addEventListener('click', submitCurrentAnswer);
//     caseForm.appendChild(submitButton);
// }

// function submitCurrentAnswer() {
//     // Ensure currentCaseIndex is within bounds
//     if (currentCaseIndex >= casesBatch.length) {
//         displayError({ error: "No more cases to answer." });
//         return;
//     }

//     const caseData = casesBatch[currentCaseIndex];
//     if (!caseData) {
//         displayError({ error: "Case data is undefined." });
//         return;
//     }
    
//     // Assuming radio inputs have a name matching the case's unique id (caseData.case_id)
//     const selected = document.querySelector(`input[name="${caseData.case_id}"]:checked`);
//     if (!selected) {
//         displayError({ error: "Please select an answer." });
//         return;
//     }
    
//     // Save the answer for this case
//     answersBatch[currentCaseIndex] = parseInt(selected.value, 10);

//     // Move on to the next case
//     currentCaseIndex++;

//     // If there are still unanswered cases in the batch, display the next case
//     if (currentCaseIndex < casesBatch.length) {
//         displayCurrentCase();
//     } else {
//         // All cases have been answered.
//         // Disable the submit button to prevent further clicks.
//         const submitBtn = document.querySelector('.submit-button');
//         if (submitBtn) {
//             submitBtn.disabled = true;
//         }
//         // Now send the answers to the backend.
//         sendAnswersToBackend(answersBatch);
//     }
// }

// // Sends the current batch of answers to the backend to retrieve a new set of cases
// async function sendAnswersToBackend(answersArr) {
//     // Show loading spinner (assumes an element with ID "loadingSpinner")
//     const loadingSpinner = document.getElementById('loading-spinner');
//     if (loadingSpinner) {
//         loadingSpinner.classList.remove('hidden');
//     }
//     const responseContainer = document.getElementById('response-container');
//     if (responseContainer) {
//         responseContainer.classList.add('hidden');
//         responseContainer.classList.remove('success');
//         responseContainer.classList.remove('error');
//     }


//     // For non-sub-type elements, use fallback values (from localStorage) if they're missing in the DOM.
//     const languageEl = document.getElementById('language') || { value: localStorage.getItem('language') || "" };
//     const ageEl = document.getElementById('age') || { value: localStorage.getItem('age') || "" };
//     const subjectEl = document.getElementById('subject') || { value: localStorage.getItem('subject') || "" };
//     const difficultyEl = document.getElementById('difficulty') || { value: localStorage.getItem('difficulty') || "" };
//     const questionTypeEl = document.getElementById('question_type') || { value: localStorage.getItem('question_type') || "" };
//     // DO NOT MODIFY sub-type logic: no fallback for sub-type.
//     const subTypeEl = document.getElementById('sub_type');
//     const sexEl = document.getElementById('sex') || { value: localStorage.getItem('sex') || "" };

//     // Only check for sub-type element since the others have fallbacks.
//     if (!subTypeEl) {
//         displayError({ error: "Sub-type form element is missing." });
//         if (loadingSpinner) loadingSpinner.classList.add('hidden');
//         return;
//     }

//     const payload = {
//         language: languageEl.value,
//         age: ageEl.value,
//         subject: subjectEl.value,
//         difficulty: difficultyEl.value,
//         question_type: questionTypeEl.value,
//         sub_type: subTypeEl.value,
//         role: 'default_role',
//         sex: sexEl.value,
//         answers: answersArr
//     };

//     try {
//         const response = await fetch('/generate_cases', {
//             method: 'POST',
//             headers: { 'Content-Type': 'application/json' },
//             body: JSON.stringify(payload)
//         });
//         // Clear the stored batch and handle the new response.
//         localStorage.removeItem('casesBatch');
//         await handleGenerateCasesResponse(response);
//     } catch (error) {
//         displayError({ error: error.message });
//     } finally {
//         // Hide loading spinner after API call completes
//         if (loadingSpinner) {
//             loadingSpinner.classList.add('hidden');
//         }
//         if (responseContainer) {
//             responseContainer.classList.remove('hidden');
//         }
//     }
// }

// // Original function to create a case element remains largely unchanged
// function createCaseElement(caseData) {
//     const caseElement = document.createElement('div');
//     caseElement.classList.add('case-container');

//     const caseTitle = document.createElement('div');
//     caseTitle.classList.add('case-title');
//     caseTitle.innerText = caseData.case;
//     caseElement.appendChild(caseTitle);

//     // Save the case id in a hidden input if needed (if your backend uses it)
//     const hiddenInput = document.createElement('input');
//     hiddenInput.type = 'hidden';
//     hiddenInput.name = 'case_id';
//     hiddenInput.value = caseData.case_id || '';
//     caseElement.appendChild(hiddenInput);

//     caseData.options.forEach(option => {
//         const optionElement = document.createElement('div');
//         optionElement.classList.add('option');

//         const radioInput = document.createElement('input');
//         radioInput.type = 'radio';
//         // Use a unique name per case so that only one option can be selected
//         radioInput.name = caseData.case_id;
//         radioInput.value = option.number; // Use option.number if that's what the API expects
//         radioInput.id = `option_${option.option_id}`;

//         const label = document.createElement('label');
//         label.htmlFor = `option_${option.option_id}`;
//         label.innerText = `${option.number}. ${option.option}`;

//         optionElement.appendChild(radioInput);
//         optionElement.appendChild(label);
//         caseElement.appendChild(optionElement);
//     });

//     return caseElement;
// }





function submitCurrentAnswerM() {
    if (currentCaseIndex >= casesBatch.length) {
      displayError({ error: "No more cases to answer." });
      return;
    }
  
    const caseData = casesBatch[currentCaseIndex];
    if (!caseData) {
      displayError({ error: "Case data is undefined." });
      return;
    }
  
    const selected = document.querySelector(`input[name="${caseData.case_id}"]:checked`);
    if (!selected) {
      displayError({ error: "Please select an answer." });
      return;
    }
  
    answersBatch[currentCaseIndex] = parseInt(selected.value, 10);
    currentCaseIndex++;
  
    if (currentCaseIndex < casesBatch.length) {
      displayCurrentCase();
    } else {
      const submitBtn = document.querySelector('.submit-button');
      if (submitBtn) {
        submitBtn.disabled = true;
      }
      sendAnswersToBackend(answersBatch);
    }
  }
  





  async function sendAnswersToBackendM(answersArr) {
    const loadingSpinner = document.getElementById('loading-spinner');
    if (loadingSpinner) {
      loadingSpinner.classList.remove('hidden');
    }
    const responseContainer = document.getElementById('response-container');
    if (responseContainer) {
      responseContainer.classList.add('hidden');
      responseContainer.classList.remove('success');
      responseContainer.classList.remove('error');
    }
  
    const languageEl = document.getElementById('language') || { value: localStorage.getItem('language') || "" };
    const ageEl = document.getElementById('age') || { value: localStorage.getItem('age') || "" };
    const subjectEl = document.getElementById('subject') || { value: localStorage.getItem('subject') || "" };
    const difficultyEl = document.getElementById('difficulty') || { value: localStorage.getItem('difficulty') || "" };
    const questionTypeEl = document.getElementById('question_type') || { value: localStorage.getItem('question_type') || "" };
    const subTypeEl = document.getElementById('sub_type');
    const sexEl = document.getElementById('sex') || { value: localStorage.getItem('sex') || "" };
  
    if (!subTypeEl) {
      displayError({ error: "Sub-type form element is missing." });
      if (loadingSpinner) loadingSpinner.classList.add('hidden');
      return;
    }
  
    const payload = {
      language: languageEl.value,
      age: ageEl.value,
      subject: subjectEl.value,
      difficulty: difficultyEl.value,
      question_type: questionTypeEl.value,
      sub_type: subTypeEl.value,
      role: 'default_role',
      sex: sexEl.value,
      answers: answersArr
    };
  
    try {
      const response = await fetch('/submit_responses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      localStorage.removeItem('casesBatch');


      if (!response.ok) {
        const errorData = await response.json();
        displayError(errorData);
        return;
      }
      const analysisData = await response.json();
        if (analysisData && analysisData.analysis) {
            displayAnalysis(analysisData.analysis);
            
        } else {    
            displayError({ error: "Invalid analysis data received from server." });
        }
        responseContainer.classList.add('success');

    } catch (error) {
        displayError({ error: `Analysis failed: ${error}` });
    } finally {
    loadingSpinner.classList.add('hidden');
    responseContainer.classList.remove('hidden');
  }
  }
  































// // ... (Other functions remain largely unchanged, but consider refactoring for clarity) ...

// function displayCongratulations(message) {
//     responseContainer.classList.add('success');
//     responseContainer.innerHTML = `
//         <h2>${message}</h2>
//         <button id="newGameButton">Start New Game</button>
//         <button id="analyzeButton">Analyze Results</button>
//     `;

//     const newGameButton = document.getElementById('newGameButton');
//     newGameButton.addEventListener('click', startNewGame);

//     // Attach analyzeButton event listener AFTER it's created
//     attachAnalyzeButtonListener();
// }

// function attachAnalyzeButtonListener() {
//     const analyzeButton = document.getElementById('analyzeButton');
//     if (analyzeButton) { // Check if the button exists before adding the listener
//         analyzeButton.addEventListener('click', analyzeResults);
//     } else {
//         console.error("Analyze button not found!");
//     }
// }


// function attachMidAnalysisListener() {
//     const midanalysisButton = document.getElementById('midanalysisButton');
//     if (midanalysisButton) { // Check if the button exists before adding the listener
//         midanalysisButton.addEventListener('click', midAnalysis);
//     } else {
//         console.error("Analyze button not found!");
//     }
// }

// async function analyzeResults() {
//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     const role = document.getElementById('role').value;
//     const question_type = document.getElementById('question_type').value;

//     try {
//         const response = await fetch('/analysis', {
//             method: 'POST',
//             body: JSON.stringify({ role: role, question_type: question_type }),
//             headers: { 'Content-Type': 'application/json' }
//         });

//         if (!response.ok) {
//             const errorData = await response.json();
//             displayError(errorData);
//             return;
//         }

//         const analysisData = await response.json();
//         if (analysisData && analysisData.analysis) {
//             // Display Gemini's analysis
//             displayAnalysis(analysisData.analysis);
            
//             // If performance data is available, display it as well.
//             // if (analysisData.performance) {
//             //     displayPerformance(analysisData.performance);
//             // }
//         } else {
//             displayError({ error: "Invalid analysis data received from server." });
//         }
//         responseContainer.classList.add('success');

//     } catch (error) {
//         displayError({ error: `Analysis failed: ${error}` });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// }



// async function midAnalysis(answersArr) {
//     loadingSpinner.classList.remove('hidden');
//     responseContainer.classList.add('hidden');
//     responseContainer.classList.remove('success');
//     responseContainer.classList.remove('error');

//     // For non-sub-type elements, use fallback values (from localStorage) if they're missing in the DOM.
//     const languageEl = document.getElementById('language') || { value: localStorage.getItem('language') || "" };
//     const ageEl = document.getElementById('age') || { value: localStorage.getItem('age') || "" };
//     const subjectEl = document.getElementById('subject') || { value: localStorage.getItem('subject') || "" };
//     const difficultyEl = document.getElementById('difficulty') || { value: localStorage.getItem('difficulty') || "" };
//     const questionTypeEl = document.getElementById('question_type') || { value: localStorage.getItem('question_type') || "" };
//     // DO NOT MODIFY sub-type logic: no fallback for sub-type.
//     const subTypeEl = document.getElementById('sub_type');
//     const sexEl = document.getElementById('sex') || { value: localStorage.getItem('sex') || "" };


//     const payload = {
//         language: languageEl.value,
//         age: ageEl.value,
//         subject: subjectEl.value,
//         difficulty: difficultyEl.value,
//         question_type: questionTypeEl.value,
//         sub_type: subTypeEl.value,
//         role: 'default_role',
//         sex: sexEl.value,
//         answers: answersArr
//     };

//     try {
//         const response = await fetch('/submit_responses', {
//             method: 'POST',
//             body: JSON.stringify(payload),
//             headers: { 'Content-Type': 'application/json' }
//         });
//         localStorage.removeItem('casesBatch');


//         if (!response.ok) {
//             const errorData = await response.json();
//             displayError(errorData);
//             return;
//         }

//         const analysisData = await response.json();
//         if (analysisData && analysisData.analysis) {
//             // Display Gemini's analysis
//             displayAnalysis(analysisData.analysis);
            
//             // If performance data is available, display it as well.
//             // if (analysisData.performance) {
//             //     displayPerformance(analysisData.performance);
//             // }
//         } else {
//             displayError({ error: "Invalid analysis data received from server." });
//         }
//         responseContainer.classList.add('success');

//     } catch (error) {
//         displayError({ error: `Analysis failed: ${error}` });
//     } finally {
//         loadingSpinner.classList.add('hidden');
//         responseContainer.classList.remove('hidden');
//     }
// }


// // ... (rest of your functions) ...

// function displayAnalysis(analysis) {
//     responseContainer.innerHTML = `
//         <h2>Analysis Results</h2>
//         <div class="analysis-content">`;

//     if (typeof analysis === 'object') {
//         for (const key in analysis) {
//             responseContainer.innerHTML += `
//                 <div class="analysis-section">
//                     <h3 class="analysis-heading">${key}</h3>
//                     <ul class="analysis-list">`;
//             if (Array.isArray(analysis[key])) {
//                 analysis[key].forEach(item => {
//                     responseContainer.innerHTML += `<li class="analysis-item">${item}</li>`;
//                 });
//             } else {
//                 responseContainer.innerHTML += `<li class="analysis-item">${analysis[key]}</li>`;
//             }
//             responseContainer.innerHTML += `</ul>
//                 </div>`;
//         }
//     } else {
//         responseContainer.innerHTML += `<p class="analysis-text">${analysis}</p>`;
//     }

//     responseContainer.innerHTML += `</div>`;
// }

// // function displayPerformance(performanceData) {
// //     // Create a container for performance display
// //     const performanceContainer = document.createElement('div');
// //     performanceContainer.classList.add('performance-content');
// //     performanceContainer.innerHTML = `<h2>Detailed Scoring Points</h2>`;

// //     if (Array.isArray(performanceData)) {
// //         performanceData.forEach((caseItem, index) => {
// //             const caseDiv = document.createElement('div');
// //             caseDiv.classList.add('performance-case');

// //             // Assuming each case has at least a "case" description field.
// //             const caseDescription = caseItem.case || `Case ${index + 1}`;
// //             caseDiv.innerHTML = `<h3>${caseDescription}</h3>`;

// //             // Optionally, display the whole case JSON in a formatted way.
// //             const pre = document.createElement('pre');
// //             pre.textContent = JSON.stringify(caseItem, null, 4);
// //             caseDiv.appendChild(pre);

// //             performanceContainer.appendChild(caseDiv);
// //         });
// //     } else {
// //         performanceContainer.innerHTML += `<p>No performance data available.</p>`;
// //     }

// //     // Append a back button
// //     const backButton = document.createElement('button');
// //     backButton.id = 'backButton';
// //     backButton.textContent = 'Go back';
// //     backButton.addEventListener('click', () => {
// //         startNewGame();
// //         window.location.reload();
// //     });
// //     performanceContainer.appendChild(backButton);

// //     // Append the performance container to the responseContainer.
// //     responseContainer.appendChild(performanceContainer);
// // }


// function displayError(errorData) {
//     responseContainer.classList.add('error');
//     responseContainer.innerHTML = `
//         <p><strong>Error:</strong> ${errorData.error}</p>
//         <button id="tryAgainButton">Try Again</button>
//     `;

//     const tryAgainButton = document.getElementById('tryAgainButton');
//     tryAgainButton.addEventListener('click', () => {
//         responseContainer.innerHTML = '';
//         responseContainer.classList.remove('error');
//         form.querySelector('#language').disabled = false;
//         form.querySelector('#age').disabled = false;
//         form.querySelector('#subject').disabled = false;
//         form.querySelector('#difficulty').disabled = false;
//         form.querySelector('#question_type').disabled = false;
//         form.querySelector('button[type="submit"]').disabled = false;
//         responseContainer.classList.add('hidden');
//         form.submit();
//     });
// }
