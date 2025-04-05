const form = document.getElementById('generateForm');
const responseContainer = document.getElementById('response-container');
const loadingSpinner = document.getElementById('loading-spinner');
const caseForm = document.getElementById('caseForm');
const resetButton = document.getElementById('resetButton');
let bufferedCases = null;
let isBackgroundFetching = false;



// Define subtypes for each question type
const subtypes = {
  behavioral: [
    { value: 'interpersonal_skills', text: 'Interpersonal Skills' },
    { value: 'ethical_dilemmas', text: 'Ethical Dilemmas' },
    { value: 'stress_management', text: 'Stress Management' }
  ],
  study: [
    { value: 'subject_mastery', text: 'Subject Mastery' },
    { value: 'critical_thinking', text: 'Critical Thinking' },
    { value: 'practical_application', text: 'Practical Application' }
  ],
  hiring: [
    { value: 'technical_skills', text: 'Technical Skills' },
    { value: 'behavioral_interview', text: 'Behavioral Interview' },
    { value: 'situational_judgment', text: 'Situational Judgment' }
  ]
};

// Update subtype options based on question type selection
const questionTypeSelect = document.getElementById('question_type');
const subTypeSelect = document.getElementById('sub_type');

questionTypeSelect.addEventListener('change', (event) => {
  const selectedType = event.target.value;
  updateSubTypeOptions(selectedType);
});

function updateSubTypeOptions(selectedType) {
  subTypeSelect.innerHTML = '';
  if (subtypes[selectedType]) {
    subtypes[selectedType].forEach(subtype => {
      const option = document.createElement('option');
      option.value = subtype.value;
      option.text = subtype.text;
      subTypeSelect.appendChild(option);
    });
  }
}

// Initialize subtype options
updateSubTypeOptions(questionTypeSelect.value);

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  loadingSpinner.classList.remove('hidden');
  responseContainer.classList.add('hidden');
  responseContainer.classList.remove('success');
  responseContainer.classList.remove('error');

  const formData = new FormData(form);
  const data = {};
  formData.forEach((value, key) => data[key] = value);

  try {
    const response = await fetch('/generate_cases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    handleGenerateCasesResponse(response);
  } catch (error) {
    displayError({ error: error.message });
  } finally {
    loadingSpinner.classList.add('hidden');
    responseContainer.classList.remove('hidden');
  }
});

resetButton.addEventListener('click', async () => {
  loadingSpinner.classList.remove('hidden');
  responseContainer.classList.add('hidden');
  responseContainer.classList.remove('success');
  responseContainer.classList.remove('error');

  try {
    const response = await fetch('/reset', {
      method: 'POST'
    });

    if (!response.ok) {
      const errorData = await response.json();
      displayError(errorData);
      return;
    }

    const jsonData = await response.json();
    alert(jsonData.message);
    userAnswers = {}; // Reset userAnswers
    casesBatch = []; // Clear the current batch of cases
  
    window.location.reload();
  } catch (error) {
    displayError({ error: error.message });
  } finally {
    loadingSpinner.classList.add('hidden');
    responseContainer.classList.remove('hidden');
  }
});






function startNewGame() {
  // Reset game state if necessary
  userAnswers = {}; // Reset userAnswers
  casesBatch = []; // Clear the current batch of cases

  window.location.reload();

}

// Global variables for managing current batch of cases
let currentCaseIndex = 0;
let casesBatch = [];
let userAnswers = {}; // Changed to an object
let batchCounter = 0;



