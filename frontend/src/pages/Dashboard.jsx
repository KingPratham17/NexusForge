import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Package, Calendar, Clock, Edit3, Plus, ArrowRight, Layers, Download, Trash2 } from 'lucide-react';
import apiClient, { deleteProject, getEsaDownloadUrl } from '../services/api';

export default function Dashboard() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const response = await apiClient.get('/projects');
        setProjects(response.data);
      } catch (err) {
        console.error('Failed to fetch projects', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProjects();
  }, []);

  const handleEdit = (project) => {
    try {
      let spec = project.targetSpecJson;
      if (typeof spec === 'string') {
        spec = JSON.parse(spec);
      }
      if (!spec || typeof spec !== 'object') {
        alert("Specification data for this project is empty or invalid.");
        return;
      }
      navigate('/generator', { state: { editSpec: spec, projectId: project.id, projectName: project.name, buildId: project.buildId } });
    } catch (e) {
      alert("Failed to parse project specification: " + (e.message || 'Invalid JSON'));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this adapter project?')) return;
    try {
      await deleteProject(id);
      setProjects(projects.filter(p => p.id !== id));
    } catch (e) {
      alert("Failed to delete project: " + (e.response?.data?.error || e.message));
    }
  };

  const parseSpecSummary = (jsonStr) => {
    try {
      const spec = JSON.parse(jsonStr);
      return {
        scheme: spec?.adapter?.scheme || 'custom-adapter',
        vendor: spec?.adapter?.vendor || 'SAP Partner',
        version: spec?.adapter?.version || '1.0.0',
        description: spec?.adapter?.description || 'Custom SAP Integration Adapter'
      };
    } catch (e) {
      return {
        scheme: 'sap-adapter',
        vendor: 'SAP Partner',
        version: '1.0.0',
        description: 'Custom SAP Integration Adapter'
      };
    }
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-header-bar">
        <div className="dashboard-title-group">
          <h1>Your Adapters</h1>
          <p>Manage, inspect, and iterate on your compiled SAP Integration Suite adapters.</p>
        </div>
        <button
          onClick={() => navigate('/generator')}
          className="btn-primary"
        >
          <Plus size={16} />
          Create New Adapter
        </button>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '260px' }}>
          <div style={{
            width: '36px',
            height: '36px',
            border: '3px solid #e2e8f0',
            borderTopColor: '#2563eb',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite'
          }}></div>
        </div>
      ) : projects.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-state-icon">
            <Package size={30} />
          </div>
          <h3 style={{ fontSize: '18px', fontWeight: '600', color: '#0f172a', marginBottom: '8px' }}>
            No adapters compiled yet
          </h3>
          <p style={{ color: '#64748b', fontSize: '14px', maxWidth: '420px', margin: '0 auto 24px auto' }}>
            You haven't generated any SAP adapters yet. Use the AI compiler wizard to define your requirements and build your first ESA package.
          </p>
          <button
            onClick={() => navigate('/generator')}
            className="btn-primary"
          >
            <Plus size={16} /> Start Your First Adapter
          </button>
        </div>
      ) : (
        <div className="project-grid">
          {projects.map((project) => {
            const summary = parseSpecSummary(project.targetSpecJson);
            return (
              <div key={project.id} className="project-card">
                <div className="project-card-top">
                  <div className="project-card-icon">
                    <Layers size={22} />
                  </div>
                  <span className="badge badge-purple" style={{ fontSize: '11.5px' }}>
                    ADK 2.2 &bull; v{summary.version}
                  </span>
                </div>

                <div className="project-card-title" title={project.name}>
                  {project.name || 'Untitled Adapter'}
                </div>

                <div className="project-card-desc">
                  {summary.description}
                </div>

                <div style={{ marginBottom: '16px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <span className="badge badge-blue" style={{ fontSize: '11px' }}>
                    Scheme: {summary.scheme}
                  </span>
                  <span className="badge badge-green" style={{ fontSize: '11px' }}>
                    {summary.vendor}
                  </span>
                </div>

                <div className="project-card-footer">
                  <div className="project-card-meta">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                      <Calendar size={13} /> {new Date(project.createdAt).toLocaleDateString()}
                    </span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                      <Clock size={13} /> ID: #{project.id}
                    </span>
                  </div>

                  <div className="project-card-actions" style={{ display: 'flex', gap: '8px' }}>
                    {project.buildId && (
                      <a
                        href={getEsaDownloadUrl(project.buildId)}
                        download
                        className="btn-secondary"
                        style={{ padding: '6px 14px', fontSize: '12.5px', textDecoration: 'none' }}
                        title="Download compiled .esa file"
                      >
                        <Download size={13} />
                        <span>ESA</span>
                      </a>
                    )}
                    <button
                      onClick={() => handleEdit(project)}
                      className="btn-primary"
                      style={{ padding: '6px 14px', fontSize: '12.5px' }}
                      title="Open and review specification in generator"
                    >
                      <Edit3 size={13} />
                      <span>Edit</span>
                    </button>
                    <button
                      onClick={() => handleDelete(project.id)}
                      className="icon-btn"
                      style={{ padding: '6px', color: '#ef4444', border: '1px solid #fee2e2', borderRadius: '6px', background: '#fef2f2' }}
                      title="Delete project"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
