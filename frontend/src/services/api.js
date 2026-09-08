import axios from 'axios';

const API_BASE = '/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Intercept requests to add the Authorization token if available
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('nexus_token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export async function fetchTechnologies() {
  const res = await apiClient.get(`/technologies`);
  return res.data;
}

export async function analyzeRequirement(prompt, technologyId) {
  const res = await apiClient.post(`/requirements/analyze`, { prompt, technologyId });
  return res.data;
}

export async function generateAdapter(specification) {
  const res = await apiClient.post(`/adapters/generate`, specification);
  return res.data;
}

export async function triggerBuild(buildId, workspacePath, adapterName, scheme) {
  const res = await apiClient.post(`/adapters/build`, { buildId, workspacePath, adapterName, scheme });
  return res.data;
}

export async function fetchBuildStatus(buildId) {
  const res = await apiClient.get(`/adapters/build/${buildId}/status`);
  return res.data;
}

export function getEsaDownloadUrl(buildId) {
  return `${API_BASE}/adapters/build/${buildId}/download/esa`;
}

export async function autoFixBuild(buildId, errorLog, specification) {
  const res = await apiClient.post(`/adapters/autofix`, { buildId, errorLog, specification });
  return res.data;
}

// --- Auth Endpoints ---

export async function login(email, password) {
  const res = await apiClient.post(`/auth/signin`, { email, password });
  if (res.data.token) {
    localStorage.setItem('nexus_token', res.data.token);
    localStorage.setItem('nexus_user', JSON.stringify(res.data));
  }
  return res.data;
}

export async function register(name, email, password) {
  const res = await apiClient.post(`/auth/signup`, { name, email, password });
  return res.data;
}

export function logout() {
  localStorage.removeItem('nexus_token');
  localStorage.removeItem('nexus_user');
}

// --- Project Endpoints ---

export async function saveProject(name, targetSpecJson, buildId) {
  const res = await apiClient.post(`/projects`, { name, targetSpecJson, buildId });
  return res.data;
}

export async function listProjects() {
  const res = await apiClient.get(`/projects`);
  return res.data;
}

export async function deleteProject(id) {
  const res = await apiClient.delete(`/projects/${id}`);
  return res.data;
}

export default apiClient;
