/* ==========================================================================
   MINDFLOW PORTAL: SPA FRONTEND CLIENT CONTROL
   ========================================================================== */

// Global Game Client State
let clientState = {
    playerName: null,
    selectedCategory: null,
    selectedMode: null,
    activeTimer: null,
    remainingSeconds: 120,
    currentScore: 0,
    currentIndex: 0,
    totalQuestions: 0
};

// Document Elements
const screens = {
    welcome: document.getElementById('welcome-screen'),
    lobby: document.getElementById('lobby-screen'),
    category: document.getElementById('category-screen'),
    mode: document.getElementById('mode-screen'),
    game: document.getElementById('game-screen'),
    result: document.getElementById('result-screen'),
    leaderboard: document.getElementById('leaderboard-screen')
};

// ==========================================================================
// BOOTSTRAP & SESSION AUTO-RECOVERY
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    setupEventListeners();
    checkActiveSession();
});

// Check if player has an existing session on load
function checkActiveSession() {
    fetch('api/session')
        .then(res => res.json())
        .then(data => {
            if (data.authenticated) {
                setPlayerIdentity(data.playerName);
                if (data.hasActiveGame) {
                    // Recover ongoing game
                    clientState.selectedCategory = data.category;
                    clientState.selectedMode = data.gameMode;
                    clientState.currentIndex = data.currentIndex;
                    clientState.totalQuestions = data.totalQuestions;
                    
                    // Show game board and fetch current question status
                    showScreen('game-screen');
                    restoreActiveGame();
                } else {
                    showScreen('lobby-screen');
                }
            } else {
                showScreen('welcome-screen');
            }
        })
        .catch(err => {
            console.error("Session check failed, falling back to login screen", err);
            showScreen('welcome-screen');
        });
}

// Restore state if page refreshed during game
function restoreActiveGame() {
    // Re-trigger start logic to fetch state (StartGameServlet is idempotent for active game session)
    const params = new URLSearchParams();
    params.append('category', clientState.selectedCategory);
    params.append('mode', clientState.selectedMode);

    fetch('api/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            setupGameBoard(data);
        } else {
            showScreen('lobby-screen');
        }
    })
    .catch(err => {
        showToast('❌ Failed to restore active quiz', 'danger');
        showScreen('lobby-screen');
    });
}

// ==========================================================================
// SPA ROUTER: SCREEN SWAPPING
// ==========================================================================
function showScreen(screenId) {
    // Hide all screens
    Object.values(screens).forEach(screen => {
        if (screen) {
            screen.classList.remove('active');
        }
    });

    // Show target screen
    const target = document.getElementById(screenId);
    if (target) {
        target.classList.add('active');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

// ==========================================================================
// EVENT LISTENERS & SETUP
// ==========================================================================
function setupEventListeners() {
    // Theme Toggle
    const themeBtn = document.getElementById('theme-toggle-btn');
    if (themeBtn) {
        themeBtn.addEventListener('click', toggleTheme);
    }

    // Welcome Name Form Submission
    const nameForm = document.getElementById('name-form');
    if (nameForm) {
        nameForm.addEventListener('submit', handleNameSubmission);
    }

    // Lobby Navigation
    document.getElementById('lobby-play-btn').addEventListener('click', () => {
        showScreen('category-screen');
    });
    
    document.getElementById('lobby-leaderboard-btn').addEventListener('click', fetchAndShowLeaderboard);
    
    document.getElementById('lobby-logout-btn').addEventListener('click', handleLogout);

    // Category Grid Taps
    const catCards = document.querySelectorAll('.category-card');
    catCards.forEach(card => {
        card.addEventListener('click', () => {
            const category = card.getAttribute('data-category');
            selectCategory(category);
        });
    });

    // Mode Selector Taps
    document.getElementById('mode-classic-btn').addEventListener('click', () => {
        startQuiz('Classic');
    });

    document.getElementById('mode-rapid-btn').addEventListener('click', () => {
        startQuiz('Rapid Fire');
    });

    // In-game: Skip Actions
    document.getElementById('game-skip-btn').addEventListener('click', handleSkipRequest);
    document.getElementById('skip-cancel-btn').addEventListener('click', hideSkipModal);
    document.getElementById('skip-confirm-btn').addEventListener('click', executeSkip);

    // In-game: Quit Actions
    document.getElementById('game-quit-btn').addEventListener('click', showQuitModal);
    document.getElementById('quit-cancel-btn').addEventListener('click', hideQuitModal);
    document.getElementById('quit-confirm-btn').addEventListener('click', executeQuit);

    // In-game: Rapid Answer text form
    const rapidForm = document.getElementById('rapid-answer-form');
    if (rapidForm) {
        rapidForm.addEventListener('submit', handleRapidTextSubmission);
    }
}

// ==========================================================================
// CORE HANDLERS
// ==========================================================================

// Player Name Submission
function handleNameSubmission() {
    const input = document.getElementById('username-input');
    const errorDiv = document.getElementById('name-error');
    const name = input.value.trim();

    if (!name) {
        showNameError("Name cannot be blank!");
        return;
    }

    const params = new URLSearchParams();
    params.append('name', name);

    fetch('api/name', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            setPlayerIdentity(data.name);
            showScreen('lobby-screen');
            showToast(`👋 Welcome, ${data.name}!`);
        } else {
            showNameError(data.error || "Failed to set player name.");
        }
    })
    .catch(err => {
        showNameError("Connection error. Is Tomcat running?");
    });
}

