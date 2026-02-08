// State Management
const state = {
    profile: {
        age: '',
        gender: '',
        weight: '',
        height: '',
        goal: 'maintain',
        dietType: 'balanced'
    },
    preferences: [],
    ingredients: [],
    recommendations: [],
    stores: [],
    recipes: [],
    editingIndex: null,
    walkingSessions: {}
};

// DOM Elements
const elements = {
    tabs: document.querySelectorAll('.tab-btn'),
    tabContents: document.querySelectorAll('.tab-content'),
    imageUpload: document.getElementById('imageUpload'),
    uploadArea: document.getElementById('uploadArea'),
    uploadIcon: document.getElementById('uploadIcon'),
    uploadText: document.getElementById('uploadText'),
    spinner: document.getElementById('spinner'),
    previewImage: document.getElementById('previewImage'),
    ingredientsCard: document.getElementById('ingredientsCard'),
    ingredientsList: document.getElementById('ingredientsList'),
    ingredientCount: document.getElementById('ingredientCount'),
    confirmAllBtn: document.getElementById('confirmAllBtn'),
    profileForm: document.getElementById('profileForm'),
    preferenceInput: document.getElementById('preferenceInput'),
    addPreferenceBtn: document.getElementById('addPreferenceBtn'),
    preferencesTags: document.getElementById('preferencesTags'),
    recommendationsList: document.getElementById('recommendationsList'),
    storesList: document.getElementById('storesList'),
    recipesList: document.getElementById('recipesList'),
    toast: document.getElementById('toast'),
    toastMessage: document.getElementById('toastMessage'),
    editModal: document.getElementById('editModal'),
    closeModalBtn: document.getElementById('closeModalBtn'),
    cancelEditBtn: document.getElementById('cancelEditBtn'),
    saveEditBtn: document.getElementById('saveEditBtn'),
    editName: document.getElementById('editName'),
    editQuantity: document.getElementById('editQuantity'),
    editUnit: document.getElementById('editUnit'),
    editFreshness: document.getElementById('editFreshness')
};

// --- Initialization ---

function init() {
    loadFromStorage();
    setupEventListeners();
    renderPreferences();
    
    // 1. Load Profile from Backend on startup
    loadProfileFromBackend().then(() => {
        loadProfileData();
    }).catch(() => {
        console.log("Using local profile data");
        loadProfileData();
    });

    if (state.ingredients.length > 0) {
        renderIngredients();
    }
}

// --- Event Listeners ---

function setupEventListeners() {
    // Tabs
    elements.tabs.forEach(tab => {
        tab.addEventListener('click', () => handleTabSwitch(tab));
    });

    // Uploads
    elements.uploadArea.addEventListener('click', () => elements.imageUpload.click());
    elements.imageUpload.addEventListener('change', handleImageUpload);

    // Forms
    elements.profileForm.addEventListener('submit', handleProfileSubmit);

    // Preferences
    elements.addPreferenceBtn.addEventListener('click', addPreference);
    elements.preferenceInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            addPreference();
        }
    });

    // Buttons
    elements.confirmAllBtn.addEventListener('click', confirmAllIngredients);
    
    // Modals
    elements.closeModalBtn.addEventListener('click', closeModal);
    elements.cancelEditBtn.addEventListener('click', closeModal);
    elements.saveEditBtn.addEventListener('click', saveEdit);
    elements.editModal.addEventListener('click', (e) => {
        if (e.target === elements.editModal) closeModal();
    });

    // Drag & Drop
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        elements.uploadArea.addEventListener(eventName, preventDefaults, false);
    });
    elements.uploadArea.addEventListener('drop', handleDrop);

    // Settings / Logout
    const settingsBtn = document.getElementById('settingsBtn');
    const settingsMenu = document.getElementById('settingsMenu');
    const logoutItem = document.getElementById('logoutItem');

    if (settingsBtn && settingsMenu) {
        settingsBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            settingsMenu.classList.toggle('hidden');
        });
        document.addEventListener('click', (e) => {
            if (!settingsMenu.classList.contains('hidden') && !settingsBtn.contains(e.target)) {
                settingsMenu.classList.add('hidden');
            }
        });
    }

    if (logoutItem) {
        logoutItem.addEventListener('click', async () => {
            const supabase = window.supabaseClient;
            if (supabase) await supabase.auth.signOut();
            window.location.href = 'auth.html';
        });
    }
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