async function handleGenerateCasesResponse(response) {
  if (!response.ok) {
    const errorData = await response.json();
    displayError(errorData);
    return;
  }


  responseContainer.innerHTML = `

  <div class="case-content">`;


  responseContainer.innerHTML = `

  <div class="case-content">`;

  const jsonData = await response.json();

  if (jsonData.message && jsonData.message === "CONGRATULATIONS YOU FINISHED THE GAME") {
    displayCongratulations(jsonData.message);
    userAnswers = {}; // Reset userAnswers to an empty object

    return;
  }

  if (!jsonData.data) {
    displayError({ error: "Server response missing 'data' field." });
    return;
  }

  batchCounter++;
  casesBatch = Array.isArray(jsonData.data) ? jsonData.data : [jsonData.data];

  try {
    // Clear storage before setting new data
    localStorage.removeItem('casesBatch');

    // Give a slight delay to allow storage to be freed up
    await new Promise(resolve => setTimeout(resolve, 10));

    // Store the new batch
    localStorage.setItem('casesBatch', JSON.stringify(casesBatch));
  } catch (error) {
    console.error("Storage Error:", error);
    // displayError({ error: "Failed to store data. Try clearing browser storage." });
  }

  currentCaseIndex = 0;
  displayCurrentCase();
}











function submitCurrentAnswer() {
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

  userAnswers[caseData.case_id] = parseInt(selected.value, 10); // Store answer with case_id as key
  currentCaseIndex++;

  if (currentCaseIndex < casesBatch.length) {
      displayCurrentCase();
  } else {
      // Check if we have buffered cases before calling backend
      if (bufferedCases && bufferedCases.length > 0) {
          console.log("Using buffered cases instead of calling backend");
          casesBatch = bufferedCases;
          bufferedCases = null;
          currentCaseIndex = 0;
          displayCurrentCase();
      } else {
          console.log("No buffered cases available, calling backend");
          const submitBtn = document.querySelector('.submit-button');
          if (submitBtn) {
              submitBtn.disabled = true;
          }
          sendAnswersToBackend(userAnswers); // Pass userAnswers object
      }
  }
}




async function sendAnswersToBackend(userAnswers) { // Changed parameter name
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
  const imageEl = document.getElementById('allow_image') || { value: localStorage.getItem('allow_image') || "" };

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
    answers: userAnswers, // Send userAnswers object
    allow_image: imageEl.value
  };

  try {
    const response = await fetch('/generate_cases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    localStorage.removeItem('casesBatch');
    await handleGenerateCasesResponse(response);
  } catch (error) {
    displayError({ error: error.message });
  } finally {
    if (loadingSpinner) {
      loadingSpinner.classList.add('hidden');
    }
    if (responseContainer) {
      responseContainer.classList.remove('hidden');
    }
  }
}

function createCaseElement(caseData) {
  const caseElement = document.createElement('div');
  caseElement.classList.add('case-container');

  const caseTitle = document.createElement('div');
  caseTitle.classList.add('case-title');
  caseTitle.innerText = caseData.case;
  caseElement.appendChild(caseTitle);

  // Display generated image if available
  if (caseData.generated_image_data) {
    const img = document.createElement('img');
    img.src = caseData.generated_image_data;
    img.alt = 'Generated image for case';
    img.classList.add('case-image');
    caseElement.appendChild(img);
  } else {
    const noImageText = document.createElement('p');
    noImageText.innerText = '';
    noImageText.innerText = '';
    caseElement.appendChild(noImageText);
  }

  const hiddenInput = document.createElement('input');
  hiddenInput.type = 'hidden';
  hiddenInput.name = 'case_id';
  hiddenInput.value = caseData.case_id || '';
  caseElement.appendChild(hiddenInput);

  caseData.options.forEach(option => {
    const optionElement = document.createElement('div');
    optionElement.classList.add('option');

    const radioInput = document.createElement('input');
    radioInput.type = 'radio';
    radioInput.name = caseData.case_id; // Use case_id as name
    radioInput.value = option.number;
    radioInput.id = `option_${option.option_id}`;

    const label = document.createElement('label');
    label.htmlFor = `option_${option.option_id}`;
    label.innerText = `${option.number}. ${option.option}`;

    optionElement.appendChild(radioInput);
    optionElement.appendChild(label);
    caseElement.appendChild(optionElement);

    // Add a horizontal line after each option
    const hr = document.createElement('hr');
    caseElement.appendChild(hr);
  });

  return caseElement;
}