function showNameError(msg) {
    const errorDiv = document.getElementById('name-error');
    errorDiv.textContent = msg;
    errorDiv.classList.remove('hidden');
}

// Clears name storage on client
function handleLogout() {
    // Overwrite with empty string to force servlet session clean up (client handles redirect)
    const params = new URLSearchParams();
    params.append('name', '');
    
    fetch('api/name', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .finally(() => {
        clientState.playerName = null;
        document.getElementById('player-badge').classList.add('hidden');
        document.getElementById('username-input').value = '';
        document.getElementById('name-error').classList.add('hidden');
        showScreen('welcome-screen');
        showToast('Logged out successfully.');
    });
}

// Set badge and profile identities
function setPlayerIdentity(name) {
    clientState.playerName = name;
    document.getElementById('badge-name').textContent = name;
    document.getElementById('lobby-greeting-name').textContent = name;
    document.getElementById('player-badge').classList.remove('hidden');
}

// Select a quiz category
function selectCategory(category) {
    clientState.selectedCategory = category;
    document.getElementById('selected-category-label').textContent = category;
    showScreen('mode-screen');
}

// ==========================================================================
// QUIZ ENGINE OPERATIONS
// ==========================================================================

// Start Game API Action
function startQuiz(mode) {
    clientState.selectedMode = mode;
    
    const params = new URLSearchParams();
    params.append('category', clientState.selectedCategory);
    params.append('mode', mode);

    fetch('api/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            setupGameBoard(data);
            showScreen('game-screen');
            showToast(`🚀 Starting ${mode}!`);
        } else {
            showToast(`❌ Error: ${data.error}`, 'danger');
        }
    })
    .catch(err => {
        showToast('❌ Failed to boot game. Connection error.', 'danger');
    });
}

