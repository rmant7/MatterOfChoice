const form = document.getElementById('generateFormV2');
const responseContainer = document.getElementById('response-container');
const loadingSpinner = document.getElementById('loading-spinner');
const questionTypeSelect = document.getElementById('question_type');
const subTypeSelect = document.getElementById('sub_type');
const actionButtons = document.getElementById('action-buttons');
const nextButton = document.getElementById('nextButton');
const submitAnalysisButton = document.getElementById('submitAnalysisButton');

let casesBatch = [];
let currentCaseIndex = 0;
let userAnswers = {};

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

questionTypeSelect.addEventListener('change', (event) => {
  updateSubTypeOptions(event.target.value);
});

// Initialize subtype options
updateSubTypeOptions(questionTypeSelect.value);

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    loadingSpinner.classList.remove('hidden');
    responseContainer.innerHTML = '';
    actionButtons.classList.add('hidden');

    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => data[key] = value);

    try {
        const response = await fetch('/start_case_generation', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.status !== 202) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to start case generation.');
        }

        const { job_id } = await response.json();

        const pollJobStatus = async () => {
            try {
                const statusResponse = await fetch(`/get_job_status/${job_id}`);
                if (!statusResponse.ok) {
                    throw new Error('Failed to get job status.');
                }

                const job = await statusResponse.json();

                if (job.status === 'complete') {
                    loadingSpinner.classList.add('hidden');
                    casesBatch = job.result;
                    currentCaseIndex = 0;
                    userAnswers = {};
                    displayCurrentCase();
                    actionButtons.classList.remove('hidden');
                } else if (job.status === 'failed') {
                    throw new Error(job.error || 'Case generation failed.');
                } else {
                    setTimeout(pollJobStatus, 2000);
                }
            } catch (error) {
                loadingSpinner.classList.add('hidden');
                responseContainer.innerHTML = `<p class="error">${error.message}</p>`;
            }
        };

        pollJobStatus();

    } catch (error) {
        loadingSpinner.classList.add('hidden');
        responseContainer.innerHTML = `<p class="error">${error.message}</p>`;
    }
});

function displayCurrentCase() {
    const caseData = casesBatch[currentCaseIndex];
    let casesHtml = `
        <div class="case-card">
            <h3>${caseData.case}</h3>
            <div class="case-options">
                ${caseData.options.map(option => `
                    <div class="option">
                        <input type="radio" name="${caseData.case_id}" value="${option.number}" id="option_${option.option_id}">
                        <label for="option_${option.option_id}">${option.number}. ${option.option}</label>
                    </div>
                `).join('')}
            </div>
        </div>
    `;
    responseContainer.innerHTML = casesHtml;
}

nextButton.addEventListener('click', () => {
    const selectedOption = document.querySelector(`input[name="${casesBatch[currentCaseIndex].case_id}"]:checked`);
    if (selectedOption) {
        userAnswers[casesBatch[currentCaseIndex].case_id] = selectedOption.value;
    } else {
        // Handle case where no option is selected
        alert('Please select an answer.');
        return;
    }

    currentCaseIndex++;
    if (currentCaseIndex < casesBatch.length) {
        displayCurrentCase();
    } else {
        // End of cases
        responseContainer.innerHTML = `<p>All cases have been answered. Submit for analysis.</p>`;
        nextButton.style.display = 'none';
    }
});

submitAnalysisButton.addEventListener('click', async () => {
    if (currentCaseIndex < casesBatch.length) {
        const selectedOption = document.querySelector(`input[name="${casesBatch[currentCaseIndex].case_id}"]:checked`);
        if (selectedOption) {
            userAnswers[casesBatch[currentCaseIndex].case_id] = selectedOption.value;
        } else {
            alert('Please select an answer.');
            return;
        }
    }

    loadingSpinner.classList.remove('hidden');
    responseContainer.innerHTML = '';
    actionButtons.classList.add('hidden');

    const data = {
        answers: userAnswers,
        language: document.getElementById('language').value,
        role: 'default_role',
        question_type: document.getElementById('question_type').value,
        sub_type: document.getElementById('sub_type').value
    };

    try {
        const response = await fetch('/submit_responses', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to submit for analysis.');
        }

        const analysisData = await response.json();
        loadingSpinner.classList.add('hidden');
        displayAnalysis(analysisData);

    } catch (error) {
        loadingSpinner.classList.add('hidden');
        responseContainer.innerHTML = `<p class="error">${error.message}</p>`;
    }
});

function displayAnalysis(analysisData) {
    let analysisHtml = `
        <div class="analysis-container">
            <h2>Analysis Results</h2>
            <div class="overall-judgement">
                <h3>Overall Judgement</h3>
                <p>${analysisData.overall_judgement}</p>
            </div>
            <div class="cases-analysis">
                <h3>Detailed Analysis</h3>
                ${analysisData.cases.map(caseItem => `
                    <div class="case-analysis-item">
                        <h4>${caseItem.case_description}</h4>
                        <p><strong>Your Choice:</strong> ${caseItem.player_choice}</p>
                        <p><strong>Optimal Choice:</strong> ${caseItem.optimal_choice}</p>
                        <p><strong>Analysis:</strong> ${caseItem.analysis}</p>
                    </div>
                `).join('')}
            </div>
        </div>
    `;
    responseContainer.innerHTML = analysisHtml;
}