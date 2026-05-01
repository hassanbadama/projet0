// Variables globales
let criteria = [];
let alternatives = [];
let scores = {};
let pairwiseMatrix = {};

// Ajouter un critère
function addCriterion() {
    const input = document.getElementById('criterionInput');
    const name = input.value.trim();
    if (name && !criteria.includes(name)) {
        criteria.push(name);
        updateCriteriaList();
        input.value = '';
    }
}

function updateCriteriaList() {
    const container = document.getElementById('criteriaList');
    container.innerHTML = criteria.map((c, i) => `
        <div class="badge bg-secondary p-2">
            📌 ${c}
            <button class="btn-close btn-close-white ms-2" style="font-size: 10px;" onclick="removeCriterion(${i})"></button>
        </div>
    `).join('');
}

function removeCriterion(index) {
    criteria.splice(index, 1);
    updateCriteriaList();
}

// Ajouter une alternative
function addAlternative() {
    const input = document.getElementById('alternativeInput');
    const name = input.value.trim();
    if (name && !alternatives.includes(name)) {
        alternatives.push(name);
        updateAlternativesList();
        input.value = '';
    }
}

function updateAlternativesList() {
    const container = document.getElementById('alternativesList');
    container.innerHTML = alternatives.map((a, i) => `
        <div class="badge bg-info p-2">
            🎯 ${a}
            <button class="btn-close btn-close-white ms-2" style="font-size: 10px;" onclick="removeAlternative(${i})"></button>
        </div>
    `).join('');
}

function removeAlternative(index) {
    alternatives.splice(index, 1);
    updateAlternativesList();
}

// Navigation
function nextStep(currentStep) {
    if (currentStep === 1 && criteria.length < 2) {
        alert('Veuillez ajouter au moins 2 critères');
        return;
    }
    if (currentStep === 2 && alternatives.length < 2) {
        alert('Veuillez ajouter au moins 2 alternatives');
        return;
    }
    if (currentStep === 2) {
        saveCriteriaAndAlternatives();
        initializeScores();
    }
    if (currentStep === 3) {
        saveScores();
        initializePairwiseMatrix();
    }
    
    $(`#step${currentStep}`).removeClass('active');
    $(`#step${currentStep + 1}`).addClass('active');
    updateStepIndicator(currentStep + 1);
}

function prevStep(currentStep) {
    $(`#step${currentStep}`).removeClass('active');
    $(`#step${currentStep - 1}`).addClass('active');
    updateStepIndicator(currentStep - 1);
}

function updateStepIndicator(step) {
    $('.step-circle').removeClass('active completed');
    for (let i = 1; i <= 5; i++) {
        if (i < step) {
            $(`.step-circle[data-step="${i}"]`).addClass('completed');
        } else if (i === step) {
            $(`.step-circle[data-step="${i}"]`).addClass('active');
        }
    }
}

function saveCriteriaAndAlternatives() {
    $.ajax({
        url: '/api/criteria',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ criteria: criteria }),
        async: false
    });
    
    $.ajax({
        url: '/api/alternatives',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ alternatives: alternatives }),
        async: false
    });
}

function initializeScores() {
    const container = document.getElementById('scoresContainer');
    let html = '';
    
    criteria.forEach(criterion => {
        html += `
            <div class="card mb-3">
                <div class="card-header bg-light">
                    <strong>📋 ${criterion}</strong>
                </div>
                <div class="card-body">
                    <table class="table table-bordered">
                        <thead>
                            <tr><th>Alternative</th><th>Score (0-10)</th></tr>
                        </thead>
                        <tbody>
        `;
        
        alternatives.forEach(alt => {
            html += `
                <tr>
                    <td><strong>${alt}</strong></td>
                    <td>
                        <input type="number" step="0.1" min="0" max="10" 
                               class="form-control score-input" 
                               data-criterion="${criterion}" 
                               data-alternative="${alt}"
                               value="0">
                    </td>
                </tr>
            `;
        });
        
        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

function saveScores() {
    scores = {};
    criteria.forEach(criterion => {
        scores[criterion] = {};
        alternatives.forEach(alt => {
            const input = $(`.score-input[data-criterion="${criterion}"][data-alternative="${alt}"]`);
            scores[criterion][alt] = parseFloat(input.val()) || 0;
        });
    });
    
    $.ajax({
        url: '/api/scores',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ scores: scores }),
        async: false
    });
}

function initializePairwiseMatrix() {
    const container = document.getElementById('pairwiseContainer');
    let html = '<div class="table-responsive"><table class="table table-bordered text-center">';
    html += '<thead><tr><th></th>';
    criteria.forEach(c => html += `<th>${c}</th>`);
    html += '</tr></thead><tbody>';
    
    for (let i = 0; i < criteria.length; i++) {
        html += `<tr><th>${criteria[i]}</th>`;
        for (let j = 0; j < criteria.length; j++) {
            if (i === j) {
                html += `<td>1</td>`;
            } else if (i < j) {
                html += `
                    <td>
                        <select class="form-select form-select-sm pairwise-select" 
                                data-c1="${criteria[i]}" 
                                data-c2="${criteria[j]}">
                            ${generateSaatyOptions()}
                        </select>
                    </td>
                `;
            } else {
                html += `<td id="cell_${criteria[i]}_${criteria[j]}">1</td>`;
            }
        }
        html += '</tr>';
    }
    html += '</tbody></table></div>';
    
    container.innerHTML = html;
    
    // Initialiser pairwiseMatrix
    pairwiseMatrix = {};
    criteria.forEach(c1 => {
        pairwiseMatrix[c1] = {};
        criteria.forEach(c2 => {
            pairwiseMatrix[c1][c2] = c1 === c2 ? 1 : 1;
        });
    });
    
    // Écouter les changements
    $('.pairwise-select').change(function() {
        const c1 = $(this).data('c1');
        const c2 = $(this).data('c2');
        const value = parseFloat($(this).val());
        
        pairwiseMatrix[c1][c2] = value;
        pairwiseMatrix[c2][c1] = 1 / value;
        
        $(`#cell_${c2}_${c1}`).text((1 / value).toFixed(2));
        
        // Sauvegarder
        $.ajax({
            url: '/api/pairwise',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ criterion1: c1, criterion2: c2, value: value })
        });
    });
}

