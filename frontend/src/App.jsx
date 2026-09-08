import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/Header';
import Generator from './pages/Generator';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';
import { AuthContext } from './context/AuthContext';
import { useContext } from 'react';

import { GeneratorProvider } from './context/GeneratorContext';

import LandingPage from './pages/LandingPage';

// Protected Route Wrapper
const ProtectedRoute = ({ children }) => {
  const { token, loading } = useContext(AuthContext);
  
  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: '3px solid #e2e8f0',
          borderTopColor: '#2563eb',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite'
        }}></div>
      </div>
    );
  }

  if (!token) {
    // Redirect to home if not logged in
    return <Navigate to="/" replace />;
  }

  return children;
};

export default function App() {
  return (
    <GeneratorProvider>
      <BrowserRouter>
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--bg-primary)' }}>
        <Header />
        <Routes>
          {/* Default Route: Landing Page */}
          <Route path="/" element={<LandingPage />} />
          
          <Route path="/generator" element={<Generator />} />
          
          {/* Protected Routes */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/profile" 
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            } 
          />
          
          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
      </BrowserRouter>
    </GeneratorProvider>
  );
}
