import React, { useState } from 'react';
import { Sparkles, ArrowRight, Zap, Brain, Cpu } from 'lucide-react';

const EXAMPLE_PROMPTS = [
  "Build a receiver adapter for Apache Cassandra to insert JSON rows into a keyspace table using service account credentials stored in SAP Secure Store.",
  "Create a sender adapter for Firebase Firestore that reads documents from a collection using OAuth2 service account token.",
  "Build a receiver adapter for AWS S3 to upload payload files to a bucket using IAM role credentials via SAP Secure Parameter Store.",
  "Create a sender adapter for Oracle Database 19c that executes stored procedures and reads result sets using JDBC connection.",
  "Build a receiver adapter for Salesforce REST API to upsert Account records via the /services/data/v57.0/composite endpoint using OAuth2 client credentials.",
  "Create a sender adapter for SAP HANA that queries tables via SQL using secure parameter credentials and returns JSON rows.",
];

export default function RequirementForm({ onAnalyze, loading }) {
  const [prompt, setPrompt] = useState('');
  const [vendor, setVendor] = useState('Custom');
  const [version, setVersion] = useState('1.0.0');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!prompt.trim()) return;
    onAnalyze(prompt.trim(), null, { vendor, version });
  };

  const useExample = (ex) => setPrompt(ex);

  return (
    <div className="glass-panel" style={{ padding: '32px' }}>

      {/* Header */}
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Brain size={20} color="#2563eb" />
          Step 1 — Describe Your Adapter Requirement
        </h2>
        <p style={{ fontSize: '14px', color: 'var(--text-secondary)', lineHeight: '1.6' }}>
          Write a plain English description of what you want to build. The AI extracts the target system, connection parameters, authentication model, direction, and generates the full SAP ADK specification automatically — no dropdowns, no hardcoded options.
        </p>
      </div>

      {/* AI Badge */}
      <div style={{
        display: 'inline-flex', alignItems: 'center', gap: '8px',
        padding: '6px 14px', borderRadius: '20px',
        background: 'linear-gradient(135deg, #eff6ff, #f5f3ff)',
        border: '1px solid #bfdbfe', marginBottom: '20px'
      }}>
        <Zap size={13} color="#2563eb" />
        <span style={{ fontSize: '12px', fontWeight: '600', color: '#1d4ed8' }}>
          Powered by Claude AI via your local Omniroute proxy — any target system works
        </span>
      </div>

      <form onSubmit={handleSubmit}>

        {/* Main prompt area */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontSize: '13.5px', fontWeight: '600', color: 'var(--text-primary)', marginBottom: '8px' }}>
            What adapter do you want to build?
          </label>
          <textarea
            rows={5}
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="Example: Build a receiver adapter for Apache Cassandra NoSQL database that inserts JSON documents into a keyspace table. Use SAP Secure Parameter Store for credentials. The adapter should support INSERT and UPSERT operations."
            style={{ fontSize: '14px', lineHeight: '1.6', resize: 'vertical', fontFamily: 'inherit' }}
            required
          />
          <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '6px' }}>
            Mention: target system, auth method, direction (sender/receiver), data format. The more detail, the better the AI output.
          </p>
        </div>

        {/* Optional overrides */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '28px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: 'var(--text-primary)', marginBottom: '6px' }}>
              Vendor (optional override)
            </label>
            <input
              type="text"
              value={vendor}
              onChange={(e) => setVendor(e.target.value)}
              placeholder="Custom"
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: 'var(--text-primary)', marginBottom: '6px' }}>
              Version (optional override)
            </label>
            <input
              type="text"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              placeholder="1.0.0"
            />
          </div>
        </div>

        {/* Example prompts */}
        <div style={{ marginBottom: '28px' }}>
          <label style={{
            display: 'block', fontSize: '13px', fontWeight: '600',
            color: 'var(--text-secondary)', marginBottom: '12px', letterSpacing: '0.5px', textTransform: 'uppercase'
          }}>
            Example Prompts — click to use
          </label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {EXAMPLE_PROMPTS.map((ex, i) => (
              <button
                key={i}
                type="button"
                onClick={() => useExample(ex)}
                style={{
                  textAlign: 'left', padding: '10px 14px',
                  background: '#f8fafc', border: '1px solid var(--border-color)',
                  borderRadius: '8px', cursor: 'pointer', fontSize: '13px',
                  color: 'var(--text-secondary)', lineHeight: '1.5',
                  transition: 'all 0.15s ease'
                }}
                onMouseEnter={e => { e.target.style.borderColor = '#2563eb'; e.target.style.color = '#1d4ed8'; e.target.style.background = '#eff6ff'; }}
                onMouseLeave={e => { e.target.style.borderColor = 'var(--border-color)'; e.target.style.color = 'var(--text-secondary)'; e.target.style.background = '#f8fafc'; }}
              >
                {ex}
              </button>
            ))}
          </div>
        </div>

        {/* Submit */}
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button type="submit" className="btn-primary" disabled={loading || !prompt.trim()} style={{ gap: '10px', padding: '12px 28px' }}>
            {loading
              ? <><Cpu size={16} style={{ animation: 'spin 1s linear infinite' }} /> AI Analyzing...</>
              : <><Sparkles size={16} /> Analyze with AI &amp; Extract Specification <ArrowRight size={16} /></>
            }
          </button>
        </div>
      </form>
    </div>
  );
}
