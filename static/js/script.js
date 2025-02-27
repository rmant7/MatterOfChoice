const form = document.getElementById('generateForm');
const responseContainer = document.getElementById('response-container');
const loadingSpinner = document.getElementById('loading-spinner');
const caseForm = document.getElementById('caseForm');
const resetButton = document.getElementById('resetButton');

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

caseForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  loadingSpinner.classList.remove('hidden');
  responseContainer.classList.add('hidden');
  responseContainer.classList.remove('success');
  responseContainer.classList.remove('error');
  const formData = new FormData(form);
  const data = {};
  formData.forEach((value, key) => data[key] = value);

  const caseElements = caseForm.querySelectorAll('.case-container');
  const selectedOption = caseElements[0].querySelector('input[type="radio"]:checked');

  if (!selectedOption) {
    alert('Please select an option.');
    loadingSpinner.classList.add('hidden');
    return;
  }

  const selectedOptionIndex = Array.from(caseElements[0].querySelectorAll('input[type="radio"]')).findIndex(radio => radio.checked) + 1;
  data.answers = selectedOptionIndex;

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
}

// Global variables for managing current batch of cases
let currentCaseIndex = 0;
let casesBatch = [];
let answersBatch = [];
let batchCounter = 0;



async function handleGenerateCasesResponse(response) {
  if (!response.ok) {
    const errorData = await response.json();
    displayError(errorData);
    return;
  }

  const jsonData = await response.json();

  if (jsonData.message && jsonData.message === "CONGRATULATIONS YOU FINISHED THE GAME") {
    displayCongratulations(jsonData.message);
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
  answersBatch = new Array(casesBatch.length);
  displayCurrentCase();
}


function displayCurrentCase() {
    caseForm.innerHTML = '';
  
    if (casesBatch.length === 0 || currentCaseIndex >= casesBatch.length) {
      displayError({ error: "No case available." });
      return;
    }
  
    const caseData = casesBatch[currentCaseIndex];
    const caseElement = createCaseElement(caseData);
    caseForm.appendChild(caseElement);
  
  //   if (batchCounter > 1 && currentCaseIndex % 3 === 0) {
  //     const analyzeButton = document.createElement('button');
  //     analyzeButton.type = 'button';
  //     analyzeButton.id = 'analyzeButton';
  //     analyzeButton.classList.add('analyze-button');
  //     analyzeButton.innerText = 'Analyze Results';
  //     caseForm.appendChild(analyzeButton);
  //     attachAnalyzeButtonListener();
  //   }
  
    const submitButton = document.createElement('button');
    submitButton.type = 'button';
    submitButton.classList.add('submit-button');
    submitButton.innerText = 'Next Case';
    submitButton.addEventListener('click', submitCurrentAnswer);
    caseForm.appendChild(submitButton);
  
  
  
    const submitButtonm = document.createElement('button');
    submitButtonm.type = 'button';
    submitButtonm.id = 'submitButtonm';
    submitButtonm.classList.add('submit-buttonm');
    submitButtonm.innerText = 'Submit And Analyze';
    submitButtonm.addEventListener('click', submitCurrentAnswerM);
    caseForm.appendChild(submitButtonm);
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

async function sendAnswersToBackend(answersArr) {
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
    noImageText.innerText = 'No image available.';
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
    radioInput.name = caseData.case_id;
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
    <button id="newGameButton">Start New Game</button>
    <button id="analyzeButton">Analyze Results</button>
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
  loadingSpinner.classList.remove('hidden');
  responseContainer.classList.add('hidden');
  responseContainer.classList.remove('success');
  responseContainer.classList.remove('error');


  const language = document.getElementById('language').value;

  const role = document.getElementById('role').value;
  const question_type = document.getElementById('question_type').value;

  try {
    const response = await fetch('/analysis', {
      method: 'POST',
      body: JSON.stringify({ role: role, question_type: question_type, language: language }),
      headers: { 'Content-Type': 'application/json' }
    });

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

function displayAnalysis(analysis) {
  responseContainer.innerHTML = `
    <button id="backButton">Clear Analysisk</button>
    <h2>Analysis Results</h2>
    <div class="analysis-content">`;
  
  if (typeof analysis === 'object') {
    for (const key in analysis) {
      responseContainer.innerHTML += `
        <div class="analysis-section">
          <h3 class="analysis-heading">${key}</h3>
          <ul class="analysis-list">`;
      if (Array.isArray(analysis[key])) {
        analysis[key].forEach(item => {
          responseContainer.innerHTML += `<li class="analysis-item">${item}</li>`;
        });
      } else {
        responseContainer.innerHTML += `<li class="analysis-item">${analysis[key]}</li>`;
      }
      responseContainer.innerHTML += `</ul>
        </div>`;
    }
  } else {
    responseContainer.innerHTML += `<p class="analysis-text">${analysis}</p>`;
  }
  
  responseContainer.innerHTML += `
    </div>
  `;
  
  const backButton = document.getElementById('backButton');
  backButton.addEventListener('click', () => {
    startNewGame();
    window.location.reload();
  });
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
  
  

// Placeholder scoring functions
function calculateTotalScore(answers) {
  return answers.length * 5;
}
function calculateMaxTotalScore(answers) {
  return answers.length * 10;
}
function calculateScoreForAnswer(answer) {
  return Math.floor(Math.random() * 6);
}
function calculateMaxScoreForAnswer(answer) {
  return 10;
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
  

      const submitBtn = document.querySelector('.submit-button');
      if (submitBtn) {
        submitBtn.disabled = true;
      }
      sendAnswersToBackendM(answersBatch);
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
  

