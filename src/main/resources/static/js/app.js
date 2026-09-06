const state = { credentials: null, jobs: [], selectedJobId: null, savingJob: false };
const $ = (id) => document.getElementById(id);

function setVisible(element, visible) { element.hidden = !visible; }

function showToast(message, error = false) {
    const toast = $('toast');
    toast.textContent = message;
    toast.classList.toggle('error', error);
    setVisible(toast, true);
    window.setTimeout(() => setVisible(toast, false), 3500);
}

function setConnection(online) {
    const connection = $('connectionState');
    connection.textContent = online ? 'Connected' : 'Offline';
    connection.classList.toggle('online', online);
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
            ...(options.headers || {}),
            Authorization: `Basic ${state.credentials}`
        }
    });
    if (response.status === 401) {
        let message = 'Your session expired. Please sign in again.';
        try { const body = await response.json(); message = body.message || message; } catch (ignored) { /* response may be empty */ }
        logout();
        throw new Error(message);
    }
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try { const body = await response.json(); message = body.message || message; } catch (ignored) { /* response may be empty */ }
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

function login(event) {
    event.preventDefault();
    const username = $('username').value.trim();
    const password = $('password').value;
    const loginButton = $('loginButton');
    setVisible($('loginError'), false);
    loginButton.disabled = true;
    loginButton.textContent = 'Connecting...';
    state.credentials = btoa(`${username}:${password}`);
    api('/api/jobs').then(() => {
        showDashboard();
    }).catch((error) => {
        state.credentials = null;
        $('loginError').textContent = error.message || 'Unable to sign in.';
        setVisible($('loginError'), true);
        setConnection(false);
    }).finally(() => {
        loginButton.disabled = false;
        loginButton.innerHTML = 'Open control room <span aria-hidden="true">&#8594;</span>';
    });
}

function logout() {
    state.credentials = null;
    state.jobs = [];
    state.selectedJobId = null;
    $('jobsTable').innerHTML = '';
    $('navJobCount').textContent = '0';
    $('totalJobs').textContent = '0';
    $('activeJobs').textContent = '0';
    $('pausedJobs').textContent = '0';
    $('lastEvent').textContent = '--';
    setVisible($('dashboardView'), false);
    setVisible($('loginView'), true);
    setVisible($('logoutButton'), false);
    setConnection(false);
}

function showDashboard() {
    setVisible($('loginView'), false);
    setVisible($('dashboardView'), true);
    setVisible($('logoutButton'), true);
    setConnection(true);
    setDashboardView('overview');
    loadJobs();
}

function setDashboardView(view) {
    const copy = {
        overview: ['Control room', 'A clear view of what is scheduled, what ran, and what needs attention.'],
        jobs: ['Job registry', 'Create, pause, resume, run, and remove scheduled work.'],
        activity: ['Execution history', 'Inspect the selected job and its latest execution trail.']
    };
    document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.dataset.view === view));
    document.querySelectorAll('.view-panel').forEach((panel) => panel.classList.toggle('is-hidden', !panel.dataset.panel.split(' ').includes(view)));
    setVisible($('metrics'), view === 'overview');
    $('viewTitle').textContent = copy[view][0];
    $('viewDescription').textContent = copy[view][1];
}

async function loadJobs() {
    try {
        state.jobs = await api('/api/jobs');
        renderJobs();
        if (state.selectedJobId && state.jobs.some((job) => job.id === state.selectedJobId)) await selectJob(state.selectedJobId);
        setConnection(true);
    } catch (error) { showToast(error.message, true); }
}

