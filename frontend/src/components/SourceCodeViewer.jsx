import React, { useState } from 'react';
import { FolderGit2, FileText, ArrowRight, ArrowLeft, Play, CheckCircle2 } from 'lucide-react';

export default function SourceCodeViewer({ generatedResult, onBuild, onPrev, loading }) {
  const files = generatedResult?.generatedFiles || {};
  const fileNames = Object.keys(files);
  const [selectedFile, setSelectedFile] = useState(fileNames[0] || '');

  return (
    <div className="glass-panel" style={{ padding: '28px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '17px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FolderGit2 size={18} color="#2563eb" /> Step 4: Generated SAP ADK Project Source Files
          </h2>
          <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Isolated Workspace: <code>{generatedResult?.workspacePath}</code> &bull; Generated {generatedResult?.filesCount} artifacts.
          </p>
        </div>
        <span className="badge badge-green">
          <CheckCircle2 size={12} /> Deterministically Compiled
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: '16px', marginBottom: '24px' }}>
        {/* Left: File Tree */}
        <div className="glass-card" style={{ padding: '12px', maxHeight: '480px', overflowY: 'auto' }}>
          <h3 style={{ fontSize: '11px', fontWeight: '600', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '8px', paddingLeft: '8px' }}>
            Project Explorer
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {fileNames.map((fileName) => {
              const isSelected = selectedFile === fileName;
              return (
                <div
                  key={fileName}
                  onClick={() => setSelectedFile(fileName)}
                  style={{
                    padding: '8px 10px',
                    borderRadius: '6px',
                    fontSize: '12.5px',
                    fontFamily: 'var(--font-mono)',
                    cursor: 'pointer',
                    background: isSelected ? '#eff6ff' : 'transparent',
                    color: isSelected ? '#1d4ed8' : 'var(--text-secondary)',
                    border: isSelected ? '1px solid #bfdbfe' : '1px solid transparent',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}
                >
                  <FileText size={14} color={isSelected ? '#2563eb' : '#64748b'} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {fileName}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: Code Content */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#0f172a', padding: '10px 16px', borderTopLeftRadius: '8px', borderTopRightRadius: '8px', border: '1px solid #1e293b', borderBottom: 'none' }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: '#60a5fa' }}>
              {selectedFile}
            </span>
            <span style={{ fontSize: '11px', color: '#94a3b8' }}>Read-Only Verified</span>
          </div>
          <pre className="code-block" style={{ borderTopLeftRadius: 0, borderTopRightRadius: 0, height: '436px', overflowY: 'auto' }}>
            <code>{files[selectedFile] || '// Select a file from explorer'}</code>
          </pre>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <button type="button" className="btn-secondary" onClick={onPrev}>
          <ArrowLeft size={16} /> Back to Specification
        </button>
        <button type="button" className="btn-primary" onClick={onBuild} disabled={loading}>
          <Play size={16} /> {loading ? 'Launching Build Worker...' : 'Build Project with Maven Worker'} <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}