// Configures and mounts game board data
function setupGameBoard(data) {
    clientState.currentIndex = data.currentIndex;
    clientState.totalQuestions = data.totalQuestions;
    clientState.currentScore = 0;
    
    // Status Bar Setups
    document.getElementById('game-mode-badge').textContent = data.mode;
    document.getElementById('game-progress-text').textContent = `${data.currentIndex + 1} / ${data.totalQuestions}`;
    document.getElementById('game-score-text').textContent = '0';
    document.getElementById('game-category-stamp').textContent = data.category;

    // Mode Specific Layout switches
    const optionsContainer = document.getElementById('game-options-container');
    const textInputContainer = document.getElementById('game-text-input-container');
    const timerContainer = document.getElementById('game-timer-container');
    const timerTrack = document.getElementById('game-timer-track');

    if ("Rapid Fire".equalsIgnoreCase(data.mode)) {
        optionsContainer.classList.add('hidden');
        textInputContainer.classList.remove('hidden');
        timerContainer.classList.remove('hidden');
        timerTrack.classList.remove('hidden');
        
        // Start 120s countdown timer
        startRapidTimer(120);
        
        // Focus text box
        setTimeout(() => {
            document.getElementById('rapid-answer-input').focus();
        }, 100);
    } else {
        optionsContainer.classList.remove('hidden');
        textInputContainer.classList.add('hidden');
        timerContainer.classList.add('hidden');
        timerTrack.classList.add('hidden');
        
        if (clientState.activeTimer) {
            clearInterval(clientState.activeTimer);
        }
    }

    renderQuestion(data.question);
}

// Renders individual question models
function renderQuestion(q) {
    document.getElementById('game-question-text').textContent = q.question;
    
    if ("Rapid Fire".equalsIgnoreCase(clientState.selectedMode)) {
        // Clear text field
        const txtInput = document.getElementById('rapid-answer-input');
        txtInput.value = '';
        txtInput.focus();
    } else {
        // Build MCQ Options
        const container = document.getElementById('game-options-container');
        container.innerHTML = '';
        
        const letters = ['A', 'B', 'C'];
        q.options.forEach((opt, idx) => {
            const letter = letters[idx] || 'A';
            const btn = document.createElement('button');
            btn.className = 'option-btn';
            btn.setAttribute('data-option', opt);
            btn.innerHTML = `
                <span class="option-letter">${letter}</span>
                <span class="option-text">${escapeHtml(opt)}</span>
            `;
            btn.addEventListener('click', () => handleOptionSelection(btn, opt));
            container.appendChild(btn);
        });
    }
}

// ==========================================================================
// MCQ OPTIONS SELECTION (Classic Mode)
// ==========================================================================
function handleOptionSelection(clickedBtn, selectedValue) {
    // Disable other option buttons to prevent multiple clicks during evaluation
    const buttons = document.querySelectorAll('.option-btn');
    buttons.forEach(btn => btn.style.pointerEvents = 'none');
    
    // Call submitAnswer passing the clicked button for visual feedback coloring
    submitAnswer(selectedValue, false, clickedBtn);
}

// ==========================================================================
// FREE TEXT FORM SUBMISSION (Rapid Fire Mode)
// ==========================================================================
function handleRapidTextSubmission() {
    const input = document.getElementById('rapid-answer-input');
    const val = input.value.trim();
    
    if (!val) {
        showToast('⚠️ Please type an answer or skip!', 'danger');
        return;
    }
    
    submitAnswer(val, false);
}

