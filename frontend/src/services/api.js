const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_BASE = import.meta.env.VITE_AUTH_BASE_URL || 'http://localhost:8080/api/auth';

let _onUnauthorized = null;

export function setUnauthorizedCallback(callback) {
  _onUnauthorized = callback;
}

function getHeaders() {
  const user = JSON.parse(localStorage.getItem('user'));
  const headers = { 'Content-Type': 'application/json' };
  if (user && user.token) {
    headers['Authorization'] = 'Bearer ' + user.token;
  }
  return headers;
}

async function fetchWithAuth(url, options = {}) {
  const finalOptions = {
    ...options,
    headers: { ...getHeaders(), ...options.headers }
  };
  
  const res = await fetch(url, finalOptions);
  
  if (res.status === 401) {
    if (_onUnauthorized) {
      _onUnauthorized();
    }
    throw new Error('Session expired');
  }
  
  if (!res.ok) {
    let errorMsg = 'API request failed';
    try {
      const errData = await res.json();
      errorMsg = errData.message || errorMsg;
    } catch (e) {
      // Ignored
    }
    throw new Error(errorMsg);
  }
  
  const text = await res.text();
  return text ? JSON.parse(text) : {};
}

// Auth API
export async function login(username, password) {
  const res = await fetch(`${AUTH_BASE}/signin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) {
    let errorMsg = 'Invalid username or password';
    try {
      const errData = await res.json();
      errorMsg = errData.message || errorMsg;
    } catch (e) {}
    throw new Error(errorMsg);
  }
  return res.json();
}

export async function signup(username, password, email) {
  const res = await fetch(`${AUTH_BASE}/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, email })
  });
  if (!res.ok) {
    let errorMsg = 'Signup failed';
    try {
      const errData = await res.json();
      errorMsg = errData.message || errorMsg;
    } catch (e) {}
    throw new Error(errorMsg);
  }
  return res.json();
}

// Project API
export async function fetchProjects() {
  return fetchWithAuth(`${API_BASE}/projects`);
}

export async function fetchProject(id) {
  return fetchWithAuth(`${API_BASE}/projects/${id}`);
}

export async function createProject(projectData) {
  return fetchWithAuth(`${API_BASE}/projects`, {
    method: 'POST',
    body: JSON.stringify(projectData)
  });
}

export async function updateProject(id, updates) {
  return fetchWithAuth(`${API_BASE}/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(updates)
  });
}

export async function deleteProject(id) {
  return fetchWithAuth(`${API_BASE}/projects/${id}`, {
    method: 'DELETE'
  });
}

// Adapter Generation API
export async function analyzeRequirement(prompt, technologyId) {
  return fetchWithAuth(`${API_BASE}/requirements/analyze`, {
    method: 'POST',
    body: JSON.stringify({ prompt, technologyId }),
  });
}

export async function generateAdapter(projectId, specification) {
  return fetchWithAuth(`${API_BASE}/adapters/generate?projectId=${projectId}`, {
    method: 'POST',
    body: JSON.stringify(specification),
  });
}

export async function triggerBuild(buildId, workspacePath, adapterName, scheme) {
  return fetchWithAuth(`${API_BASE}/adapters/build`, {
    method: 'POST',
    body: JSON.stringify({ buildId, workspacePath, adapterName, scheme }),
  });
}

export async function fetchBuildStatus(buildId) {
  return fetchWithAuth(`${API_BASE}/adapters/build/${buildId}/status`);
}

export async function fetchGeneratedFiles(buildId) {
  return fetchWithAuth(`${API_BASE}/adapters/build/${buildId}/files`);
}

async function downloadArtifactWithAuth(url, defaultFilename) {
  const user = JSON.parse(localStorage.getItem('user'));
  const headers = {};
  if (user && user.token) {
    headers['Authorization'] = `Bearer ${user.token}`;
  }

  try {
    const response = await fetch(url, { method: 'GET', headers });
    
    if (response.status === 401) throw new Error('Unauthorized - please log in again');
    if (response.status === 403) throw new Error('Forbidden - you do not have permission');
    if (response.status === 404) throw new Error('File not found');
    if (!response.ok) throw new Error(`Download failed with status ${response.status}`);
    
    let filename = defaultFilename;
    const disposition = response.headers.get('Content-Disposition');
    if (disposition && disposition.indexOf('attachment') !== -1) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(disposition);
      if (matches != null && matches[1]) { 
        filename = matches[1].replace(/['"]/g, '');
      }
    }

    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = objectUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(objectUrl);
  } catch (err) {
    alert('Download error: ' + err.message);
    console.error('Download error:', err);
  }
}

export async function downloadEsa(buildId) {
  return downloadArtifactWithAuth(`${API_BASE}/adapters/build/${buildId}/download/esa`, `adapter-${buildId}.esa`);
}

export async function downloadSource(buildId) {
  return downloadArtifactWithAuth(`${API_BASE}/adapters/build/${buildId}/download/source`, `source-${buildId}.zip`);
}

export async function autoFixBuild(buildId, errorLog, specification) {
  return fetchWithAuth(`${API_BASE}/adapters/autofix`, {
    method: 'POST',
    body: JSON.stringify({ buildId, errorLog, specification }),
  });
}