function renderJobs() {
    const table = $('jobsTable');
    table.innerHTML = '';
    $('totalJobs').textContent = state.jobs.length;
    $('navJobCount').textContent = state.jobs.length;
    $('activeJobs').textContent = state.jobs.filter((job) => job.enabled).length;
    $('pausedJobs').textContent = state.jobs.filter((job) => !job.enabled).length;
    setVisible($('jobsEmpty'), state.jobs.length === 0);
    state.jobs.forEach((job) => {
        const row = document.createElement('tr');
        row.className = job.id === state.selectedJobId ? 'selected' : '';
        row.innerHTML = `<td><div class="job-title">${escapeHtml(job.name)}</div><div class="job-url">${escapeHtml(job.targetUrl || 'Simulated execution')}</div></td>
            <td><code>${escapeHtml(job.cronExpression)}</code></td><td><span class="state ${job.enabled ? 'active' : ''}">${job.enabled ? 'Active' : 'Paused'}</span></td>
            <td>${job.maxRetries}</td><td><div class="actions"><button class="button button-small button-secondary" data-action="select" data-id="${job.id}" type="button">History</button>
            <button class="button button-small button-secondary" data-action="run" data-id="${job.id}" type="button">Run</button>
            <button class="button button-small button-quiet" data-action="toggle" data-id="${job.id}" type="button">${job.enabled ? 'Pause' : 'Enable'}</button>
            <button class="button button-small button-quiet button-danger" data-action="delete" data-id="${job.id}" type="button">Delete</button></div></td>`;
        table.appendChild(row);
    });
}

function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character])); }

async function selectJob(id) {
    state.selectedJobId = Number(id);
    const job = state.jobs.find((item) => item.id === state.selectedJobId);
    if (!job) return;
    $('selectedJobName').textContent = job.name;
    $('jobDetail').innerHTML = `<div class="detail-empty"><strong>${job.enabled ? 'Active schedule' : 'Paused schedule'}</strong><br>${escapeHtml(job.cronExpression)}<br>${escapeHtml(job.targetUrl || 'No webhook target')}</div>`;
    const history = await api(`/api/jobs/${id}/executions`);
    $('lastEvent').textContent = history.length ? formatDate(history[0].executedAt) : '--';
    $('historyList').innerHTML = history.length ? history.slice(0, 8).map(historyItem).join('') : '<div class="detail-empty">No executions recorded.</div>';
    setVisible($('historyList'), true);
    renderJobs();
}

function historyItem(item) {
    const status = item.status.toLowerCase();
    return `<article class="history-item"><div class="history-meta"><span class="history-status ${status}">${escapeHtml(item.status)}</span><span>${formatDate(item.executedAt)}</span></div><div class="history-meta"><span>${item.attempts} attempt(s)</span><span>${item.durationMs} ms</span></div>${item.errorMessage ? `<p class="history-error">${escapeHtml(item.errorMessage)}</p>` : ''}</article>`;
}

function formatDate(value) { return value ? new Date(value).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : '--'; }

function askConfirmation(title, message, actionLabel = 'Confirm') {
    return new Promise((resolve) => {
        const dialog = $('confirmDialog');
        $('confirmTitle').textContent = title;
        $('confirmMessage').textContent = message;
        $('confirmActionButton').textContent = actionLabel;
        dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
        dialog.showModal();
    });
}

async function handleJobAction(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const id = button.dataset.id;
    try {
        if (button.dataset.action === 'select') await selectJob(id);
        if (button.dataset.action === 'run') { await api(`/api/jobs/${id}/run`, { method: 'POST' }); showToast('Job executed successfully.'); await loadJobs(); await selectJob(id); }
        if (button.dataset.action === 'toggle') { const job = state.jobs.find((item) => item.id === Number(id)); await api(`/api/jobs/${id}/${job.enabled ? 'disable' : 'enable'}`, { method: 'PATCH' }); showToast(job.enabled ? 'Job paused.' : 'Job enabled.'); await loadJobs(); }
        if (button.dataset.action === 'delete') {
            const job = state.jobs.find((item) => item.id === Number(id));
            const confirmed = await askConfirmation('Delete scheduled job?',
                `This will permanently remove ${job.name} and its schedule. Execution history will remain available for audit.`,
                'Delete job');
            if (!confirmed) return;
            button.disabled = true;
            button.textContent = 'Deleting...';
            try {
                await api(`/api/jobs/${id}`, { method: 'DELETE' });
                showToast('Job deleted.');
                state.selectedJobId = null;
                setVisible($('historyList'), false);
                await loadJobs();
            } finally {
                button.disabled = false;
                button.textContent = 'Delete';
            }
        }
    } catch (error) { showToast(error.message, true); }
}

