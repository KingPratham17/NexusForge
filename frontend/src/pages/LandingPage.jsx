import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthModal from '../components/AuthModal';
import { Zap, Shield, Cpu, Layers, ArrowRight, Code, Database, Sparkles } from 'lucide-react';

export default function LandingPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [isAuthModalOpen, setAuthModalOpen] = useState(false);

  const handlePrimaryCTA = () => {
    if (user) {
      navigate('/generator');
    } else {
      setAuthModalOpen(true);
    }
  };

  return (
    <div className="landing-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-background-glow"></div>
        <div className="hero-content">
          <div className="hero-badge">
            <Sparkles size={14} className="sparkle-icon" />
            <span>NexusForge Engine v2.0 is Live</span>
          </div>
          <h1 className="hero-title">
            The World's Smartest <br/>
            <span className="text-gradient">SAP ADK Compiler</span>
          </h1>
          <p className="hero-subtitle">
            Instantly translate natural language into production-ready OSGi bundles. Built with zero-trust credential security, isolated workspaces, and 12-point architectural validation.
          </p>
          <div className="hero-actions">
            <button className="btn-primary btn-large cta-btn" onClick={handlePrimaryCTA}>
              {user ? 'Launch Compiler' : 'Start Building for Free'} <ArrowRight size={18} />
            </button>
            <button className="btn-secondary btn-large" onClick={() => navigate('/generator')}>
              Try Sandbox Without Login
            </button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="features-header">
          <h2>Engineered for SAP Architects</h2>
          <p>Everything you need to rapidly prototype and deploy custom CPI adapters.</p>
        </div>

        <div className="features-grid">
          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#eff6ff', color: '#2563eb' }}>
              <Zap size={24} />
            </div>
            <h3>AI-Driven Generation</h3>
            <p>Describe your integration requirements in plain English. Claude AI automatically produces structured ADK artifacts, Camel routes, and component descriptors.</p>
          </div>

          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
              <Layers size={24} />
            </div>
            <h3>12-Point Inspection</h3>
            <p>Every compiled <code>.esa</code> package undergoes strict OSGi validation, checking package exports, class dependencies, and XML descriptor integrity.</p>
          </div>

          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#f0fdf4', color: '#059669' }}>
              <Shield size={24} />
            </div>
            <h3>Zero-Trust Security</h3>
            <p>Strict parameter aliasing prevents raw credentials from ever being injected into the source code. Uses standard <code>SAP_SECURE_ALIAS</code> abstraction by default.</p>
          </div>

          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#fffbeb', color: '#d97706' }}>
              <Cpu size={24} />
            </div>
            <h3>Auto-Healing Builds</h3>
            <p>If the Maven compilation fails, the built-in diagnostic engine feeds stack traces back to the AI for self-correction and instant re-compilation.</p>
          </div>

          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#fef2f2', color: '#dc2626' }}>
              <Database size={24} />
            </div>
            <h3>Stateful Persistence</h3>
            <p>Never lose your progress. NexusForge saves your build workspaces, specification history, and final ESA artifacts for instantaneous resumption and download.</p>
          </div>

          <div className="feature-card glass-card">
            <div className="feature-icon-wrapper" style={{ background: '#f0f9ff', color: '#0284c7' }}>
              <Code size={24} />
            </div>
            <h3>Pure Java 8 Fidelity</h3>
            <p>Generates deeply backwards-compatible, thread-safe Producer/Consumer components targeted exactly at SAP Integration Suite's OSGi runtime environment.</p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="footer-content">
          <div className="footer-logo">
            <Cpu size={20} color="#2563eb" />
            <span className="footer-logo-text">NexusForge</span>
          </div>
          <p className="footer-copyright">&copy; {new Date().getFullYear()} NexusForge Advanced Agentic IDE. All rights reserved.</p>
        </div>
      </footer>

      <AuthModal isOpen={isAuthModalOpen} onClose={() => setAuthModalOpen(false)} />
    </div>
  );
}
