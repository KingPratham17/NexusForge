import React from 'react';
import { User, Mail, Shield, LogOut, CheckCircle, Database } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user, logout } = useAuth();

  if (!user) {
    return (
      <div className="profile-page-wrapper" style={{ textAlign: 'center', paddingTop: '80px' }}>
        <div className="empty-state-box">
          <User size={36} color="#94a3b8" style={{ margin: '0 auto 12px auto' }} />
          <h3 style={{ fontSize: '18px', fontWeight: '600', color: '#0f172a', marginBottom: '6px' }}>
            Session Expired or Not Logged In
          </h3>
          <p style={{ color: '#64748b', fontSize: '14px' }}>Please sign in to access your account profile and saved adapters.</p>
        </div>
      </div>
    );
  }

  const userInitial = user.name ? user.name.charAt(0).toUpperCase() : (user.email ? user.email.charAt(0).toUpperCase() : 'U');

  return (
    <div className="profile-page-wrapper">
      <div className="profile-card-hero">
        <div className="profile-banner"></div>
        
        <div className="profile-hero-content">
          <div className="profile-avatar-large">
            {userInitial}
          </div>

          <div className="profile-user-info-row">
            <div>
              <h2 className="profile-name-title">{user.name || 'NexusForge Developer'}</h2>
              <div className="profile-meta-tags">
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <Mail size={15} color="#64748b" />
                  <span>{user.email}</span>
                </span>
                <span>&bull;</span>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <Shield size={15} color="#2563eb" />
                  <span className="badge badge-blue">
                    {user.roles && user.roles.length > 0 ? user.roles.join(', ') : 'ROLE_USER'}
                  </span>
                </span>
                <span className="badge badge-green">
                  <CheckCircle size={12} /> Active Account
                </span>
              </div>
            </div>

            <div>
              <button
                onClick={logout}
                className="btn-signout"
              >
                <LogOut size={16} />
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        </div>

        <div className="profile-info-grid">
          <div className="profile-info-card">
            <div className="profile-info-label">User Identifier</div>
            <div className="profile-info-value" style={{ fontFamily: 'var(--font-mono)', color: '#2563eb' }}>
              #{user.id || '1'}
            </div>
          </div>

          <div className="profile-info-card">
            <div className="profile-info-label">Compiler Tier</div>
            <div className="profile-info-value">Enterprise ADK 2.2</div>
          </div>

          <div className="profile-info-card">
            <div className="profile-info-label">Storage Segregation</div>
            <div className="profile-info-value">Multi-Tenant Isolated</div>
          </div>
        </div>
      </div>
    </div>
  );
}