// --- API Helpers ---

async function getAuthToken() {
    const supabase = window.supabaseClient;
    if (!supabase) return null;
    const { data: { session } } = await supabase.auth.getSession();
    return session?.access_token || null;
}

function getBackendUrl() {
    // Use the variable attached to window by index.html or fallback
    return window.BACKEND_URL || 'http://localhost:8000';
}

// --- Tab Logic ---

function handleTabSwitch(tab) {
    const tabId = tab.dataset.tab;
    
    elements.tabs.forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    
    elements.tabContents.forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(`${tabId}-tab`).classList.add('active');

    // Trigger Real API calls when switching tabs
    if (tabId === 'shopping') {
        generateRecommendations(); // Calls /nutrition/analyze-fridge
    } else if (tabId === 'recipes') {
        generateRecipes(); // Calls /recipes/recommend
    } else if (tabId === 'profile') {
        loadWalkingTrend();
    }
}

// --- Image Scanning (Real Backend Call) ---

function handleImageUpload(e) {
    const file = e.target.files[0];
    if (file && file.type.startsWith('image/')) processImage(file);
}

function handleDrop(e) {
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) processImage(file);
}

function processImage(file) {
    elements.uploadIcon.classList.add('hidden');
    elements.spinner.classList.remove('hidden');
    elements.uploadText.textContent = 'Analyzing fridge...';

    const reader = new FileReader();
    reader.onload = (e) => {
        elements.previewImage.innerHTML = `<img src="${e.target.result}" alt="Preview">`;
        elements.previewImage.classList.remove('hidden');
    };
    reader.readAsDataURL(file);

    const formData = new FormData();
    formData.append('file', file);
    scanFridgeImage(formData);
}

async function scanFridgeImage(formData) {
    try {
        const token = await getAuthToken();
        if (!token) throw new Error("Please log in to scan.");

        const res = await fetch(`${getBackendUrl()}/fridge/scan`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        if (!res.ok) throw new Error("Scan failed. Check server logs.");
        const data = await res.json();
        
        state.ingredients = data.items || [];
        saveToStorage();
        renderIngredients();
        showToast('Ingredients detected!');
    } catch (e) {
        showToast(e.message);
        console.error(e);
    } finally {
        elements.spinner.classList.add('hidden');
        elements.uploadIcon.classList.remove('hidden');
        elements.uploadText.textContent = 'Upload a photo of your fridge';
    }
}

// --- Nutrition Analysis & Stores (Real Backend Call) ---

async function generateRecommendations() {
    if (state.ingredients.length === 0) {
        elements.recommendationsList.innerHTML = '<div class="empty-state"><p>Scan fridge first!</p></div>';
        return;
    }

    elements.recommendationsList.innerHTML = '<div class="spinner"></div>';
    elements.storesList.innerHTML = '<div class="spinner"></div>';

    try {
        const token = await getAuthToken();
        const res = await fetch(`${getBackendUrl()}/nutrition/analyze-fridge`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                ingredients: state.ingredients,
                user_location: { lat: 40.7128, lng: -74.0060 } // Mock location for now
            })
        });

        if (!res.ok) throw new Error("Analysis failed");
        const data = await res.json();
        
        renderRecommendations(data.suggestions);
        
        // After getting shopping list, fetch store recommendations
        if (data.shopping_list_generated && data.shopping_list_generated.length > 0) {
            fetchStores(data.shopping_list_generated);
        } else {
            elements.storesList.innerHTML = '<p>No shopping items needed right now.</p>';
        }

    } catch (e) {
        console.error(e);
        elements.recommendationsList.innerHTML = '<p>Failed to load analysis.</p>';
        showToast("Failed to analyze nutrition");
    }
}

async function fetchStores(shoppingList) {
    try {
        const res = await fetch(`${getBackendUrl()}/stores/recommend`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                lat: 40.7128, 
                lng: -74.0060, 
                items: shoppingList
            })
        });

        const data = await res.json();
        state.stores = data.stores || [];
        renderStores(state.stores);
    } catch (e) {
        console.error("Store fetch error", e);
        elements.storesList.innerHTML = '<p>Could not load stores.</p>';
    }
}

