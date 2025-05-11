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
    { value: 'interpersonal_skills', text: window.i18n ? window.i18n.getTranslation('interpersonal_skills') : 'Interpersonal Skills' },
    { value: 'ethical_dilemmas', text: window.i18n ? window.i18n.getTranslation('ethical_dilemmas') : 'Ethical Dilemmas' },
    { value: 'stress_management', text: window.i18n ? window.i18n.getTranslation('stress_management') : 'Stress Management' }
  ],
  study: [
    { value: 'subject_mastery', text: window.i18n ? window.i18n.getTranslation('subject_mastery') : 'Subject Mastery' },
    { value: 'critical_thinking', text: window.i18n ? window.i18n.getTranslation('critical_thinking') : 'Critical Thinking' },
    { value: 'practical_application', text: window.i18n ? window.i18n.getTranslation('practical_application') : 'Practical Application' }
  ],
  hiring: [
    { value: 'technical_skills', text: window.i18n ? window.i18n.getTranslation('technical_skills') : 'Technical Skills' },
    { value: 'behavioral_interview', text: window.i18n ? window.i18n.getTranslation('behavioral_interview') : 'Behavioral Interview' },
    { value: 'situational_judgment', text: window.i18n ? window.i18n.getTranslation('situational_judgment') : 'Situational Judgment' }
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
      option.text = window.i18n ? window.i18n.getTranslation(subtype.value) : subtype.text;
      option.setAttribute('data-translate', subtype.value);
      subTypeSelect.appendChild(option);
    });
  }
}