function displayCongratulations(message) {
  responseContainer.classList.add('success');
  responseContainer.innerHTML = `
    <h2>${message}</h2>
    <button id="newGameButton">Finish</button>
    <button id="analyzeButton">Analyze </button>
  `;
  const newGameButton = document.getElementById('newGameButton');
  newGameButton.addEventListener('click', startNewGame);
  attachAnalyzeButtonListener();
}

function attachAnalyzeButtonListener() {
  const analyzeButton = document.getElementById('analyzeButton');
  if (analyzeButton) {
    analyzeButton.addEventListener('click', analyzeResults);
  } else {
    console.error("Analyze button not found!");
  }
}

async function analyzeResults() {
  console.log("analyzeResults: Function started.");
  loadingSpinner.classList.remove('hidden');
  responseContainer.classList.add('hidden');
  responseContainer.classList.remove('success');
  responseContainer.classList.remove('error');

  const language = document.getElementById('language').value;
  const role = document.getElementById('role').value;
  const question_type = document.getElementById('question_type').value;

  console.log("analyzeResults: Sending request with", { role, question_type, language });
  try {
    const response = await fetch('/analysis', {
      method: 'POST',
      body: JSON.stringify({ role: role, question_type: question_type, language: language }),
      headers: { 'Content-Type': 'application/json' }
    });
    console.log("analyzeResults: Response status", response.status);

    if (!response.ok) {
      const errorData = await response.json();
      console.error("analyzeResults: Error response from server", errorData);
      displayError(errorData);
      return;
    }

    const analysisData = await response.json();
    console.log("analyzeResults: Raw analysisData received:", analysisData);

    // Instead of expecting analysisData.analysis, pass the entire analysisData.
    if (analysisData && analysisData.cases) {
      console.log("analyzeResults: Valid analysisData detected. Passing to displayAnalysis.");
      displayAnalysis(analysisData);
    } else {
      console.error("analyzeResults: Invalid analysis data structure:", analysisData);
      displayError({ error: "Invalid analysis data received from server." });
    }
    responseContainer.classList.add('success');
  } catch (error) {
    console.error("analyzeResults: Exception occurred:", error);
    displayError({ error: `Analysis failed: ${error}` });
  } finally {
    loadingSpinner.classList.add('hidden');
    responseContainer.classList.remove('hidden');
    console.log("analyzeResults: Finished execution.");
  }
}


function displayError(errorData) {
    // Create the modal container
    const modal = document.createElement('div');
    modal.id = 'errorModal';
    modal.style.position = 'fixed';
    modal.style.top = '50%';
    modal.style.left = '50%';
    modal.style.transform = 'translate(-50%, -50%)';
    modal.style.backgroundColor = '#fff';
    modal.style.padding = '20px';
    modal.style.borderRadius = '8px';
    modal.style.boxShadow = '0px 4px 10px rgba(0, 0, 0, 0.2)';
    modal.style.zIndex = '1000';
    modal.style.textAlign = 'center';
    modal.style.width = '300px';

    // Close button (X)
    const closeButton = document.createElement('span');
    closeButton.innerHTML = '&times;';
    closeButton.style.position = 'absolute';
    closeButton.style.top = '10px';
    closeButton.style.right = '15px';
    closeButton.style.fontSize = '20px';
    closeButton.style.cursor = 'pointer';
    closeButton.style.fontWeight = 'bold';
    closeButton.style.color = '#333';

    // Close modal when clicking the close button
    closeButton.addEventListener('click', closeModal);

    // Add error message
    modal.innerHTML = `
      <p><strong>Error:</strong> ${errorData.error}</p>
    `;
    modal.appendChild(closeButton);

    // Create a backdrop
    const backdrop = document.createElement('div');
    backdrop.id = 'modalBackdrop';
    backdrop.style.position = 'fixed';
    backdrop.style.top = '0';
    backdrop.style.left = '0';
    backdrop.style.width = '100%';
    backdrop.style.height = '100%';
    backdrop.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
    backdrop.style.zIndex = '999';

    // Close modal when clicking outside of it
    backdrop.addEventListener('click', closeModal);

    // Append modal and backdrop to the document body
    document.body.appendChild(backdrop);
    document.body.appendChild(modal);

    // Function to close the modal
    function closeModal() {
      document.body.removeChild(modal);
      document.body.removeChild(backdrop);
    }
  }




