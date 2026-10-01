import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchProjects, createProject, deleteProject } from '../services/api';
import { Plus, Folder, LayoutGrid, Loader, ChevronRight, Trash2 } from 'lucide-react';

export default function DashboardPage() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      const data = await fetchProjects();
      setProjects(data);
    } catch (err) {
      setError(err.message || 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateProject = async () => {
    navigate(`/app/projects/new/generate`);
  };

  const handleDeleteProject = async (e, id) => {
    e.stopPropagation(); // Prevent card click
    if (!window.confirm('Are you sure you want to delete this project?')) return;
    try {
      await deleteProject(id);
      setProjects(projects.filter(p => p.id !== id));
    } catch (err) {
      alert(err.message || 'Failed to delete project');
    }
  };

  if (loading) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Loader size={32} className="spin" color="#2563eb" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', width: '100%', padding: '40px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '700', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <LayoutGrid size={24} color="#2563eb" />
            Projects
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>Manage your saved adapter generation projects</p>
        </div>
        
        <button className="btn-primary" onClick={handleCreateProject} disabled={creating}>
          {creating ? <Loader size={16} className="spin" /> : <Plus size={16} />} 
          New Adapter
        </button>
      </div>

      {error && (
        <div style={{ padding: '16px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#b91c1c', marginBottom: '24px' }}>
          {error}
        </div>
      )}

      {projects.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '64px 24px', background: 'white', borderRadius: '12px', border: '1px dashed #cbd5e1' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '48px', height: '48px', borderRadius: '50%', background: '#f1f5f9', color: '#94a3b8', marginBottom: '16px' }}>
            <Folder size={24} />
          </div>
          <h3 style={{ fontSize: '18px', fontWeight: '600', marginBottom: '8px' }}>No projects yet</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>Create your first custom adapter project to get started.</p>
          <button className="btn-primary" onClick={handleCreateProject} disabled={creating}>
            <Plus size={16} /> Create Adapter
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {projects.map(p => (
            <div 
              key={p.id} 
              className="glass-card" 
              style={{ padding: '20px', cursor: 'pointer', display: 'flex', flexDirection: 'column', position: 'relative' }}
              onClick={() => navigate(`/app/projects/${p.id}`)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', paddingRight: '24px' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: 'var(--text-primary)' }}>{p.name}</h3>
                <span className={`badge ${['SUCCESS', 'SAVED'].includes(p.status) ? 'badge-green' : p.status === 'FAILED' ? 'badge-red' : 'badge-blue'}`} style={{ fontSize: '11px' }}>
                  {p.status || 'DRAFT'}
                </span>
              </div>
              
              <button 
                onClick={(e) => handleDeleteProject(e, p.id)}
                style={{ position: 'absolute', top: '16px', right: '16px', background: 'transparent', border: 'none', color: '#cbd5e1', padding: '4px', cursor: 'pointer' }}
                onMouseOver={(e) => e.currentTarget.style.color = '#ef4444'}
                onMouseOut={(e) => e.currentTarget.style.color = '#cbd5e1'}
                title="Delete project"
              >
                <Trash2 size={18} />
              </button>
              
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '24px', flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Technology:</span>
                  <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{p.technology || 'Unspecified'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Direction:</span>
                  <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{p.direction || 'Unspecified'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Last updated:</span>
                  <span>{new Date(p.lastUpdated).toLocaleDateString()}</span>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', color: '#2563eb', fontSize: '13px', fontWeight: '500', borderTop: '1px solid #f1f5f9', paddingTop: '12px' }}>
                View Project <ChevronRight size={16} />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