function updateSchedulePreview() {
    const mode = $('scheduleMode').value;
    let expression;
    let summary;
    setVisible($('minutesFields'), mode === 'minutes');
    setVisible($('hourlyFields'), mode === 'hourly');
    setVisible($('dailyFields'), mode === 'daily');
    if (mode === 'minutes') {
        const interval = Math.max(1, Math.min(59, Number($('minuteInterval').value) || 1));
        expression = `0 0/${interval} * * * *`;
        summary = `Runs every ${interval} minute${interval === 1 ? '' : 's'}`;
    } else if (mode === 'hourly') {
        const minute = Math.max(0, Math.min(59, Number($('hourMinute').value) || 0));
        expression = `0 ${minute} * * * *`;
        summary = `Runs at minute ${minute} of every hour`;
    } else {
        const hour = Math.max(0, Math.min(23, Number($('dailyHour').value) || 0));
        const minute = Math.max(0, Math.min(59, Number($('dailyMinute').value) || 0));
        expression = `0 ${minute} ${hour} * * *`;
        summary = `Runs every day at ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    }
    $('cronExpression').value = expression;
    $('cronPreview').textContent = expression;
    $('scheduleSummary').textContent = summary;
}

function openNewJob() {
    $('jobForm').reset();
    $('jobId').value = '';
    $('dialogTitle').textContent = 'Create a job';
    $('jobFormError').hidden = true;
    $('saveStatus').hidden = true;
    $('saveJobButton').disabled = false;
    updateSchedulePreview();
    $('jobDialog').showModal();
}

async function saveJob(event) {
    event.preventDefault();
    if (state.savingJob) return;
    state.savingJob = true;
    updateSchedulePreview();
    const payload = { name: $('jobName').value.trim(), cronExpression: $('cronExpression').value.trim(), targetUrl: $('targetUrl').value.trim() || null, maxRetries: Number($('maxRetries').value || 0) };
    const saveButton = $('saveJobButton');
    saveButton.disabled = true;
    saveButton.querySelector('.button-label').textContent = 'Saving...';
    setVisible(saveButton.querySelector('.button-spinner'), true);
    setVisible($('saveStatus'), true);
    setVisible($('jobFormError'), false);
    try {
        await api('/api/jobs', { method: 'POST', body: JSON.stringify(payload) });
        $('jobDialog').close();
        showToast('Job saved and schedule registered.');
        await loadJobs();
    } catch (error) {
        $('jobFormError').textContent = error.message.includes('already exists')
            ? 'This job name is already in use. Choose a unique name.' : error.message;
        setVisible($('jobFormError'), true);
    } finally {
        state.savingJob = false;
        saveButton.disabled = false;
        saveButton.querySelector('.button-label').textContent = 'Save job';
        setVisible(saveButton.querySelector('.button-spinner'), false);
        setVisible($('saveStatus'), false);
    }
}

$('loginForm').addEventListener('submit', login);
$('logoutButton').addEventListener('click', logout);
$('refreshButton').addEventListener('click', loadJobs);
$('newJobButton').addEventListener('click', openNewJob);
$('jobsTable').addEventListener('click', handleJobAction);
$('jobForm').addEventListener('submit', saveJob);
$('closeDialogButton').addEventListener('click', () => $('jobDialog').close());
$('cancelDialogButton').addEventListener('click', () => $('jobDialog').close());
document.querySelectorAll('.nav-item').forEach((item) => item.addEventListener('click', () => setDashboardView(item.dataset.view)));
['scheduleMode', 'minuteInterval', 'hourMinute', 'dailyHour', 'dailyMinute'].forEach((id) => $(id).addEventListener('input', updateSchedulePreview));
document.querySelectorAll('.stepper-button').forEach((button) => button.addEventListener('click', () => {
    const retries = $('maxRetries');
    retries.value = Math.max(Number(retries.min), Math.min(Number(retries.max), Number(retries.value || 0) + Number(button.dataset.step)));
}));
