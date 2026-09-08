import React, { useState } from 'react';
import { CheckCheck, CheckCircle2, XCircle, Download, RotateCcw, PackageCheck, FileArchive, Save, Loader2, Upload } from 'lucide-react';
import { getEsaDownloadUrl, saveProject } from '../services/api';
import { useAuth } from '../context/AuthContext';
import AuthModal from './AuthModal';

export default function ArtifactInspector({ buildJob, specification, onReset }) {
  const { user } = useAuth();
  const [isAuthModalOpen, setAuthModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState('');

  const report = buildJob?.inspectionReport;
  const items = report?.checkItems || [];

  const handleSave = async () => {
    if (!user) {
      setAuthModalOpen(true);
      return;
    }
    
    setIsSaving(true);
    setSaveStatus('');
    try {
      await saveProject(specification?.adapter?.name || 'Untitled Adapter', JSON.stringify(specification), buildJob?.id);
      setSaveStatus('success');
    } catch (error) {
      console.error(error);
      setSaveStatus('error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '28px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <div>
          <h2 style={{ fontSize: '17px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '8px' }}>
            {buildJob?.isArchived ? (
              <><FileArchive size={20} color="#2563eb" /> Archived Project Artifacts</>
            ) : (
              <><CheckCheck size={20} color="#059669" /> Step 6: 12-Point ADK Artifact Inspection Report</>
            )}
          </h2>
          <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            {buildJob?.isArchived 
              ? 'This project was loaded from your saved archives. The full 12-point inspection report is only available immediately after a build.'
              : 'Empirical verification of OSGi bundle manifest headers, Camel service descriptors, dependency embeddings, and ESA subsystem structure.'
            }
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {buildJob?.isArchived ? (
             <span className="badge badge-blue" style={{ fontSize: '13px', padding: '6px 14px' }}>
               ✓ ARCHIVED
             </span>
          ) : (
            <span className={`badge ${report?.overallSuccess ? 'badge-green' : 'badge-red'}`} style={{ fontSize: '13px', padding: '6px 14px' }}>
              {report?.overallSuccess ? '✓ ALL 12 CHECKS PASSED' : '✗ ADK CHECKS FAILED'}
            </span>
          )}
        </div>
      </div>

      {/* Overview Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #059669', background: '#f0fdf4' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <FileArchive size={18} color="#059669" />
            <span style={{ fontSize: '13px', color: '#166534', fontWeight: '500' }}>Generated ESA Package</span>
          </div>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: '#15803d', wordBreak: 'break-all', fontWeight: '600' }}>
            {report?.esaPath ? report.esaPath.split(/[/\\]/).pop() : 'N/A'}
          </span>
        </div>

        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #2563eb', background: '#eff6ff' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <PackageCheck size={18} color="#2563eb" />
            <span style={{ fontSize: '13px', color: '#1e40af', fontWeight: '500' }}>Component JAR</span>
          </div>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: '#1d4ed8', wordBreak: 'break-all', fontWeight: '600' }}>
            {report?.jarPath ? report.jarPath.split(/[/\\]/).pop() : 'N/A'}
          </span>
        </div>

        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #7c3aed', background: '#f5f3ff' }}>
          <div style={{ fontSize: '13px', color: '#5b21b6', marginBottom: '4px', fontWeight: '500' }}>Inspection Score</div>
          <span style={{ fontSize: '20px', fontWeight: '700', color: '#6d28d9' }}>
            {report?.passedChecks || 0} / {report?.totalChecks || 12}
          </span>
        </div>
      </div>

      {/* 12 Check Cards Grid (Hidden if archived since report is not in memory) */}
      {!buildJob?.isArchived && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '12px', marginBottom: '24px' }}>
          {items.map((item) => (
            <div
              key={item.checkNumber}
              className="glass-card"
              style={{
                padding: '14px',
                borderColor: item.passed ? '#a7f3d0' : '#fecaca',
                background: item.passed ? '#ecfdf5' : '#fef2f2'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {item.passed ? <CheckCircle2 size={16} color="#059669" /> : <XCircle size={16} color="#dc2626" />}
                  <span style={{ fontSize: '13.5px', fontWeight: '600', color: item.passed ? '#065f46' : '#991b1b' }}>
                    #{item.checkNumber}. {item.checkName}
                  </span>
                </div>
                <span className={`badge ${item.passed ? 'badge-green' : 'badge-red'}`} style={{ fontSize: '10px' }}>
                  {item.passed ? 'PASS' : 'FAIL'}
                </span>
              </div>
              <p style={{ fontSize: '12.5px', color: item.passed ? '#047857' : '#b91c1c', lineHeight: '1.4', paddingLeft: '24px' }}>
                {item.details}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Footer Action Buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button type="button" className="btn-secondary" onClick={onReset}>
          <RotateCcw size={16} /> Create Another Custom Adapter
        </button>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            type="button"
            className={user ? "btn-secondary" : "btn-primary"}
            onClick={handleSave}
            disabled={isSaving || saveStatus === 'success' || buildJob?.status !== 'SUCCESS'}
            style={{
              background: saveStatus === 'success' ? '#10b981' : (buildJob?.status !== 'SUCCESS' ? '#f1f5f9' : (user ? 'white' : '#f59e0b')),
              color: saveStatus === 'success' ? 'white' : (buildJob?.status !== 'SUCCESS' ? '#94a3b8' : (user ? '#0f172a' : 'white')),
              borderColor: saveStatus === 'success' ? '#10b981' : (buildJob?.status !== 'SUCCESS' ? '#e2e8f0' : (user ? '#e2e8f0' : '#f59e0b')),
              cursor: buildJob?.status !== 'SUCCESS' ? 'not-allowed' : 'pointer'
            }}
          >
            {isSaving ? <Loader2 size={16} className="animate-spin" /> : 
             saveStatus === 'success' ? <CheckCircle2 size={16} /> : 
             buildJob?.status !== 'SUCCESS' ? <XCircle size={16} /> :
             user ? <Save size={16} /> : <Upload size={16} />}
            {isSaving ? 'Saving...' : 
             saveStatus === 'success' ? 'Saved to DB' : 
             buildJob?.status !== 'SUCCESS' ? 'ESA Build Required to Save' :
             user ? 'Save Project' : 'Login to Save Project'}
          </button>

          {buildJob?.id && (
            <a
              href={getEsaDownloadUrl(buildJob.id)}
              download
              className="btn-primary"
              style={{ textDecoration: 'none' }}
            >
              <Download size={16} /> Download ESA Deployment Package (.esa)
            </a>
          )}
        </div>
      </div>
      <AuthModal isOpen={isAuthModalOpen} onClose={() => setAuthModalOpen(false)} />
    </div>
  );
}