// ==========================================================================
// ANSWER SUBMISSION API ENGINE
// ==========================================================================
function submitAnswer(answer, isSkip, clickedBtn) {
    const params = new URLSearchParams();
    params.append('answer', answer);
    if (isSkip) {
        params.append('skip', 'true');
    }

    fetch('api/answer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.timeUp) {
            // Timer breached on server
            handleGameFinished(data);
            showToast('⏰ Time is up! Scoring halted.', 'danger');
            return;
        }

        if (data.success) {
            // 1. Update running score details on HUD
            clientState.currentScore = data.score;
            document.getElementById('game-score-text').textContent = data.score;

            // 2. Evaluate feedback based on game mode
            if ("Rapid Fire".equalsIgnoreCase(clientState.selectedMode)) {
                // Check for dynamic time adjustment from streaks (+1s / -1s)
                if (data.timeAdjustment && data.timeAdjustment !== 0) {
                    clientState.remainingSeconds += data.timeAdjustment;
                    clientState.remainingSeconds = Math.max(0, clientState.remainingSeconds);
                    document.getElementById('game-timer-text').textContent = `${clientState.remainingSeconds}s`;
                    
                    if (data.timeAdjustment > 0) {
                        showToast(`🔥 Streak! +1s Bonus!`, 'success');
                    } else {
                        showToast(`💀 Streak Penalty! -1s!`, 'danger');
                    }
                } else {
                    // Standard floating evaluations (toast-feedback)
                    if (isSkip) {
                        showToast(`⚠️ Skipped! -1 point. (Ans: ${data.correctAnswer})`, 'danger');
                    } else if ("correct".equalsIgnoreCase(data.feedback)) {
                        showToast('🎉 Correct! +4 points');
                    } else {
                        showToast(`❌ Wrong! -1 point. (Ans: ${data.correctAnswer})`, 'danger');
                    }
                }

                if (data.finished) {
                    executeFinishGame();
                } else {
                    clientState.currentIndex = data.currentIndex;
                    document.getElementById('game-progress-text').textContent = `${data.currentIndex + 1} / ${clientState.totalQuestions}`;
                    renderQuestion(data.question);
                }
            } else {
                // Classic Mode MCQ Feedback: Color Option Buttons
                if (clickedBtn) {
                    if ("correct".equalsIgnoreCase(data.feedback)) {
                        clickedBtn.classList.add('eval-correct');
                        showToast('🎉 Correct! +4 points');
                    } else if ("wrong".equalsIgnoreCase(data.feedback)) {
                        clickedBtn.classList.add('eval-wrong');
                        showToast(`❌ Wrong! -1 point. (Ans: ${data.correctAnswer})`, 'danger');
                        
                        // Highlight the actual correct option button in green
                        const buttons = document.querySelectorAll('.option-btn');
                        buttons.forEach(btn => {
                            const btnVal = btn.getAttribute('data-option');
                            if (btnVal === data.correctAnswer) {
                                btn.classList.add('eval-correct');
                            }
                        });
                    } else if (isSkip) {
                        showToast('⚠️ Skipped! No penalty.');
                    }
                }

                // Introduce a 1-second delay so the player can see the correct/wrong color highlights
                setTimeout(() => {
                    if (data.finished) {
                        executeFinishGame();
                    } else {
                        clientState.currentIndex = data.currentIndex;
                        document.getElementById('game-progress-text').textContent = `${data.currentIndex + 1} / ${clientState.totalQuestions}`;
                        renderQuestion(data.question);
                    }
                }, 1000);
            }
        } else {
            showToast(`❌ Submission Error: ${data.error}`, 'danger');
        }
    })
    .catch(err => {
        showToast('❌ Submission failed. Check server connection.', 'danger');
    });
}

// ==========================================================================
// SKIP OPERATIONS
// ==========================================================================
function handleSkipRequest() {
    if ("Rapid Fire".equalsIgnoreCase(clientState.selectedMode)) {
        // Rapid fire requires a warning deduction popup
        document.getElementById('skip-warning-modal').classList.remove('hidden');
    } else {
        // Classic has zero skip penalty, proceed immediately
        executeSkip();
    }
}

function hideSkipModal() {
    document.getElementById('skip-warning-modal').classList.add('hidden');
}

function executeSkip() {
    hideSkipModal();
    submitAnswer('', true);
}

// ==========================================================================
// QUIT QUIZ OPERATIONS
// ==========================================================================
function showQuitModal() {
    document.getElementById('quit-confirm-modal').classList.remove('hidden');
}

function hideQuitModal() {
    document.getElementById('quit-confirm-modal').classList.add('hidden');
}

function executeQuit() {
    hideQuitModal();
    executeFinishGame();
}

// ==========================================================================
// FINISH & PERSIST GAME SCORES
// ==========================================================================
function executeFinishGame() {
    if (clientState.activeTimer) {
        clearInterval(clientState.activeTimer);
    }

    fetch('api/finish', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                handleGameFinished(data);
            } else {
                showToast(`❌ Incomplete save: ${data.error}`, 'danger');
                showScreen('lobby-screen');
            }
        })
        .catch(err => {
            showToast('❌ Network error during submission.', 'danger');
            showScreen('lobby-screen');
        });
}