function generateSaatyOptions() {
    let options = '<option value="1">1 - Égal</option>';
    for (let i = 2; i <= 9; i++) {
        options += `<option value="${i}">${i} - Plus important</option>`;
    }
    for (let i = 2; i <= 9; i++) {
        options += `<option value="${1/i}">1/${i} - Moins important</option>`;
    }
    return options;
}

function calculateAHP() {
    saveScores();
    
    const requestData = {
        criteria: criteria,
        alternatives: alternatives,
        scores: scores,
        pairwiseMatrix: pairwiseMatrix
    };
    
    $.ajax({
        url: '/api/calculate',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(response) {
            displayResults(response);
            $('#step4').removeClass('active');
            $('#step5').addClass('active');
            updateStepIndicator(5);
        },
        error: function(error) {
            alert('Erreur lors du calcul: ' + error.responseText);
        }
    });
}

function displayResults(response) {
    const container = document.getElementById('resultsContainer');
    
    if (!response.success) {
        container.innerHTML = `<div class="alert alert-danger">${response.message}</div>`;
        return;
    }
    
    const result = response.result;
    let html = '';
    
    // Meilleure alternative
    if (result.consistent) {
        html += `
            <div class="alert alert-success text-center">
                <h4><i class="fas fa-trophy"></i> Meilleure alternative recommandée</h4>
                <h2 class="display-4">${result.bestAlternative}</h2>
                <p>Avec un score de ${(result.bestScore * 100).toFixed(2)}%</p>
            </div>
        `;
    } else {
        html += `
            <div class="alert alert-danger">
                <h5><i class="fas fa-exclamation-triangle"></i> Matrice inconsistante !</h5>
                <p>Ratio de Consistance (CR = ${result.consistencyRatio.toFixed(4)}) ≥ 0.1</p>
                <p>λmax = ${result.lambdaMax.toFixed(4)}, IC = ${result.consistencyIndex.toFixed(4)}</p>
            </div>
        `;
    }
    
    // Vérification de consistance
    const consistencyClass = result.consistent ? 'consistency-good' : 'consistency-bad';
    html += `
        <div class="consistency-badge ${consistencyClass} mb-3">
            <strong>Vérification de consistance:</strong><br>
            CR = ${result.consistencyRatio.toFixed(4)} ${result.consistent ? '< 0.1 ✅' : '≥ 0.1 ❌'}<br>
            ${!result.consistent ? '<span class="text-danger">La matrice est inconsistante, les résultats peuvent ne pas être fiables.</span>' : ''}
        </div>
    `;
    
    // Raisons d'inconsistance
    if (!result.consistent && result.inconsistencyReasons && result.inconsistencyReasons.length > 0) {
        html += `
            <div class="card mb-3 border-warning">
                <div class="card-header bg-warning text-dark">
                    <strong>⚠️ Raisons possibles de l'inconsistance</strong>
                </div>
                <div class="card-body">
                    <ul>
                        ${result.inconsistencyReasons.map(reason => `<li>${reason}</li>`).join('')}
                    </ul>
                </div>
            </div>
        `;
    }
    
    // Poids des critères
    html += `
        <div class="card mb-3">
            <div class="card-header bg-primary text-white">
                <strong>📊 Poids des critères</strong>
            </div>
            <div class="card-body">
                <ul class="list-group">
                    ${Object.entries(result.criteriaWeights).map(([c, w]) => 
                        `<li class="list-group-item d-flex justify-content-between align-items-center">
                            ${c}
                            <span class="badge bg-primary rounded-pill">${(w * 100).toFixed(2)}%</span>
                        </li>`
                    ).join('')}
                </ul>
            </div>
        </div>
    `;
    
    // Scores finaux
    html += `
        <div class="card">
            <div class="card-header bg-success text-white">
                <strong>🏆 Scores finaux des alternatives</strong>
            </div>
            <div class="card-body">
                <ul class="list-group">
                    ${Object.entries(result.alternativeScores)
                        .sort((a,b) => b[1] - a[1])
                        .map(([alt, score]) => 
                            `<li class="list-group-item d-flex justify-content-between align-items-center">
                                ${alt}
                                <span class="badge bg-success rounded-pill">${(score * 100).toFixed(2)}%</span>
                            </li>`
                        ).join('')}
                </ul>
            </div>
        </div>
    `;
    
    container.innerHTML = html;
}

function resetApp() {
    window.location.href = '/reset';
}