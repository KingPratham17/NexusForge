const API_BASE = '/api/v1';

export async function fetchTechnologies() {
  const res = await fetch(`${API_BASE}/technologies`);
  if (!res.ok) throw new Error('Failed to fetch technology catalog');
  return res.json();
}

export async function analyzeRequirement(prompt, technologyId) {
  const res = await fetch(`${API_BASE}/requirements/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt, technologyId }),
  });
  if (!res.ok) throw new Error('Failed to analyze requirement');
  return res.json();
}

export async function generateAdapter(specification) {
  const res = await fetch(`${API_BASE}/adapters/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(specification),
  });
  if (!res.ok) throw new Error('Failed to generate adapter sources');
  return res.json();
}

export async function triggerBuild(buildId, workspacePath, adapterName, scheme) {
  const res = await fetch(`${API_BASE}/adapters/build`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ buildId, workspacePath, adapterName, scheme }),
  });
  if (!res.ok) throw new Error('Failed to trigger Maven build worker');
  return res.json();
}

export async function fetchBuildStatus(buildId) {
  const res = await fetch(`${API_BASE}/adapters/build/${buildId}/status`);
  if (!res.ok) throw new Error('Failed to fetch build status');
  return res.json();
}

export function getEsaDownloadUrl(buildId) {
  return `${API_BASE}/adapters/build/${buildId}/download/esa`;
}

export async function autoFixBuild(buildId, errorLog, specification) {
  const res = await fetch(`${API_BASE}/adapters/autofix`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ buildId, errorLog, specification }),
  });
  if (!res.ok) throw new Error('Failed to auto-fix adapter code');
  return res.json();
}