function displayResults(data) {
  responseContainer.innerHTML = `
    <h2>Results</h2>
    <div class="result-summary">
      <p><strong>Your Score:</strong> ${data.total_score}</p>
      <p><strong>Max Possible Score:</strong> ${data.max_total_score}</p>
    </div>
    <div class="result-details">
  `;
  data.results.forEach(result => {
    responseContainer.innerHTML += `
      <div class="case-result">
        <p><strong>Case ID:</strong> ${result.case_id}</p>
        <p><strong>Your Score:</strong> ${result.score}</p>
        <p><strong>Max Score:</strong> ${result.max_score}</p>
      </div>
    `;
  });
  responseContainer.innerHTML += `</div>`;
  const newGameButton = document.createElement('button');
  newGameButton.id = 'newGameButton';
  newGameButton.textContent = 'New Game';
  newGameButton.addEventListener('click', startNewGame);
  responseContainer.appendChild(newGameButton);
}



function submitCurrentResponses() {
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

    userAnswers[caseData.case_id] = parseInt(selected.value, 10);
    currentCaseIndex++;


      const submitBtn = document.querySelector('.submit-button');
      if (submitBtn) {
        submitBtn.disabled = true;
      }
      submitResponses(userAnswers);
  }






  async function submitResponses(userAnswers) {
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
    const imageEl = document.getElementById('allow_image') || { value: localStorage.getItem('allow_image') || "" };


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
      answers: userAnswers,


      allow_image: imageEl.value


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
        if (analysisData) {
            displayAnalysis(analysisData);

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
























function checkUnansweredCases() {
  // Count only cases that haven't been answered yet
  let unansweredCount = 0;
  for (let i = currentCaseIndex; i < casesBatch.length; i++) {
      if (typeof userAnswers[casesBatch[i].case_id] === 'undefined' || userAnswers[casesBatch[i].case_id] === null) {
          unansweredCount++;
      }
  }
  console.log(`Found ${unansweredCount} unanswered cases remaining`);
  return unansweredCount;
}





function proceedCases() {
    console.log("proceedCases: Starting with buffered cases:", bufferedCases?.length);

    if (bufferedCases && bufferedCases.length > 0) {
        // Use the buffered cases
        casesBatch = bufferedCases;
        bufferedCases = null;
        currentCaseIndex = 0;
        userAnswers = {}; // Reset userAnswers
        displayCurrentCase();
        console.log("proceedCases: Using buffered cases");
    } else {
        // Fallback to original behavior
        console.log("proceedCases: No buffered cases available, falling back to sendAnswersToBackend");
        sendAnswersToBackend(userAnswers);
    }
}





async function fetchCasesInBackground(userAnswers = null) {
  // Prevent multiple simultaneous fetches
  if (isBackgroundFetching) return;

  // Check if we actually need new cases
  const unansweredCases = checkUnansweredCases();
  if (unansweredCases > 1 || bufferedCases) {
      console.log("No need to fetch cases: sufficient cases available");
      return;
  }

  isBackgroundFetching = true;
  console.log("Starting background case fetch");

  const payload = {
      language: document.getElementById('language')?.value || localStorage.getItem('language') || "",
      age: document.getElementById('age')?.value || localStorage.getItem('age') || "",
      subject: document.getElementById('subject')?.value || localStorage.getItem('subject') || "",
      difficulty: document.getElementById('difficulty')?.value || localStorage.getItem('difficulty') || "",
      question_type: document.getElementById('question_type')?.value || localStorage.getItem('question_type') || "",
      sub_type: document.getElementById('sub_type')?.value || "",
      role: 'default_role',
      sex: document.getElementById('sex')?.value || localStorage.getItem('sex') || "",
      allow_image: document.getElementById('allow_image')?.value || localStorage.getItem('allow_image') || ""
  };

  // Include answersArr in the payload if provided
  if (userAnswers) {
      payload.answers = userAnswers;
  }

  try {
      const response = await fetch('/generate_cases', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
      });

      if (!response.ok) {
          console.error('Background case fetching failed:', response.status);
          return;
      }

      const jsonData = await response.json();
      if (jsonData.data) {
          bufferedCases = Array.isArray(jsonData.data) ? jsonData.data : [jsonData.data];
          console.log('Successfully buffered new cases:', bufferedCases.length);
      }
  } catch (error) {
      console.error('Error in background case fetching:', error);
  } finally {
      isBackgroundFetching = false;
  }
}

function displayCurrentCase(checkBufferedCases = true) {
  caseForm.innerHTML = '';
  responseContainer.innerHTML = '';

  if (casesBatch.length === 0 || currentCaseIndex >= casesBatch.length) {
      displayError({ error: "No case available." });
      return;
  }

  // Check remaining unanswered cases and trigger background fetch if needed
  if (checkBufferedCases) {
      const unansweredCases = checkUnansweredCases();
      if (unansweredCases <= 1 && !isBackgroundFetching && !bufferedCases) {
          console.log("Only 1 or fewer unanswered cases remaining, triggering background fetch");
          fetchCasesInBackground(userAnswers);
      }
  }

  const caseData = casesBatch[currentCaseIndex];
  const caseElement = createCaseElement(caseData);
  caseForm.appendChild(caseElement);
  responseContainer.appendChild(caseElement);


      // Hide the generate button when cases are displayed
      toggleGenerateButton(false);

      responseContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });


  const submitButton = document.createElement('button');
  submitButton.type = 'button';
  submitButton.classList.add('submit-button');
  submitButton.innerText = 'Next Case';
  submitButton.addEventListener('click', submitCurrentAnswer);
  caseForm.appendChild(submitButton);
  responseContainer.appendChild(submitButton);

  const submitButtonm = document.createElement('button');
  submitButtonm.type = 'button';
  submitButtonm.id = 'submitButtonm';
  submitButtonm.classList.add('submit-buttonm');
  submitButtonm.innerText = 'Submit And Analyze';
  submitButtonm.addEventListener('click', submitCurrentResponses);
  caseForm.appendChild(submitButtonm);
  responseContainer.appendChild(submitButtonm);
}