// Initialize subtype options
updateSubTypeOptions(questionTypeSelect.value);

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  loadingSpinner.classList.remove('hidden');
  loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'start' });

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
  loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'start' });

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

  // if (jsonData.message && jsonData.message === window.i18n.getTranslation("congratulations")) {
  //   displayCongratulations(jsonData.message);
  //   userAnswers = {}; // Reset userAnswers to an empty object
  //   return;
  // }

  if (jsonData.message && jsonData.message === "CONGRATULATIONS YOU FINISHED THE GAME") {
    const message = window.i18n.getTranslation("congratulations");
    displayCongratulations(message);
    userAnswers = {}; // Reset userAnswers to an empty object

    return;
  }

  if (!jsonData.data) {
    displayError({ error: window.i18n.getTranslation("error") + ": " + window.i18n.getTranslation("no_case") });
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
      displayError({ error: window.i18n.getTranslation("no_case") });
      return;
  }

  const caseData = casesBatch[currentCaseIndex];
  if (!caseData) {
      displayError({ error: window.i18n.getTranslation("no_case") });
      return;
  }

  const selected = document.querySelector(`input[name="${caseData.case_id}"]:checked`);
  if (!selected) {
      displayError({ error: window.i18n.getTranslation("please_select") });
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
    loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
  const imageEl = document.getElementById('allow_image'); // Always retrieve directly from the DOM
  const allowImageValue = imageEl && imageEl.checked ? 'on' : ''; // Use the checkbox's checked state

  if (!subTypeEl) {
    displayError({ error: window.i18n.getTranslation("error") });
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
    allow_image: allowImageValue // Use the updated value
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
    img.alt = window.i18n.getTranslation('generated_image_alt');
    img.classList.add('case-image');

    // Add placeholder text while the image is loading
    const placeholderText = document.createElement('p');
    placeholderText.innerText = 'Waiting for image...';
    placeholderText.classList.add('image-placeholder');
    caseElement.appendChild(placeholderText);

    img.addEventListener('load', () => {
      placeholderText.remove(); // Remove placeholder when image loads
    });

    img.addEventListener('error', () => {
      placeholderText.innerText = 'Failed to load image'; // Update placeholder on error
    });

    caseElement.appendChild(img);
  } else {
    const noImageText = document.createElement('p');
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
    <button id="newGameButton">${window.i18n.getTranslation('proceed')}</button>
    <button id="analyzeButton">${window.i18n.getTranslation('analyze_again')}</button>
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
  loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'start' });

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
      displayError({ error: window.i18n.getTranslation('no_analysis') });
    }
    responseContainer.classList.add('success');
  } catch (error) {
    console.error("analyzeResults: Exception occurred:", error);
    displayError({ error: `${window.i18n.getTranslation('error')}: ${error}` });
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
      <p><strong>${window.i18n.getTranslation('error')}:</strong> ${errorData.error}</p>
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
    <h2>${window.i18n.getTranslation('analysis_results')}</h2>
    <div class="result-summary">
      <p><strong>${window.i18n.getTranslation('score')}</strong> ${data.total_score}</p>
      <p><strong>${window.i18n.getTranslation('overall_assessment')}:</strong> ${data.max_total_score}</p>
    </div>
    <div class="result-details">
  `;
  data.results.forEach(result => {
    responseContainer.innerHTML += `
      <div class="case-result">
        <p><strong>${window.i18n.getTranslation('case')}:</strong> ${result.case_id}</p>
        <p><strong>${window.i18n.getTranslation('your_answer')}:</strong> ${result.score}</p>
        <p><strong>${window.i18n.getTranslation('correct_answer')}:</strong> ${result.max_score}</p>
      </div>
    `;
  });
  responseContainer.innerHTML += `</div>`;
  const newGameButton = document.createElement('button');
  newGameButton.id = 'newGameButton';
  newGameButton.textContent = window.i18n.getTranslation('start_game');
  newGameButton.addEventListener('click', startNewGame);
  responseContainer.appendChild(newGameButton);
}

function submitCurrentResponses() {
    if (currentCaseIndex >= casesBatch.length) {
      displayError({ error: window.i18n.getTranslation('no_case') });
      return;
    }

    const caseData = casesBatch[currentCaseIndex];
    if (!caseData) {
      displayError({ error: window.i18n.getTranslation('no_case') });
      return;
    }

    const selected = document.querySelector(`input[name="${caseData.case_id}"]:checked`);
    if (!selected) {
      displayError({ error: window.i18n.getTranslation('please_select') });
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
    loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'start' });

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
  const imageEl = document.getElementById('allow_image'); // Always retrieve directly from the DOM
  const allowImageValue = imageEl && imageEl.checked ? 'on' : ''; // Use the checkbox's checked state

  if (!subTypeEl) {
    displayError({ error: window.i18n.getTranslation('error') });
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
    allow_image: allowImageValue // Use the updated value
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
          displayError({ error: window.i18n.getTranslation('no_analysis') });
      }
      responseContainer.classList.add('success');
  } catch (error) {
      displayError({ error: `${window.i18n.getTranslation('error')}: ${error}` });
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
  if (unansweredCases > 4 || bufferedCases) {
      console.log("No need to fetch cases: sufficient cases available");
      return;
  }

  isBackgroundFetching = true;
  console.log("Starting background case fetch");

  const payload = {
      language: document.getElementById('language')?.value || localStorage.getItem('language') || "",
      age: document.getElementById('age')?.value || localStorage.getItem('age') || "",
      subject: document.getElementById('subject')?.value || localStorage.getItem('subject')?.value || "",
      difficulty: document.getElementById('difficulty')?.value || localStorage.getItem('difficulty') || "",
      question_type: document.getElementById('question_type')?.value || localStorage.getItem('question_type') || "",
      sub_type: document.getElementById('sub_type')?.value || "",
      role: 'default_role',
      sex: document.getElementById('sex')?.value || localStorage.getItem('sex') || "",
      allow_image: document.getElementById('allow_image')?.checked ? 'on' : '' // Always retrieve directly from the DOM
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
      displayError({ error: window.i18n.getTranslation('no_case') });
      return;
  }

  // Check remaining unanswered cases and trigger background fetch if needed
  if (checkBufferedCases) {
      const unansweredCases = checkUnansweredCases();
      if (unansweredCases <= 3 && !isBackgroundFetching && !bufferedCases) {
          console.log("Only 4 or fewer unanswered cases remaining, triggering background fetch");
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
  submitButton.innerText = window.i18n.getTranslation('next_case');
  submitButton.addEventListener('click', submitCurrentAnswer);
  caseForm.appendChild(submitButton);
  responseContainer.appendChild(submitButton);

  const submitButtonm = document.createElement('button');
  submitButtonm.type = 'button';
  submitButtonm.id = 'submitButtonm';
  submitButtonm.classList.add('submit-buttonm');
  submitButtonm.innerText = window.i18n.getTranslation('submit_analyze');
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
          displayError({ error: window.i18n.getTranslation('no_analysis') });
          return;
      }
  } else if (typeof analysis === "object" && analysis !== null) {
      analysisData = analysis;
      console.log("displayAnalysis: Analysis is already an object:", analysisData);
  } else {
      console.error("displayAnalysis: Invalid analysis data type received:", analysis);
      displayError({ error: window.i18n.getTranslation('no_analysis') });
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
      displayError({ error: window.i18n.getTranslation('no_analysis') });
      return;
  }

  console.log("displayAnalysis: Data validated successfully. Proceeding to render UI.");
  const overallJudgement = analysisData.overall_judgement || window.i18n.getTranslation('no_analysis');
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
                  <i class="fas fa-sync-alt"></i> ${window.i18n.getTranslation('analyze_again')}
              </button>
              <h2 class="analysis-title">${window.i18n.getTranslation('analysis_results')}</h2>
              <div class="score-display">
                  <span class="score-label">${window.i18n.getTranslation('score')}</span>
                  <span class="score-value ${percentageScore >= 70 ? 'good-score' : 'needs-improvement'}">
                      ${percentageScore}%
                  </span>
                  <span class="score-details">(${correctCases}/${totalCases} correct)</span>
              </div>
          </div>

          <!-- Overall Judgment Section -->
          <div class="overall-judgment">
              <h3>${window.i18n.getTranslation('overall_assessment')}</h3>
              <div class="judgment-content">
                  <i class="fas fa-chart-line judgment-icon"></i>
                  <p>${overallJudgement}</p>
              </div>
          </div>

          <!-- Cases Analysis Section -->
          <div class="cases-analysis">
              <h3>${window.i18n.getTranslation('detailed_case_analysis')}</h3>
              ${cases.map((caseItem, index) => `
                  <div class="case-analysis-item ${caseItem.player_choice === caseItem.optimal_choice ? 'correct' : 'incorrect'}">
                      <div class="case-number">${window.i18n.getTranslation('case')} ${index + 1}</div>
                      <div class="case-content">
                          <div class="case-question">
                              <i class="fas fa-question-circle"></i>
                              <p>${caseItem.case_description || window.i18n.getTranslation('no_case')}</p>
                          </div>
                          <div class="choices-comparison">
                              <div class="choice player-choice">
                                  <span class="choice-label">${window.i18n.getTranslation('your_answer')}</span>
                                  <span class="choice-value">${caseItem.player_choice || "N/A"}</span>
                              </div>
                              <div class="choice correct-choice">
                                  <span class="choice-label">${window.i18n.getTranslation('correct_answer')}</span>
                                  <span class="choice-value">${caseItem.optimal_choice || "N/A"}</span>
                              </div>
                          </div>
                          <div class="case-feedback">
                              <i class="fas fa-lightbulb"></i>
                              <p>${caseItem.analysis || window.i18n.getTranslation('no_analysis')}</p>
                          </div>
                      </div>
                  </div>
              `).join('')}
          </div>

          <!-- Action Buttons -->
          <div class="analysis-actions">
              <button id="proceedButton" class="proceed-button">
                  <i class="fas fa-arrow-right"></i> ${window.i18n.getTranslation('proceed')}
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

let generateButtonVisible = true;

function toggleGenerateButton(show) {
    const generateButton = document.getElementById('generateButton');
    if (generateButton) {
        generateButton.style.display = show ? 'block' : 'none';
        generateButtonVisible = show;
    }
}

// Scroll to the loading spinner when it is shown
loadingSpinner.addEventListener('transitionend', () => {
  if (!loadingSpinner.classList.contains('hidden')) {
    loadingSpinner.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
});

// --- ADDITIVE IMAGE POLLING FEATURE ---
(function() {
  // Only run after DOM is fully loaded
  window.addEventListener('DOMContentLoaded', function() {
    // Helper: check if images are enabled for this session
    function imagesEnabled() {
      const el = document.getElementById('allow_image');
      return el && (el.checked || el.value === 'on' || el.value === 'true');
    }

    // Helper: poll for image for a given case_id and update placeholder
    function pollForImage(caseId, placeholder) {
      let attempts = 0;
      const interval = setInterval(async function() {
        attempts++;
        try {
          const resp = await fetch(`/get_image/${caseId}`);
          if (!resp.ok) return;
          const data = await resp.json();
          if (data.generated_image_data === null) {
            placeholder.innerText = window.i18n?.getTranslation('image_failed') || 'Image generation failed.';
            clearInterval(interval);
          } else if (data.generated_image_data) {
            const img = document.createElement('img');
            img.src = data.generated_image_data;
            img.alt = window.i18n?.getTranslation('generated_image_alt') || 'Generated image';
            img.className = 'case-image';
            placeholder.replaceWith(img);
            clearInterval(interval);
          } else if (attempts > 60) {
            placeholder.innerText = window.i18n?.getTranslation('image_timeout') || 'Image not available.';
            clearInterval(interval);
          }
        } catch (e) {
          // Ignore errors, keep polling
        }
      }, 2000);
    }

    // Hook: after each case is rendered, add placeholder and polling if needed
    const origCreateCaseElement = window.createCaseElement;
    if (typeof origCreateCaseElement === 'function') {
      window.createCaseElement = function(caseData) {
        const el = origCreateCaseElement(caseData);
        // Only add if images are enabled and generated_image_data is null
        if (imagesEnabled() && !caseData.generated_image_data) {
          // Only add if not already present
          if (!el.querySelector('.image-placeholder')) {
            const placeholder = document.createElement('p');
            placeholder.className = 'image-placeholder';
            placeholder.innerText = window.i18n?.getTranslation('waiting_for_image') || 'Waiting for image...';
            el.appendChild(placeholder);
            pollForImage(caseData.case_id, placeholder);
          }
        } else if (!imagesEnabled()) {
          // If images are disabled, show a message
          if (!el.querySelector('.image-disabled-msg')) {
            const msg = document.createElement('p');
            msg.className = 'image-disabled-msg';
            msg.innerText = window.i18n?.getTranslation('images_disabled') || 'Images are disabled for this session.';
            el.appendChild(msg);
          }
        }
        return el;
      };
    }
  });
})();
// --- END ADDITIVE IMAGE POLLING FEATURE ---