// --- Recipe Generation (Real Backend Call) ---

async function generateRecipes() {
    if (state.ingredients.length === 0) {
        elements.recipesList.innerHTML = '<div class="empty-state"><p>Scan fridge first!</p></div>';
        return;
    }

    elements.recipesList.innerHTML = '<div class="spinner"></div>';

    try {
        const token = await getAuthToken();
        const res = await fetch(`${getBackendUrl()}/recipes/recommend`, {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                ingredients: state.ingredients,
                diet_type: state.profile.dietType
            })
        });

        if (!res.ok) throw new Error("Recipe generation failed");
        const data = await res.json();
        renderRecipes(data.recipes);
    } catch (e) {
        elements.recipesList.innerHTML = '<div class="empty-state"><p>Failed to load recipes</p></div>';
        showToast(e.message);
    }
}

// --- Render Functions ---

function renderIngredients() {
    elements.ingredientsCard.classList.remove('hidden');
    elements.ingredientCount.textContent = `${state.ingredients.length} items`;
    
    elements.ingredientsList.innerHTML = state.ingredients.map((item, index) => `
        <div class="ingredient-item ${item.confirmed ? 'confirmed' : 'unconfirmed'}">
            <div class="ingredient-header">
                <div class="ingredient-info">
                    <h4>${item.name}</h4>
                    <p>${item.quantity} ${item.unit}</p>
                    <span class="freshness-badge freshness-${item.freshness}">${item.freshness}</span>
                </div>
            </div>
            <div class="ingredient-actions">
                ${!item.confirmed ? `<button class="btn-confirm" onclick="confirmIngredient(${index})">Confirm</button>` : ''}
                <button class="btn-edit" onclick="editIngredient(${index})">Edit</button>
                <button class="btn-delete" onclick="removeIngredient(${index})">Delete</button>
            </div>
        </div>
    `).join('');
}

function renderRecommendations(suggestions) {
    if (!suggestions || suggestions.length === 0) {
        elements.recommendationsList.innerHTML = '<p>Great job! Your fridge looks balanced.</p>';
        return;
    }
    
    elements.recommendationsList.innerHTML = suggestions.map(s => `
        <div class="recommendation-item">
            <div class="recommendation-info">
                <h4>${s.suggestion}</h4>
                <p class="recommendation-reason">${s.reason}</p>
                <span class="badge" style="background:#fee2e2; color:#991b1b; font-size: 0.75rem;">${s.gap.replace('_', ' ')}</span>
            </div>
        </div>
    `).join('');
}

function renderStores(stores) {
    if (!stores || stores.length === 0) {
        elements.storesList.innerHTML = '<p>No matching stores found nearby.</p>';
        return;
    }

    elements.storesList.innerHTML = stores.map((store, idx) => `
        <div class="store-item">
            <div class="store-header">
                <h4 class="store-name">${store.name}</h4>
                <span class="store-distance">${store.distance_km} km</span>
            </div>
            <p style="font-size: 0.9rem;">Est. Price: <strong style="color:var(--primary-color)">$${store.total_price}</strong></p>
            <p style="font-size: 0.85rem; color: var(--text-medium);">
                Trip steps (round trip): ${estimateSteps(store.distance_km * 2, state.profile.gender).toLocaleString()}
            </p>
            <div class="store-actions" style="margin-top:0.5rem;">
                <button id="walkBtn-${idx}" class="btn-primary walk-btn" onclick="startWalking(${idx})">Start Walking</button>
            </div>
            <div id="walkTracker-${idx}" class="walk-tracker hidden">
                <div class="walk-row">
                    <span class="label">Elapsed:</span>
                    <span id="walkElapsed-${idx}">00:00</span>
                </div>
                <div class="walk-row">
                    <span class="label">Calories burned:</span>
                    <span id="walkCalories-${idx}">0</span>
                </div>
                <div class="walk-row">
                    <span class="label">Target steps:</span>
                    <span id="walkTarget-${idx}">${estimateSteps(store.distance_km * 2, state.profile.gender).toLocaleString()}</span>
                </div>
            </div>
        </div>
    `).join('');
}

