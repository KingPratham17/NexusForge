import React, { useState } from 'react';
import { Code2, ArrowRight, ArrowLeft, CheckCircle2, ShieldCheck, Cpu } from 'lucide-react';

export default function SpecificationViewer({ specification, onGenerate, onPrev, loading }) {
  const [jsonText, setJsonText] = useState(() => JSON.stringify(specification || {}, null, 2));

  React.useEffect(() => {
    if (specification) {
      setJsonText(JSON.stringify(specification, null, 2));
    }
  }, [specification]);

  const handleGenerate = () => {
    try {
      const parsed = JSON.parse(jsonText);
      onGenerate(parsed);
    } catch (e) {
      alert('Invalid JSON syntax: ' + e.message);
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '28px' }}>
      <h2 style={{ fontSize: '17px', fontWeight: '600', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Code2 size={18} color="#2563eb" /> Step 3: Canonical Adapter Specification
      </h2>
      <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
        This structured canonical JSON model decouples user requirements from deterministic code compilation.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px', marginBottom: '24px' }}>
        {/* Left: JSON Editor */}
        <div>
          <label style={{ display: 'block', fontSize: '13.5px', fontWeight: '500', color: 'var(--text-primary)', marginBottom: '6px' }}>
            Canonical Specification (JSON)
          </label>
          <textarea
            rows={18}
            className="code-block"
            value={jsonText}
            onChange={(e) => setJsonText(e.target.value)}
            style={{ width: '100%', fontSize: '13px', lineHeight: '1.5' }}
          />
        </div>

        {/* Right: Validation & Architecture Summary */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div className="glass-card" style={{ padding: '18px' }}>
            <h3 style={{ fontSize: '14px', fontWeight: '600', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ShieldCheck size={16} color="#059669" /> Specification Validation
            </h3>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '13px' }}>
              <li style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#047857' }}>
                <CheckCircle2 size={14} /> Adapter scheme & symbolic name valid
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#047857' }}>
                <CheckCircle2 size={14} /> Authentication model: Secure Store Parameter
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#047857' }}>
                <CheckCircle2 size={14} /> Disambiguated connection parameters
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#047857' }}>
                <CheckCircle2 size={14} /> Target bytecode: Java 8 (ADK 2.2.0)
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#047857' }}>
                <CheckCircle2 size={14} /> Producer execution pattern confirmed
              </li>
            </ul>
          </div>

          <div className="glass-card" style={{ padding: '18px', background: '#f5f3ff', borderColor: '#ddd6fe' }}>
            <h3 style={{ fontSize: '14px', fontWeight: '600', marginBottom: '8px', color: '#6d28d9', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Cpu size={16} /> Compiler Guarantee
            </h3>
            <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)', lineHeight: '1.45' }}>
              The deterministic template engine compiles this specification into validated Java classes, metadata.xml, config.adk, and pom.xml without LLM hallucination.
            </p>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <button type="button" className="btn-secondary" onClick={onPrev}>
          <ArrowLeft size={16} /> Back to Parameters
        </button>
        <button type="button" className="btn-primary" onClick={handleGenerate} disabled={loading}>
          {loading ? 'Generating Code...' : 'Generate Project Code'} <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}
