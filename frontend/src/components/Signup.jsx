import React, { useState } from 'react';

export default function Signup({ onSignupSuccess, onSwitchToLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await fetch('http://localhost:8080/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || 'Signup failed');
      alert('Signup successful! Please login.');
      onSignupSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="glass-panel" style={{ maxWidth: '400px', margin: '40px auto', padding: '32px' }}>
      <h2 style={{ marginBottom: '20px', textAlign: 'center' }}>Sign Up for NexusForge</h2>
      {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px' }}>Username</label>
          <input type="text" value={username} onChange={e => setUsername(e.target.value)} required style={{ width: '100%' }} />
        </div>
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', marginBottom: '5px' }}>Password</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required style={{ width: '100%' }} />
        </div>
        <button className="primary-btn" type="submit" disabled={loading} style={{ width: '100%', marginBottom: '10px' }}>
          {loading ? 'Signing up...' : 'Sign Up'}
        </button>
      </form>
      <div style={{ textAlign: 'center' }}>
        <button onClick={onSwitchToLogin} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', textDecoration: 'underline' }}>
          Already have an account? Login
        </button>
      </div>
    </div>
  );
}