function renderRecipes(recipes) {
    if (!recipes || recipes.length === 0) {
        elements.recipesList.innerHTML = '<p>No recipes could be generated.</p>';
        return;
    }

    elements.recipesList.innerHTML = recipes.map(r => `
        <div class="recipe-card">
            <div class="recipe-content">
                <h3 class="recipe-title">${r.title}</h3>
                <div class="recipe-meta">
                    <span class="recipe-meta-item">Match: ${r.match_score}%</span>
                    <span class="recipe-meta-item">${r.macros.calories} cal</span>
                    <span class="recipe-meta-item">${r.macros.protein} protein</span>
                </div>
                <p class="recipe-ingredients"><strong>Missing:</strong> ${r.missing_ingredients.length ? r.missing_ingredients.join(', ') : 'None'}</p>
                <p style="font-size:0.85rem; color:#666; margin-top:8px; line-height: 1.4;">${r.reasoning}</p>
            </div>
        </div>
    `).join('');
}

// --- Data Management Helpers ---

window.confirmIngredient = (index) => {
    state.ingredients[index].confirmed = true;
    saveToStorage();
    renderIngredients();
};

window.removeIngredient = (index) => {
    state.ingredients.splice(index, 1);
    saveToStorage();
    renderIngredients();
};

window.editIngredient = (index) => {
    state.editingIndex = index;
    const item = state.ingredients[index];
    elements.editName.value = item.name;
    elements.editQuantity.value = item.quantity;
    elements.editModal.classList.remove('hidden');
};

function confirmAllIngredients() {
    state.ingredients.forEach(i => i.confirmed = true);
    saveToStorage();
    renderIngredients();
}

function closeModal() {
    elements.editModal.classList.add('hidden');
}

function saveEdit() {
    if (state.editingIndex === null) return;
    state.ingredients[state.editingIndex].name = elements.editName.value;
    state.ingredients[state.editingIndex].quantity = elements.editQuantity.value;
    state.ingredients[state.editingIndex].confirmed = true;
    saveToStorage();
    renderIngredients();
    closeModal();
}

// --- Profile Handling ---

function handleProfileSubmit(e) {
    e.preventDefault();
    state.profile = {
        age: document.getElementById('age').value,
        gender: document.getElementById('gender').value,
        weight: document.getElementById('weight').value,
        height: document.getElementById('height').value,
        goal: document.getElementById('goal').value,
        dietType: document.getElementById('dietType').value
    };
    
    saveToStorage();
    saveProfileToBackend(state.profile)
        .then(() => showToast('Profile Saved!'))
        .catch(err => {
            console.error(err);
            showToast('Saved locally only');
        });
}

async function saveProfileToBackend(profile) {
    const token = await getAuthToken();
    if (!token) return;
    
    const payload = {
        age: Number(profile.age),
        weight_kg: Number(profile.weight),
        height_cm: Number(profile.height),
        gender: profile.gender,
        health_goal: profile.goal,
        diet_type: profile.dietType,
        preferences: state.preferences
    };

    const res = await fetch(`${getBackendUrl()}/profile`, {
        method: 'PUT',
        headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
    });
    
    if (!res.ok) throw new Error("Profile save failed");
}

