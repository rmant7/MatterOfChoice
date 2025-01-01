const form = document.getElementById('generateForm');
const responseContainer = document.getElementById('response-container');
const loadingSpinner = document.getElementById('loading-spinner');
const caseForm = document.getElementById('caseForm');

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
        return; // Prevent submission
    }

    // Get the selected option's index (1-based)
    const selectedOptionIndex = Array.from(caseElements[0].querySelectorAll('input[type="radio"]')).findIndex(radio => radio.checked) + 1;
    data.answers = selectedOptionIndex; // Send the selected option index as the answer

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

// ... (rest of the code remains unchanged) ...






async function submitAnswers(answers) {
    loadingSpinner.classList.remove('hidden');
    responseContainer.classList.add('hidden');
    responseContainer.classList.remove('success');
    responseContainer.classList.remove('error');

    try {
        // Simulate a successful response
        const data = {
            total_score: calculateTotalScore(answers), // You'll need to implement this function
            max_total_score: calculateMaxTotalScore(answers), // You'll need to implement this function
            results: answers.map(answer => ({
                case_id: answer.case_id,
                score: calculateScoreForAnswer(answer), // You'll need to implement this function
                max_score: calculateMaxScoreForAnswer(answer) // You'll need to implement this function
            }))
        };

        displayResults(data);
        responseContainer.classList.add('success');


    } catch (error) {
        // This would handle errors in your score calculation logic, if any.
        displayError({ error: error.message }); 
    } finally {
        loadingSpinner.classList.add('hidden');
        responseContainer.classList.remove('hidden');
    }
}

// Placeholder functions - you'll need to define the actual scoring logic.
function calculateTotalScore(answers) {
    // Replace with your actual score calculation
    return answers.length * 5; // Example: 5 points per answer
}


function calculateMaxTotalScore(answers) {
    // Replace with your actual max score calculation
    return answers.length * 10; // Example: 10 max points per answer
}

function calculateScoreForAnswer(answer) {
    // Replace with your actual score calculation for a single answer
    return Math.floor(Math.random() * 6); // Example: random score between 0 and 5
}

function calculateMaxScoreForAnswer(answer) {
     // Replace with your actual max score calculation for a single answer
    return 10; // Example: 10 max points per answer
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

    // Add New Game button
    const newGameButton = document.createElement('button');
    newGameButton.id = 'newGameButton';
    newGameButton.textContent = 'New Game';
    newGameButton.addEventListener('click', startNewGame);
    responseContainer.appendChild(newGameButton);
}

function displayCases(data) {
    caseForm.innerHTML = ''; // Clear previous cases

    if (!data || !data.data) {
        displayError({ error: "Invalid or empty response from server" });
        return;
    }

    // Handle single case scenario
    if (!Array.isArray(data.data)) {
        const caseData = data.data;
        const caseElement = createCaseElement(caseData);
        caseForm.appendChild(caseElement);
    }
    // Handle multiple cases scenario (assuming array of case objects)

    else if (Array.isArray(data.data)) {
        data.data.forEach(caseData => {
            const caseElement = createCaseElement(caseData);
            caseForm.appendChild(caseElement);
        });
    } else {
        displayError({ error: "Invalid data format from server." });
    }


    const submitButton = document.createElement('button');
    submitButton.type = 'submit';
    submitButton.classList.add('submit-button');
    submitButton.innerText = 'Submit Answers';
    caseForm.appendChild(submitButton);
}


function createCaseElement(caseData) {
    const caseElement = document.createElement('div');
    caseElement.classList.add('case-container');

    const caseTitle = document.createElement('div');
    caseTitle.classList.add('case-title');
    caseTitle.innerText = caseData.case;
    caseElement.appendChild(caseTitle);

    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'case_id';
    hiddenInput.value = caseData.case_id;
    caseElement.appendChild(hiddenInput);

    caseData.options.forEach(option => {
        const optionElement = document.createElement('div');
        optionElement.classList.add('option');

        const radioInput = document.createElement('input');
        radioInput.type = 'radio';
        radioInput.name = caseData.case_id;
        radioInput.value = option.option_id;
        radioInput.id = `option_${option.option_id}`;

        const label = document.createElement('label');
        label.htmlFor = `option_${option.option_id}`;
        label.innerText = `${option.number}. ${option.option}`;

        optionElement.appendChild(radioInput);
        optionElement.appendChild(label);
        caseElement.appendChild(optionElement);
    });

    return caseElement;
}



function startNewGame() {
    // Clear previous game data
    caseForm.innerHTML = '';  // Clear the form
    responseContainer.innerHTML = ''; // Clear the results
    responseContainer.classList.remove('success');
    responseContainer.classList.remove('error');

    // Reset the form and enable input
    form.reset();
    form.querySelector('#language').disabled = false;
    form.querySelector('#age').disabled = false;
    form.querySelector('#difficulty').disabled = false;
    form.querySelector('#subject').disabled = false;

    form.querySelector('button[type="submit"]').disabled = false;

    // Reset the visibility
    loadingSpinner.classList.add('hidden');
    responseContainer.classList.add('hidden');

    location.reload();
}


async function handleGenerateCasesResponse(response) {
    if (!response.ok) {
        const errorData = await response.json();
        displayError(errorData);
        return; // Important: Stop execution if there's an error
    }

    const jsonData = await response.json();

    // Robust check for data structure
    if (!jsonData.data) {
        displayError({ error: "Server response missing 'data' field." });
        return;
    }

    //Check if data.data is an array of objects or a single object.
    const data = Array.isArray(jsonData.data) ? jsonData.data : [jsonData.data]; // wrap in array if needed


    if (data && data.length > 0) {
        displayCases( {data: data}); // Pass the corrected data to displayCases
        responseContainer.classList.add('success');
    } else {
        displayError({ error: "No cases received from the server." });
    }
}


function displayError(errorData) {
    responseContainer.classList.add('error');
    responseContainer.innerHTML = `
        <p><strong>Error:</strong> ${errorData.error}</p>
        <button id="tryAgainButton">Try Again</button>
    `;

    const tryAgainButton = document.getElementById('tryAgainButton');
    tryAgainButton.addEventListener('click', () => {
        responseContainer.innerHTML = '';
        responseContainer.classList.remove('error');
        form.querySelector('#language').disabled = false;
        form.querySelector('#age').disabled = false;
        form.querySelector('#subject').disabled = false;
        form.querySelector('#difficulty').disabled = false;

        form.querySelector('button[type="submit"]').disabled = false;
        responseContainer.classList.add('hidden');
        form.submit();
    });
}
