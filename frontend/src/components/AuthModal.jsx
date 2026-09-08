import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function AuthModal({ isOpen, onClose }) {
  const { login, register } = useAuth();
  const [isLogin, setIsLogin] = useState(true);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isLogin) {
        await login(email, password);
      } else {
        await register(name, email, password);
        await login(email, password); // Auto login after register
      }
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Authentication failed');
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(15, 23, 42, 0.6)',
      backdropFilter: 'blur(4px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999
    }}>
      <div className="glass-panel" style={{ width: '400px', padding: '32px', position: 'relative' }}>
        <h2 style={{ fontSize: '24px', fontWeight: '700', marginBottom: '24px', color: '#0f172a' }}>
          {isLogin ? 'Welcome Back' : 'Create Account'}
        </h2>
        
        {error && (
          <div style={{ padding: '12px', backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', borderRadius: '8px', marginBottom: '20px', fontSize: '13px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#475569', marginBottom: '8px' }}>Full Name</label>
              <input 
                type="text" 
                style={{
                  width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid #cbd5e1', 
                  fontSize: '14px', outline: 'none', transition: 'border-color 0.2s'
                }}
                value={name}
                onChange={(e) => setName(e.target.value)}
                required 
                placeholder="John Doe"
              />
            </div>
          )}
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#475569', marginBottom: '8px' }}>Email Address</label>
            <input 
              type="email" 
              style={{
                width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid #cbd5e1', 
                fontSize: '14px', outline: 'none', transition: 'border-color 0.2s'
              }}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required 
              placeholder="you@company.com"
            />
          </div>
          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#475569', marginBottom: '8px' }}>Password</label>
            <input 
              type="password" 
              style={{
                width: '100%', padding: '10px 14px', borderRadius: '8px', border: '1px solid #cbd5e1', 
                fontSize: '14px', outline: 'none', transition: 'border-color 0.2s'
              }}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required 
              placeholder="••••••••"
            />
          </div>
          
          <div style={{ display: 'flex', gap: '12px' }}>
            <button 
              type="submit" 
              className="btn-primary"
              style={{ flex: 1, padding: '10px', justifyContent: 'center' }}
            >
              {isLogin ? 'Sign In' : 'Register'}
            </button>
            <button 
              type="button" 
              onClick={() => onClose()}
              className="btn-secondary"
              style={{ padding: '10px 16px' }}
            >
              Cancel
            </button>
          </div>
        </form>
        
        <div style={{ marginTop: '24px', textAlign: 'center' }}>
          <button 
            type="button" 
            onClick={() => { setIsLogin(!isLogin); setError(''); }}
            style={{ background: 'none', border: 'none', color: '#2563eb', fontSize: '13px', fontWeight: '500', cursor: 'pointer' }}
          >
            {isLogin ? "Need an account? Sign up here" : "Already have an account? Log in"}
          </button>
        </div>
      </div>
    </div>
  );
}