function handleGameFinished(data) {
    if (clientState.activeTimer) {
        clearInterval(clientState.activeTimer);
    }
    
    document.getElementById('result-player').textContent = data.playerName;
    document.getElementById('result-score').textContent = data.score;
    document.getElementById('result-mode').textContent = data.mode;
    document.getElementById('result-category').textContent = data.category;
    
    showScreen('result-screen');
    
    // Confetti celebration if score is positive!
    if (data.score > 0) {
        triggerConfetti();
    }
}

// ==========================================================================
// LEADERBOARD RENDER OPERATIONS
// ==========================================================================
function fetchAndShowLeaderboard() {
    showScreen('leaderboard-screen');
    const tbody = document.getElementById('leaderboard-data');
    tbody.innerHTML = `<tr><td colspan="6" class="table-loading">Loading top scores...</td></tr>`;

    fetch('api/leaderboard?limit=10')
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                renderLeaderboardTable(data.scores);
            } else {
                tbody.innerHTML = `<tr><td colspan="6" class="table-loading danger">Error loading leaderboard</td></tr>`;
            }
        })
        .catch(err => {
            tbody.innerHTML = `<tr><td colspan="6" class="table-loading danger">Connection error loading top scores</td></tr>`;
        });
}

function renderLeaderboardTable(scores) {
    const tbody = document.getElementById('leaderboard-data');
    tbody.innerHTML = '';

    if (!scores || scores.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="table-loading">No scores recorded yet. Be the first!</td></tr>`;
        return;
    }

    scores.forEach((us, idx) => {
        const rank = idx + 1;
        let rankHtml = `<span class="rank-number">${rank}</span>`;
        
        // Crown decorations for top 3
        if (rank === 1) {
            rankHtml = `<span class="rank-badge rank-1" title="Gold Crown">👑</span>`;
        } else if (rank === 2) {
            rankHtml = `<span class="rank-badge rank-2" title="Silver Medal">🥈</span>`;
        } else if (rank === 3) {
            rankHtml = `<span class="rank-badge rank-3" title="Bronze Medal">🥉</span>`;
        }

        // Format timestamp to nice IST format
        const dateStr = formatTimestamp(us.timestamp);

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${rankHtml}</td>
            <td><strong>${escapeHtml(us.playerName)}</strong></td>
            <td><span class="highlight">${us.score}</span></td>
            <td><span class="badge" style="background:${us.mode === 'Classic' ? 'var(--color-primary-light)' : 'var(--color-accent-light)'}; color:${us.mode === 'Classic' ? 'var(--color-primary)' : 'var(--color-accent)'}">${us.mode}</span></td>
            <td>${escapeHtml(us.category)}</td>
            <td>${dateStr}</td>
        `;
        tbody.appendChild(tr);
    });
}

// ==========================================================================
// RAPID COUNTDOWN TIMER MANAGEMENT
// ==========================================================================
function startRapidTimer(seconds) {
    if (clientState.activeTimer) {
        clearInterval(clientState.activeTimer);
    }

    clientState.remainingSeconds = seconds;
    const timerText = document.getElementById('game-timer-text');
    const timerBar = document.getElementById('game-timer-bar');

    timerText.textContent = `${clientState.remainingSeconds}s`;
    timerText.classList.remove('timer-danger');
    timerBar.style.width = '100%';
    timerBar.style.backgroundColor = 'var(--color-accent)';

    clientState.activeTimer = setInterval(() => {
        clientState.remainingSeconds--;
        
        // Update Displays
        timerText.textContent = `${clientState.remainingSeconds}s`;
        const percentage = (clientState.remainingSeconds / seconds) * 100;
        timerBar.style.width = `${percentage}%`;

        // Warning alerts on low time
        if (clientState.remainingSeconds <= 15) {
            timerText.classList.add('timer-danger');
            timerBar.style.backgroundColor = 'var(--color-danger)';
        }

        if (clientState.remainingSeconds <= 0) {
            clearInterval(clientState.activeTimer);
            showToast('⏰ Time limit reached!', 'danger');
            executeFinishGame();
        }
    }, 1000);
}

