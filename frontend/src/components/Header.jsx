import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Cpu, CheckCircle, Shield, Sparkles, LogIn, LogOut, LayoutDashboard, User } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import AuthModal from './AuthModal';

export default function Header() {
  const { user, logout } = useAuth();
  const [isAuthModalOpen, setAuthModalOpen] = useState(false);
  const location = useLocation();

  const isCurrent = (path) => location.pathname === path;

  return (
    <header className="glass-panel" style={{ margin: '0', padding: '14px 32px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderRadius: '0', borderLeft: 'none', borderRight: 'none', borderTop: 'none' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <Link to="/" style={{ 
          width: '42px', 
          height: '42px', 
          borderRadius: '10px', 
          background: 'linear-gradient(135deg, #2563eb, #7c3aed)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
          textDecoration: 'none'
        }}>
          <Cpu size={22} color="white" />
        </Link>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Link to="/" style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', textDecoration: 'none', letterSpacing: '-0.02em' }}>
              NexusForge
            </Link>
            <span className="badge badge-purple">
              <Sparkles size={12} /> AI-Assisted ADK Compiler
            </span>
          </div>
          <p style={{ fontSize: '12.5px', color: 'var(--text-muted)', marginTop: '2px' }}>
            Universal Target System Compiler &bull; Schema-Aware ADK 2.2.0 &bull; Java 8 Target
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Navigation Tabs */}
        {user && (
          <nav className="nav-pill-group">
            <Link 
              to="/generator" 
              className={`nav-link-pill ${isCurrent('/') || isCurrent('/generator') ? 'active' : ''}`}
            >
              <Sparkles size={14} />
              <span>Generator</span>
            </Link>
            <Link 
              to="/dashboard" 
              className={`nav-link-pill ${isCurrent('/dashboard') ? 'active' : ''}`}
            >
              <LayoutDashboard size={14} />
              <span>Dashboard</span>
            </Link>
          </nav>
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="badge badge-blue">
            <Shield size={12} /> Secure
          </span>
          <span className="badge badge-green">
            <CheckCircle size={12} /> Ready
          </span>
        </div>

        <div style={{ width: '1px', height: '22px', backgroundColor: '#e2e8f0' }}></div>
        
        {user ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Link 
              to="/profile" 
              className={`user-profile-badge ${isCurrent('/profile') ? 'active' : ''}`}
              title="View Profile"
            >
              <div className="user-avatar-circle">
                {user.name ? user.name.charAt(0).toUpperCase() : (user.email ? user.email.charAt(0).toUpperCase() : 'U')}
              </div>
              <span style={{ fontSize: '13.5px', fontWeight: '600', color: '#334155' }}>
                {user.name || user.email?.split('@')[0]}
              </span>
            </Link>
            <button 
              onClick={logout}
              title="Sign Out"
              className="icon-btn" 
              style={{ padding: '7px 10px', color: '#94a3b8' }}
            >
              <LogOut size={16} />
            </button>
          </div>
        ) : (
          <button 
            onClick={() => setAuthModalOpen(true)}
            className="btn-primary" 
            style={{ padding: '8px 18px', fontSize: '13.5px' }}
          >
            <LogIn size={15} /> Sign In
          </button>
        )}
      </div>

      <AuthModal isOpen={isAuthModalOpen} onClose={() => setAuthModalOpen(false)} />
    </header>
  );
}