async function loadProfileFromBackend() {
    const token = await getAuthToken();
    if (!token) return;
    
    const res = await fetch(`${getBackendUrl()}/profile`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    
    if (res.ok) {
        const data = await res.json();
        // Map backend response to UI state
        state.profile.age = data.age || '';
        state.profile.weight = data.weight_kg || '';
        state.profile.height = data.height_cm || '';
        state.profile.gender = data.gender || '';
        state.profile.goal = data.health_goal || 'maintain';
        state.profile.dietType = data.diet_type || 'balanced';
        state.preferences = data.preferences || [];
        saveToStorage();
    }
}

function loadProfileData() {
    document.getElementById('age').value = state.profile.age;
    document.getElementById('gender').value = state.profile.gender;
    document.getElementById('weight').value = state.profile.weight;
    document.getElementById('height').value = state.profile.height;
    document.getElementById('goal').value = state.profile.goal;
    document.getElementById('dietType').value = state.profile.dietType;
}

// --- Local Storage & Utils ---

function saveToStorage() {
    localStorage.setItem('nutriState', JSON.stringify(state));
}

function loadFromStorage() {
    const saved = localStorage.getItem('nutriState');
    if (saved) {
        const parsed = JSON.parse(saved);
        state.profile = parsed.profile || state.profile;
        state.ingredients = parsed.ingredients || [];
        state.preferences = parsed.preferences || [];
    }
}

function addPreference() {
    const val = elements.preferenceInput.value.trim();
    if (val && !state.preferences.includes(val)) {
        state.preferences.push(val);
        elements.preferenceInput.value = '';
        saveToStorage();
        renderPreferences();
    }
}

function renderPreferences() {
    elements.preferencesTags.innerHTML = state.preferences
        .map((p, index) => `
        <span class="preference-tag">
            <span class="tag-text">${p}</span>
            <button class="tag-remove" aria-label="Remove ${p}" onclick="removePreference(${index})">&times;</button>
        </span>
    `)
        .join('');
}

function showToast(message) {
    if (!elements.toast || !elements.toastMessage) return;
    elements.toastMessage.textContent = message;
    elements.toast.classList.remove('hidden');
    elements.toast.classList.add('show');
    setTimeout(() => {
        elements.toast.classList.remove('show');
    }, 3000);
}

// Start
document.addEventListener('DOMContentLoaded', init);
// --- Walking Tracker ---
function estimateSteps(distance_km, gender) {
    let stride_m = 0.75;
    if (gender === 'male') stride_m = 0.78;
    else if (gender === 'female') stride_m = 0.70;
    const steps = (Number(distance_km || 0) * 1000) / stride_m;
    return Math.max(0, Math.round(steps));
}

function formatElapsed(ms) {
    const totalSec = Math.floor(ms / 1000);
    const m = String(Math.floor(totalSec / 60)).padStart(2, '0');
    const s = String(totalSec % 60).padStart(2, '0');
    return `${m}:${s}`;
}

window.startWalking = (index) => {
    // prevent duplicate sessions
    const sess = state.walkingSessions[index];
    if (sess && sess.running) return;

    const weight = Number(state.profile.weight) || 70.0;
    const met = 3.5; // normal walking MET
    const start = Date.now();

    const caloriesPerSecond = (met * weight) / 3600.0; // MET * kg * hours; per second
    const trackerEl = document.getElementById(`walkTracker-${index}`);
    const caloriesEl = document.getElementById(`walkCalories-${index}`);
    const elapsedEl = document.getElementById(`walkElapsed-${index}`);
    const btnEl = document.getElementById(`walkBtn-${index}`);

    if (trackerEl) trackerEl.classList.remove('hidden');
    if (btnEl) {
        btnEl.textContent = 'Stop Walking';
        btnEl.onclick = () => stopWalking(index);
    }

    const intervalId = setInterval(() => {
        const now = Date.now();
        const elapsedMs = now - start;
        const elapsedSec = Math.floor(elapsedMs / 1000);
        const calories = (caloriesPerSecond * elapsedSec);
        if (elapsedEl) elapsedEl.textContent = formatElapsed(elapsedMs);
        if (caloriesEl) caloriesEl.textContent = Math.max(0, Math.round(calories));
    }, 1000);

    state.walkingSessions[index] = {
        running: true,
        startTs: start,
        intervalId,
        met,
        weight
    };
};

window.stopWalking = (index) => {
    const sess = state.walkingSessions[index];
    const btnEl = document.getElementById(`walkBtn-${index}`);
    if (!sess) return;
    try {
        clearInterval(sess.intervalId);
    } catch {}
    const elapsedMs = Date.now() - sess.startTs;
    const elapsedSec = Math.floor(elapsedMs / 1000);
    const totalCalories = Math.max(0, Math.round((sess.met * sess.weight * (elapsedSec / 3600))));
    state.walkingSessions[index].running = false;

    // Reset button
    if (btnEl) {
        btnEl.textContent = 'Start Walking';
        btnEl.onclick = () => startWalking(index);
    }

    showToast(`congrats you burn ${totalCalories} calories through the trip to this store`);

    // Persist to backend
    const store = (state.stores && state.stores[index]) ? state.stores[index] : { name: 'Store', distance_km: 0 };
    const gender = state.profile.gender;
    const tripDistance = Number(store.distance_km || 0) * 2; // round trip
    const steps = estimateSteps(tripDistance, gender);
    saveWalkLog({
        start_ts: sess.startTs / 1000,
        end_ts: Date.now() / 1000,
        calories: totalCalories,
        store_name: store.name,
        distance_km: tripDistance,
        steps
    }).catch(err => console.error('walk log failed', err));
};

async function saveWalkLog(payload) {
    try {
        const token = await getAuthToken();
        if (!token) throw new Error('Not authenticated');
        const res = await fetch(`${getBackendUrl()}/activity/walk`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Save failed');
    } catch (e) {
        console.error(e);
        showToast('Could not save walking data');
    }
}

async function loadWalkingTrend() {
    try {
        const token = await getAuthToken();
        if (!token) return;
        const res = await fetch(`${getBackendUrl()}/activity/walk/trend?days=14`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!res.ok) throw new Error('Trend failed');
        const data = await res.json();
        renderWalkingTrend(data.series || []);
    } catch (e) {
        console.error(e);
        renderWalkingTrend([]);
    }
}

function renderWalkingTrend(series) {
    const chartEl = document.getElementById('walkingTrendChart');
    const emptyEl = document.getElementById('walkingTrendEmpty');
    if (!chartEl) return;

    if (!series || series.length === 0 || series.every(p => (p.calories || 0) === 0)) {
        chartEl.innerHTML = '';
        if (emptyEl) emptyEl.classList.remove('hidden');
        return;
    }
    if (emptyEl) emptyEl.classList.add('hidden');

    const width = chartEl.clientWidth || 600;
    const height = chartEl.clientHeight || 260;
    const padL = 48, padR = 16, padT = 16, padB = 36;
    const maxValRaw = Math.max(...series.map(p => p.calories));
    // Pick a nice max and tick step
    let step = 10;
    if (maxValRaw > 200) step = 100; else if (maxValRaw > 50) step = 50; else step = 10;
    const yMax = Math.max(step, Math.ceil(maxValRaw / step) * step);
    const yTicks = 5;

    const xSpan = width - padL - padR;
    const ySpan = height - padT - padB;

    const points = series.map((p, i) => {
        const x = padL + (i * (xSpan / (series.length - 1)));
        const y = padT + ((1 - (p.calories / yMax)) * ySpan);
        return `${x},${y}`;
    }).join(' ');

    // Build axes, grid and labels
    const axisColor = '#e5e7eb';
    const textColor = '#6b7280';
    const tickEls = [];
    for (let i = 0; i <= yTicks; i++) {
        const val = Math.round((yMax / yTicks) * i);
        const y = padT + (ySpan - (ySpan * i / yTicks));
        tickEls.push(`
            <line x1="${padL}" y1="${y}" x2="${width - padR}" y2="${y}" stroke="${axisColor}" stroke-width="1" opacity="0.6" />
            <text x="${padL - 8}" y="${y + 4}" text-anchor="end" font-size="11" fill="${textColor}">${val}</text>
        `);
    }

    // X-axis labels (show ~7 evenly spaced dates)
    const xLabelEvery = Math.max(1, Math.ceil(series.length / 7));
    const xLabels = series.map((p, i) => {
        if (i % xLabelEvery !== 0 && i !== series.length - 1) return '';
        const x = padL + (i * (xSpan / (series.length - 1)));
        const label = (p.date || '').slice(5); // show MM-DD
        return `<text x="${x}" y="${height - padB + 18}" text-anchor="middle" font-size="11" fill="${textColor}">${label}</text>`;
    }).join('');

    const svg = `
        <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
            <!-- Axes -->
            <line x1="${padL}" y1="${padT}" x2="${padL}" y2="${height - padB}" stroke="${axisColor}" stroke-width="1.5" />
            <line x1="${padL}" y1="${height - padB}" x2="${width - padR}" y2="${height - padB}" stroke="${axisColor}" stroke-width="1.5" />
            ${tickEls.join('')}
            ${xLabels}
            <!-- Data -->
            <polyline points="${points}" fill="none" stroke="var(--primary-color)" stroke-width="2" />
        </svg>
    `;
    chartEl.innerHTML = svg;
}

// Preference removal handler
window.removePreference = (index) => {
    if (index < 0 || index >= state.preferences.length) return;
    const removed = state.preferences.splice(index, 1)[0];
    saveToStorage();
    renderPreferences();
    if (removed) {
        showToast(`Preference '${removed}' removed`);
    }
};