// ==========================================================================
// SYSTEM THEME MANAGEMENT (Light / Dark)
// ==========================================================================
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.body.classList.contains('theme-light') ? 'light' : 'dark';
    const nextTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
}

function setTheme(theme) {
    const btnIcon = document.querySelector('.theme-icon');
    
    if (theme === 'dark') {
        document.body.className = 'theme-dark';
        if (btnIcon) btnIcon.textContent = '☀️';
    } else {
        document.body.className = 'theme-light';
        if (btnIcon) btnIcon.textContent = '🌙';
    }
    localStorage.setItem('theme', theme);
}

// ==========================================================================
// TOAST MICRO-NOTIFICATIONS
// ==========================================================================
function showToast(msg, type = 'success') {
    const toast = document.getElementById('feedback-toast');
    const icon = document.getElementById('toast-icon');
    const text = document.getElementById('toast-msg');

    if (!toast) return;

    if (type === 'danger') {
        icon.textContent = '❌';
        toast.style.borderColor = 'var(--color-danger)';
        toast.style.background = 'var(--card-bg)';
    } else {
        icon.textContent = '✅';
        toast.style.borderColor = 'var(--color-primary)';
        toast.style.background = 'var(--card-bg)';
    }

    text.textContent = msg;
    toast.classList.remove('hidden');

    // Auto dismiss after 2 seconds
    if (window.toastTimeout) {
        clearTimeout(window.toastTimeout);
    }
    window.toastTimeout = setTimeout(() => {
        toast.classList.add('hidden');
    }, 2200);
}

// ==========================================================================
// PREMIUM CANVAS CONFETTI SYSTEM
// ==========================================================================
function triggerConfetti() {
    const canvas = document.getElementById('confetti-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    canvas.width = canvas.parentElement.offsetWidth;
    canvas.height = canvas.parentElement.offsetHeight;

    const colors = ['#6FBF9A', '#FFB7B2', '#FFD97D', '#8AB4FF', '#B39DDB'];
    const particles = [];

    // Create 80 particles
    for (let i = 0; i < 80; i++) {
        particles.push({
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height - canvas.height,
            r: Math.random() * 6 + 4,
            d: Math.random() * canvas.height,
            color: colors[Math.floor(Math.random() * colors.length)],
            tilt: Math.random() * 10 - 5,
            tiltAngleIncremental: Math.random() * 0.07 + 0.02,
            tiltAngle: 0
        });
    }

    let animationId;
    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        let remaining = false;
        particles.forEach(p => {
            p.tiltAngle += p.tiltAngleIncremental;
            p.y += (Math.cos(p.d) + 3 + p.r / 2) / 2;
            p.x += Math.sin(p.tiltAngle);
            p.tilt = Math.sin(p.tiltAngle - p.r / 2) * 5;

            if (p.y <= canvas.height) {
                remaining = true;
            }

            ctx.beginPath();
            ctx.lineWidth = p.r;
            ctx.strokeStyle = p.color;
            ctx.moveTo(p.x + p.tilt + p.r / 2, p.y);
            ctx.lineTo(p.x + p.tilt, p.y + p.tilt + p.r / 2);
            ctx.stroke();
        });

        if (remaining) {
            animationId = requestAnimationFrame(draw);
        } else {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            cancelAnimationFrame(animationId);
        }
    }
    
    draw();
}

// ==========================================================================
// UTILITIES: STRING CASING & ESCAPING
// ==========================================================================
String.prototype.equalsIgnoreCase = function(str) {
    return this.toLowerCase() === (str || '').toLowerCase();
};

function escapeHtml(string) {
    return String(string).replace(/[&<>"']/g, function (s) {
        return {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[s];
    });
}

function formatTimestamp(tsVal) {
    if (!tsVal) return 'N/A';
    
    // Handle SQL format or standard integer
    const date = new Date(tsVal);
    
    // Format to nice Indian date (IST)
    const options = { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true,
        timeZone: 'Asia/Kolkata'
    };
    return date.toLocaleString('en-IN', options);
}