function displayAnalysis(analysis) {
  console.log("displayAnalysis: Function started with input:", analysis);
  let analysisData;
      // Hide the generate button when cases are displayed
      toggleGenerateButton(false);


  // Start background fetch immediately without answersArr
  fetchCasesInBackground();

  // If analysis is a string, try to parse it.
  if (typeof analysis === "string") {
      console.log("displayAnalysis: Analysis is a string. Attempting to parse.");
      try {
          analysisData = JSON.parse(analysis);
          console.log("displayAnalysis: Parsed analysisData from string:", analysisData);
      } catch (e) {
          console.error("displayAnalysis: Failed to parse analysis string:", analysis);
          displayError({ error: "Analysis data is not valid JSON." });
          return;
      }
  } else if (typeof analysis === "object" && analysis !== null) {
      analysisData = analysis;
      console.log("displayAnalysis: Analysis is already an object:", analysisData);
  } else {
      console.error("displayAnalysis: Invalid analysis data type received:", analysis);
      displayError({ error: "Invalid analysis data type received from server." });
      return;
  }

  // If the response is wrapped in a "data" property, unwrap it.
  if (analysisData.data) {
      console.log("displayAnalysis: Unwrapping nested data property.");
      analysisData = analysisData.data;
  }

  // Debug log for unexpected structure.
  if (!analysisData || typeof analysisData !== "object" || !Array.isArray(analysisData.cases)) {
      console.error("displayAnalysis: Unexpected analysis data structure:", analysisData);
      displayError({ error: "Invalid analysis data received from server." });
      return;
  }

  console.log("displayAnalysis: Data validated successfully. Proceeding to render UI.");
  const overallJudgement = analysisData.overall_judgement || "No overall judgment available";
  const cases = analysisData.cases;

  // Calculate percentage score
  const correctCases = cases.filter(caseItem => caseItem.player_choice === caseItem.optimal_choice).length;
  const totalCases = cases.length;
  const percentageScore = ((correctCases / totalCases) * 100).toFixed(1);

  responseContainer.innerHTML = `
      <div class="analysis-container">
          <!-- Header Section -->
          <div class="analysis-header">
              <button id="againButton" class="analysis-button">
                  <i class="fas fa-sync-alt"></i> Analyze Again
              </button>
              <h2 class="analysis-title">Analysis Results</h2>
              <div class="score-display">
                  <span class="score-label">Score:</span>
                  <span class="score-value ${percentageScore >= 70 ? 'good-score' : 'needs-improvement'}">
                      ${percentageScore}%
                  </span>
                  <span class="score-details">(${correctCases}/${totalCases} correct)</span>
              </div>
          </div>

          <!-- Overall Judgment Section -->
          <div class="overall-judgment">
              <h3>Overall Assessment</h3>
              <div class="judgment-content">
                  <i class="fas fa-chart-line judgment-icon"></i>
                  <p>${overallJudgement}</p>
              </div>
          </div>

          <!-- Cases Analysis Section -->
          <div class="cases-analysis">
              <h3>Detailed Case Analysis</h3>
              ${cases.map((caseItem, index) => `
                  <div class="case-analysis-item ${caseItem.player_choice === caseItem.optimal_choice ? 'correct' : 'incorrect'}">
                      <div class="case-number">Case ${index + 1}</div>
                      <div class="case-content">
                          <div class="case-question">
                              <i class="fas fa-question-circle"></i>
                              <p>${caseItem.case_description || "No description available"}</p>
                          </div>
                          <div class="choices-comparison">
                              <div class="choice player-choice">
                                  <span class="choice-label">Your Answer:</span>
                                  <span class="choice-value">${caseItem.player_choice || "N/A"}</span>
                              </div>
                              <div class="choice correct-choice">
                                  <span class="choice-label">Correct Answer:</span>
                                  <span class="choice-value">${caseItem.optimal_choice || "N/A"}</span>
                              </div>
                          </div>
                          <div class="case-feedback">
                              <i class="fas fa-lightbulb"></i>
                              <p>${caseItem.analysis || "No analysis available"}</p>
                          </div>
                      </div>
                  </div>
              `).join('')}
          </div>

          <!-- Action Buttons -->
          <div class="analysis-actions">
              <button id="proceedButton" class="proceed-button">
                  <i class="fas fa-arrow-right"></i> Proceed
              </button>
          </div>
      </div>
  `;

  // Add event listeners
  const againButton = document.getElementById("againButton");
  if (againButton) {
      console.log("displayAnalysis: Adding event listener to 'againButton'.");
      againButton.addEventListener("click", () => {
          analyzeResults();
      });
  }

  const proceedButton = document.getElementById("proceedButton");
  if (proceedButton) {
      console.log("displayAnalysis: Adding event listener to 'proceedButton'.");
      proceedButton.addEventListener("click", proceedCases);
  }

  console.log("displayAnalysis: UI rendered successfully.");
}

