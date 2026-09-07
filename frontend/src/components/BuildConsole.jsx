import React, { useEffect, useRef } from 'react';
import { Terminal, ArrowRight, ArrowLeft, RefreshCw, Wand2, Loader2, CheckCircle2, XCircle, ShieldCheck } from 'lucide-react';

export default function BuildConsole({ buildJob, onInspect, onPrev, onReRun, onAutoFix, autoFixing }) {
  const terminalRef = useRef(null);

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [buildJob?.buildLogs]);

  const status = buildJob?.status || 'INITIALIZED';
  const isSuccess = status === 'SUCCESS';
  const isFailure = status === 'FAILURE';
  const isRunning = status === 'COMPILING' || status === 'ADK_VALIDATION' || status === 'PACKAGING';

  return (
    <div className="glass-panel" style={{ padding: '28px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '17px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Terminal size={18} color="#2563eb" /> Step 5: Isolated Maven Build & ADK Check Worker
          </h2>
          <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Build Job ID: <code>{buildJob?.id}</code> &bull; Execution duration: {buildJob?.durationMs || 0} ms.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {autoFixing && (
            <span className="badge badge-purple" style={{ background: '#ede9fe', color: '#6d28d9', borderColor: '#c4b5fd' }}>
              <Loader2 size={12} className="animate-spin" /> Claude AI Auto-Fixing & Healing Code...
            </span>
          )}
          {!autoFixing && isRunning && (
            <span className="badge badge-blue">
              <Loader2 size={12} className="animate-spin" /> {status}...
            </span>
          )}
          {!autoFixing && isSuccess && (
            <span className="badge badge-green">
              <CheckCircle2 size={12} /> MAVEN BUILD & ADK SUCCESS
            </span>
          )}
          {!autoFixing && isFailure && (
            <span className="badge badge-red">
              <XCircle size={12} /> BUILD FAILED
            </span>
          )}
        </div>
      </div>

      {/* Terminal Log Console */}
      <div className="terminal-window" ref={terminalRef} style={{ marginBottom: '24px' }}>
        <pre style={{ margin: 0, fontFamily: 'var(--font-mono)' }}>
          {buildJob?.buildLogs || 'Initializing build process...'}
        </pre>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
        <button type="button" className="btn-secondary" onClick={onPrev}>
          <ArrowLeft size={16} /> Back to Source Code
        </button>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            type="button"
            className="btn-secondary"
            onClick={onReRun}
            disabled={isRunning || autoFixing}
            style={{ background: 'var(--bg-secondary)', borderColor: 'var(--border-color)' }}
          >
            <RefreshCw size={15} className={isRunning ? 'animate-spin' : ''} /> Re-Run Build
          </button>

          {isFailure && (
            <button
              type="button"
              className="btn-primary"
              onClick={onAutoFix}
              disabled={autoFixing}
              style={{
                background: 'linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%)',
                boxShadow: '0 4px 14px rgba(124, 58, 237, 0.3)'
              }}
            >
              <Wand2 size={16} /> AI Auto-Fix & Re-Run Build
            </button>
          )}

          <button
            type="button"
            className="btn-primary"
            onClick={onInspect}
            disabled={!isSuccess}
            style={{ opacity: isSuccess ? 1 : 0.5 }}
          >
            <ShieldCheck size={16} /> Inspect Artifacts & 12 ADK Checks <ArrowRight size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}
