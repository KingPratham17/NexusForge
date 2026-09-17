const API_BASE = 'http://localhost:8080/api/v1';

function getHeaders() {
  const user = JSON.parse(localStorage.getItem('user'));
  const headers = { 'Content-Type': 'application/json' };
  if (user && user.token) {
    headers['Authorization'] = 'Bearer ' + user.token;
  }
  return headers;
}

export async function fetchTechnologies() {
  const res = await fetch(`${API_BASE}/technologies`, { headers: getHeaders() });
  if (!res.ok) throw new Error('Failed to fetch technology catalog');
  return res.json();
}

export async function analyzeRequirement(prompt, technologyId) {
  const res = await fetch(`${API_BASE}/requirements/analyze`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ prompt, technologyId }),
  });
  if (!res.ok) throw new Error('Failed to analyze requirement');
  return res.json();
}

export async function generateAdapter(specification) {
  const res = await fetch(`${API_BASE}/adapters/generate`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(specification),
  });
  if (!res.ok) throw new Error('Failed to generate adapter sources');
  return res.json();
}

export async function triggerBuild(buildId, workspacePath, adapterName, scheme) {
  const res = await fetch(`${API_BASE}/adapters/build`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ buildId, workspacePath, adapterName, scheme }),
  });
  if (!res.ok) throw new Error('Failed to trigger Maven build worker');
  return res.json();
}

export async function fetchBuildStatus(buildId) {
  const res = await fetch(`${API_BASE}/adapters/build/${buildId}/status`, { headers: getHeaders() });
  if (!res.ok) throw new Error('Failed to fetch build status');
  return res.json();
}

export function getEsaDownloadUrl(buildId) {
  const user = JSON.parse(localStorage.getItem('user'));
  return `${API_BASE}/adapters/build/${buildId}/download/esa?token=${user?.token || ''}`;
}

export async function autoFixBuild(buildId, errorLog, specification) {
  const res = await fetch(`${API_BASE}/adapters/autofix`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ buildId, errorLog, specification }),
  });
  if (!res.ok) throw new Error('Failed to auto-fix adapter code');
  return res.json();
}

export async function createProject(name) {
  const res = await fetch(`${API_BASE}/projects`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ name })
  });
  if (!res.ok) throw new Error('Failed to create project');
  return res.json();
}

export async function updateProject(id, updates) {
  const res = await fetch(`${API_BASE}/projects/${id}`, {
    method: 'PUT',
    headers: getHeaders(),
    body: JSON.stringify(updates)
  });
  if (!res.ok) throw new Error('Failed to update project');
  return res.json();
}
