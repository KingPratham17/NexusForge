import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import DashboardPage from './pages/DashboardPage';
import GenerateAdapterPage from './pages/GenerateAdapterPage';
import ProjectWorkspace from './pages/ProjectWorkspace';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        {/* Public Routes */}
        <Route index element={<LandingPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />

        {/* Protected Application Routes */}
        <Route path="app" element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } />
        
        <Route path="app/projects/:projectId" element={
          <ProtectedRoute>
            <ProjectWorkspace />
          </ProtectedRoute>
        } />

        <Route path="app/projects/:projectId/generate" element={
          <ProtectedRoute>
            <GenerateAdapterPage />
          </ProtectedRoute>
        } />
      </Route>
    </Routes>
  );
}
