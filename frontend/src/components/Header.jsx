import React from 'react';
import { Cpu, CheckCircle, Shield, Sparkles } from 'lucide-react';

export default function Header({ user, onLogout }) {
  return (
    <header className="glass-panel" style={{ margin: '16px 24px', padding: '16px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{ 
          width: '44px', 
          height: '44px', 
          borderRadius: '10px', 
          background: 'linear-gradient(135deg, #2563eb, #7c3aed)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)' 
        }}>
          <Cpu size={24} color="white" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>
              NexusForge
            </h1>
            <span className="badge badge-purple">
              <Sparkles size={12} /> AI-Assisted ADK Compiler
            </span>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Universal Target System Compiler &bull; Schema-Aware ADK 2.2.0 &bull; Java 8 Target &bull; Powered by Claude AI
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span className="badge badge-blue">
          <Shield size={12} /> Secure Store Parameter Ready
        </span>
        <span className="badge badge-green">
          <CheckCircle size={12} /> Maven Worker Active
        </span>
        {user && (
          <div style={{ marginLeft: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontWeight: '500' }}>{user.username}</span>
            <button className="secondary-btn" onClick={onLogout} style={{ padding: '6px 12px', fontSize: '12px' }}>
              Logout
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
