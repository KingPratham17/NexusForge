import React, { createContext, useState, useContext, useEffect } from 'react';
import { setUnauthorizedCallback } from '../services/api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user'));
    } catch {
      return null;
    }
  });

  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    // When the component mounts, set the API unauthorized callback
    setUnauthorizedCallback(() => {
      logout(true);
    });
    setIsInitializing(false);
  }, []);

  const login = (userData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const logout = (sessionExpired = false) => {
    localStorage.removeItem('user');
    setUser(null);
    if (sessionExpired) {
      // Potentially can store a flash message in sessionStorage
      sessionStorage.setItem('auth_message', 'Your session has expired. Please sign in again.');
    }
  };

  if (isInitializing) {
    return <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading session...</div>;
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
