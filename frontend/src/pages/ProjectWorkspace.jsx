import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchProject, fetchBuildStatus, fetchGeneratedFiles, downloadEsa, downloadSource } from '../services/api';
import { Loader, ArrowLeft, Settings, Code, Box, Package, Activity, Play, Download } from 'lucide-react';
import SpecificationViewer from '../components/SpecificationViewer';
import SourceCodeViewer from '../components/SourceCodeViewer';
import BuildConsole from '../components/BuildConsole';
import ArtifactInspector from '../components/ArtifactInspector';

export default function ProjectWorkspace() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  
  const [buildJob, setBuildJob] = useState(null);
  const [generatedResult, setGeneratedResult] = useState(null);

  useEffect(() => {
    loadProject();
  }, [projectId]);

  const loadProject = async () => {
    if (projectId === 'new') {
      navigate('/app', { replace: true });
      return;
    }
    
    try {
      setLoading(true);
      const data = await fetchProject(projectId);
      setProject(data);
      
      if (data.buildId) {
        try {
          const buildData = await fetchBuildStatus(data.buildId);
          setBuildJob(buildData);
          
          try {
            const files = await fetchGeneratedFiles(data.buildId);
            setGeneratedResult({ 
              workspacePath: buildData.workspacePath, 
              buildId: data.buildId, 
              generatedFiles: files, 
              filesCount: Object.keys(files).length 
            });
          } catch(e) {
            setGeneratedResult({ workspacePath: buildData.workspacePath, buildId: data.buildId });
          }
        } catch (err) {
          setBuildJob(null);
          // try fetching files anyway in case they exist on disk even if build job is not in memory
          try {
            const files = await fetchGeneratedFiles(data.buildId);
            setGeneratedResult({ 
              workspacePath: "", 
              buildId: data.buildId, 
              generatedFiles: files, 
              filesCount: Object.keys(files).length 
            });
          } catch(e) {
            setGeneratedResult(null);
          }
        }
      }
    } catch (err) {
      setError(err.message || 'Failed to load project');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Loader size={32} className="spin" color="#2563eb" />
      </div>
    );
  }

  if (error || !project) {
    return (
      <div style={{ padding: '40px 24px', textAlign: 'center' }}>
        <div style={{ color: '#b91c1c', marginBottom: '16px' }}>{error || 'Project not found'}</div>
        <Link to="/app" className="btn-secondary"><ArrowLeft size={16} /> Back to Dashboard</Link>
      </div>
    );
  }

  const spec = project.adapterSpecificationJson ? JSON.parse(project.adapterSpecificationJson) : null;

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      
      {/* Project Header */}
      <div style={{ background: '#ffffff', borderBottom: '1px solid var(--border-color)', padding: '24px 32px' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <Link to="/app" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: 'var(--text-secondary)', textDecoration: 'none', marginBottom: '16px' }}>
              <ArrowLeft size={14} /> Back to Projects
            </Link>
            <h1 style={{ fontSize: '24px', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '8px' }}>
              {project.name}
            </h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '13.5px', color: 'var(--text-secondary)' }}>
              <span><strong style={{ color: 'var(--text-primary)' }}>Tech:</strong> {project.technology || 'N/A'}</span>
              <span><strong style={{ color: 'var(--text-primary)' }}>Type:</strong> {project.direction || 'N/A'}</span>
              <span className={`badge ${['SUCCESS', 'SAVED'].includes(project.status) ? 'badge-green' : project.status === 'FAILED' ? 'badge-red' : 'badge-blue'}`}>
                {project.status || 'DRAFT'}
              </span>
            </div>
          </div>
          
          <Link to={`/app/projects/${project.id}/generate`} className="btn-primary">
            <Play size={16} /> {spec ? 'Regenerate Adapter' : 'Start Generation'}
          </Link>
        </div>
        
        {/* Tabs */}
        <div style={{ maxWidth: '1200px', margin: '24px auto 0 auto', display: 'flex', gap: '24px', borderBottom: '1px solid transparent' }}>
          <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} icon={<Activity size={16} />} label="Overview" />
          <TabButton active={activeTab === 'specification'} onClick={() => setActiveTab('specification')} icon={<Settings size={16} />} label="Specification" disabled={!spec} />
          <TabButton active={activeTab === 'code'} onClick={() => setActiveTab('code')} icon={<Code size={16} />} label="Generated Code" disabled={!buildJob} />
          <TabButton active={activeTab === 'builds'} onClick={() => setActiveTab('builds')} icon={<Box size={16} />} label="Build Details" disabled={!buildJob} />
          <TabButton active={activeTab === 'artifacts'} onClick={() => setActiveTab('artifacts')} icon={<Package size={16} />} label="Artifacts" disabled={!buildJob || buildJob.status !== 'SUCCESS'} />
        </div>
      </div>

      {/* Tab Content */}
      <div style={{ flex: 1, padding: '32px', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
        {activeTab === 'overview' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
            <div className="glass-card" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '20px' }}>Project Details</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '14px' }}>
                <DetailRow label="Name" value={project.name} />
                <DetailRow label="Technology" value={project.technology} />
                <DetailRow label="Direction" value={project.direction} />
                <DetailRow label="Created" value={new Date(project.createdAt).toLocaleString()} />
                <DetailRow label="Last Updated" value={new Date(project.lastUpdated).toLocaleString()} />
                <DetailRow label="Current Phase" value={`Step ${project.currentStep || 1}`} />
              </div>
            </div>
            
            <div className="glass-card" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '20px' }}>Latest Build Status</h3>
              {buildJob ? (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                    <span className={`badge ${buildJob.status === 'SUCCESS' ? 'badge-green' : buildJob.status === 'FAILED' ? 'badge-red' : 'badge-blue'}`} style={{ fontSize: '14px', padding: '6px 12px' }}>
                      {buildJob.status}
                    </span>
                    <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>ID: {buildJob.id}</span>
                  </div>
                  {buildJob.status === 'SUCCESS' && (
                    <p style={{ fontSize: '14px', color: '#047857', marginBottom: '20px' }}>
                      Adapter successfully built and packed into ESA.
                    </p>
                  )}
                  {buildJob.status === 'FAILED' && (
                    <p style={{ fontSize: '14px', color: '#b91c1c', marginBottom: '20px' }}>
                      Build failed during phase: {buildJob.stage}. See Build Details tab for logs.
                    </p>
                  )}
                </div>
              ) : (
                <div style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
                  No builds have been executed for this project yet.
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'specification' && spec && (
          <SpecificationViewer 
            specification={spec} 
            readonly={true}
          />
        )}

        {activeTab === 'code' && buildJob && (
          <SourceCodeViewer 
            generatedResult={generatedResult} 
            readonly={true}
          />
        )}

        {activeTab === 'builds' && buildJob && (
          <BuildConsole 
            buildJob={buildJob} 
            readonly={true}
          />
        )}

        {activeTab === 'artifacts' && buildJob && buildJob.status === 'SUCCESS' && (
          <div className="glass-card" style={{ padding: '32px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '600', marginBottom: '24px' }}>Deployable Artifacts</h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '20px', border: '1px solid #e2e8f0', borderRadius: '8px', background: '#f8fafc' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{ width: '40px', height: '40px', borderRadius: '8px', background: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Package size={20} />
                  </div>
                  <div>
                    <div style={{ fontWeight: '600', color: 'var(--text-primary)', marginBottom: '4px' }}>Enterprise Service Archive (.esa)</div>
                    <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Ready for deployment to SAP Cloud Integration</div>
                  </div>
                </div>
                <button onClick={(e) => { e.preventDefault(); downloadEsa(buildJob.id); }} className="btn-primary">
                  <Download size={16} /> Download ESA
                </button>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '20px', border: '1px solid #e2e8f0', borderRadius: '8px', background: '#ffffff' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{ width: '40px', height: '40px', borderRadius: '8px', background: '#f1f5f9', color: '#64748b', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Code size={20} />
                  </div>
                  <div>
                    <div style={{ fontWeight: '600', color: 'var(--text-primary)', marginBottom: '4px' }}>Source Code Archive (.zip)</div>
                    <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Complete generated Maven project source code</div>
                  </div>
                </div>
                <button onClick={(e) => { e.preventDefault(); downloadSource(buildJob.id); }} className="btn-secondary">
                  <Download size={16} /> Download Source
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function TabButton({ active, onClick, icon, label, disabled }) {
  return (
    <button 
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={{ 
        background: 'transparent', 
        border: 'none', 
        padding: '0 0 12px 0', 
        fontSize: '14px', 
        fontWeight: '500', 
        color: disabled ? '#cbd5e1' : active ? '#2563eb' : '#64748b',
        borderBottom: `2px solid ${active ? '#2563eb' : 'transparent'}`,
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        transform: 'translateY(1px)' // cover the container border
      }}
    >
      {icon} {label}
    </button>
  );
}

function DetailRow({ label, value }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: '8px' }}>
      <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
      <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{value || '—'}</span>
    </div>
  );
}
