import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Cpu, Zap, Layers, ShieldCheck, Download, Code, Database, Globe, CheckCircle2, Server, Key } from 'lucide-react';

export default function LandingPage() {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      
      {/* Hero Section */}
      <section style={{ 
        position: 'relative',
        padding: '120px 24px 80px 24px', 
        textAlign: 'center',
        background: '#ffffff',
        overflow: 'hidden'
      }}>
        {/* Background Gradients */}
        <div style={{ position: 'absolute', top: '-10%', left: '50%', transform: 'translateX(-50%)', width: '80vw', height: '80vh', background: 'radial-gradient(ellipse at top, rgba(37, 99, 235, 0.08), transparent 70%)', zIndex: 0, pointerEvents: 'none' }} />
        <div style={{ position: 'absolute', top: '20%', right: '-10%', width: '40vw', height: '40vw', background: 'radial-gradient(circle, rgba(124, 58, 237, 0.05), transparent 60%)', zIndex: 0, pointerEvents: 'none' }} />
        
        <div style={{ position: 'relative', zIndex: 10, maxWidth: '900px', margin: '0 auto' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '6px 16px', background: 'rgba(37, 99, 235, 0.1)', color: '#1d4ed8', border: '1px solid rgba(37, 99, 235, 0.2)', borderRadius: '24px', fontSize: '13px', fontWeight: '600', marginBottom: '32px' }}>
            <Zap size={14} fill="#1d4ed8" /> AI-Assisted Integration Platform v2.0
          </div>
          
          <h1 style={{ fontSize: '64px', fontWeight: '800', color: '#0f172a', letterSpacing: '-0.03em', lineHeight: '1.1', marginBottom: '24px' }}>
            Generate Custom SAP Adapters at <span style={{ background: 'linear-gradient(135deg, #2563eb, #7c3aed)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Light Speed</span>
          </h1>
          
          <p style={{ fontSize: '20px', color: '#475569', lineHeight: '1.6', marginBottom: '48px', maxWidth: '700px', margin: '0 auto 48px auto' }}>
            Describe your connectivity requirement in plain text. NexusForge dynamically generates, validates, builds, and outputs an Enterprise Service Archive (.esa) ready for immediate deployment to SAP Cloud Integration.
          </p>
          
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
            <Link to="/signup" className="btn-primary" style={{ padding: '16px 32px', fontSize: '16px', fontWeight: '600', borderRadius: '12px' }}>
              Start Building Free <ArrowRight size={18} />
            </Link>
          </div>
        </div>

        {/* Floating Mock UI */}
        <div style={{ position: 'relative', zIndex: 10, maxWidth: '1000px', margin: '80px auto 0 auto', perspective: '1000px' }}>
          <div style={{ 
            background: 'rgba(255, 255, 255, 0.9)', 
            border: '1px solid rgba(226, 232, 240, 0.8)', 
            borderRadius: '16px', 
            boxShadow: '0 25px 50px -12px rgba(15, 23, 42, 0.15), 0 0 0 1px rgba(15, 23, 42, 0.02)',
            overflow: 'hidden',
            transform: 'rotateX(2deg) scale(1)',
            transformOrigin: 'top center'
          }}>
            <div style={{ height: '48px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', padding: '0 16px', gap: '8px' }}>
              <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#ef4444' }} />
              <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#eab308' }} />
              <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#22c55e' }} />
              <div style={{ flex: 1, textAlign: 'center', fontSize: '12px', color: '#64748b', fontWeight: '500', display: 'flex', justifyContent: 'center' }}>
                <div style={{ background: '#e2e8f0', padding: '4px 120px', borderRadius: '6px' }}>nexusforge.dev/workspace</div>
              </div>
            </div>
            <div style={{ padding: '32px', display: 'grid', gridTemplateColumns: '250px 1fr', gap: '32px', background: '#ffffff', textAlign: 'left' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ height: '24px', width: '100%', background: '#f1f5f9', borderRadius: '4px' }} />
                <div style={{ height: '24px', width: '80%', background: '#f1f5f9', borderRadius: '4px' }} />
                <div style={{ height: '24px', width: '90%', background: '#f1f5f9', borderRadius: '4px' }} />
                <div style={{ height: '24px', width: '60%', background: '#f1f5f9', borderRadius: '4px' }} />
              </div>
              <div style={{ background: '#0f172a', borderRadius: '8px', padding: '24px', color: '#e2e8f0', fontFamily: 'var(--font-mono)', fontSize: '13px', lineHeight: '1.6' }}>
                <span style={{ color: '#818cf8' }}>const</span> specification = {'{'}
                <br />  <span style={{ color: '#a5b4fc' }}>name</span>: <span style={{ color: '#34d399' }}>'KafkaSender'</span>,
                <br />  <span style={{ color: '#a5b4fc' }}>direction</span>: <span style={{ color: '#34d399' }}>'Sender'</span>,
                <br />  <span style={{ color: '#a5b4fc' }}>parameters</span>: [
                <br />    {'{'} <span style={{ color: '#a5b4fc' }}>name</span>: <span style={{ color: '#34d399' }}>'bootstrapServers'</span>, <span style={{ color: '#a5b4fc' }}>type</span>: <span style={{ color: '#34d399' }}>'String'</span> {'}'},
                <br />    {'{'} <span style={{ color: '#a5b4fc' }}>name</span>: <span style={{ color: '#34d399' }}>'topicName'</span>, <span style={{ color: '#a5b4fc' }}>type</span>: <span style={{ color: '#34d399' }}>'String'</span> {'}'}
                <br />  ]
                <br />{'}'};
                <br /><br />
                <span style={{ color: '#f472b6' }}>[BUILD SUCCESS]</span> Enterprise Service Archive created.
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section style={{ padding: '100px 24px', background: '#f8fafc' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '64px' }}>
            <h2 style={{ fontSize: '36px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>Everything you need to build integrations</h2>
            <p style={{ fontSize: '18px', color: '#64748b', maxWidth: '600px', margin: '0 auto' }}>No more boilerplate. NexusForge handles the complex ADK requirements so you can focus on connectivity.</p>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '32px' }}>
            
            <FeatureCard 
              icon={<Cpu size={24} color="#2563eb" />}
              title="AI Generation Engine"
              desc="Claude AI analyzes your target system and generates a strictly validated AdapterSpecification and raw Java sources."
            />
            
            <FeatureCard 
              icon={<Layers size={24} color="#7c3aed" />}
              title="Isolated Maven Assembly"
              desc="The generated project is assembled using Apache Maven with strict dependency controls and isolated JVM environments."
            />
            
            <FeatureCard 
              icon={<ShieldCheck size={24} color="#059669" />}
              title="SAP ADK Validation"
              desc="12-point artifact inspection enforces zero-raw-credentials policy, alias support, and OSGi bundle compliance."
            />
            
            <FeatureCard 
              icon={<Download size={24} color="#d97706" />}
              title="Deployable ESA Artifact"
              desc="A clean, secure `.esa` file is produced, ready for direct, friction-free deployment to SAP Cloud Integration."
            />
            
            <FeatureCard 
              icon={<Code size={24} color="#e11d48" />}
              title="Source Code Access"
              desc="Full access to the raw generated Java sources and Maven POMs. Download the source archive to customize further."
            />
            
            <FeatureCard 
              icon={<Server size={24} color="#0891b2" />}
              title="Stateful Projects Workspace"
              desc="Save your adapter specifications, track build history, and regenerate components as your requirements evolve."
            />

          </div>
        </div>
      </section>

      {/* Trust & Security Section */}
      <section style={{ padding: '100px 24px', background: '#ffffff' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', alignItems: 'center', gap: '64px', flexWrap: 'wrap' }}>
          <div style={{ flex: '1 1 400px' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: '#059669', fontWeight: '600', marginBottom: '16px' }}>
              <ShieldCheck size={20} /> Enterprise Grade Security
            </div>
            <h2 style={{ fontSize: '36px', fontWeight: '700', color: '#0f172a', marginBottom: '24px', lineHeight: '1.2' }}>Built for the strict constraints of SAP CI.</h2>
            <p style={{ fontSize: '18px', color: '#64748b', marginBottom: '32px', lineHeight: '1.6' }}>
              NexusForge doesn't just write code; it enforces SAP's security guidelines automatically. The AI is constrained by strict post-generation verification rules.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <CheckItem text="Zero raw credentials in adapter UI" />
              <CheckItem text="Automatic Secure Alias mapping" />
              <CheckItem text="OSGi MANIFEST.MF compliance" />
              <CheckItem text="Dynamic parameter UI rendering constraints" />
            </div>
          </div>
          
          <div style={{ flex: '1 1 500px', position: 'relative' }}>
            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '24px', padding: '40px', position: 'relative', zIndex: 10 }}>
              <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', alignItems: 'center' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: '#e0e7ff', color: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Key size={24} />
                </div>
                <div>
                  <h4 style={{ fontWeight: '600', color: '#1e293b' }}>Credential Validation</h4>
                  <p style={{ fontSize: '14px', color: '#64748b' }}>Post-build security check passed</p>
                </div>
              </div>
              <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '14px', fontWeight: '500', color: '#334155' }}>UsernameField</span>
                  <span className="badge badge-red">Rejected (Raw)</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '14px', fontWeight: '500', color: '#334155' }}>CredentialAlias</span>
                  <span className="badge badge-green">Accepted (Secure)</span>
                </div>
              </div>
            </div>
            <div style={{ position: 'absolute', top: '20px', left: '-20px', right: '20px', bottom: '-20px', background: '#eff6ff', borderRadius: '24px', zIndex: 0 }} />
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section style={{ padding: '80px 24px', background: '#0f172a', textAlign: 'center' }}>
        <div style={{ maxWidth: '800px', margin: '0 auto' }}>
          <h2 style={{ fontSize: '40px', fontWeight: '700', color: '#ffffff', marginBottom: '24px' }}>Ready to forge your next adapter?</h2>
          <p style={{ fontSize: '18px', color: '#94a3b8', marginBottom: '40px' }}>Stop wrestling with the ADK structure. Let AI handle the boilerplate.</p>
          <Link to="/signup" className="btn-primary" style={{ padding: '16px 32px', fontSize: '18px', borderRadius: '12px', background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)', border: 'none' }}>
            Get Started for Free
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer style={{ background: '#020617', padding: '40px 24px', color: '#64748b', fontSize: '14px' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: '32px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f8fafc', fontWeight: '700', fontSize: '16px', marginBottom: '16px' }}>
              <Layers size={18} /> NexusForge
            </div>
            <p style={{ maxWidth: '300px', lineHeight: '1.6' }}>The intelligent integration platform for modern SAP developers.</p>
          </div>
          
          <div style={{ display: 'flex', gap: '64px' }}>
            <div>
              <h4 style={{ color: '#f8fafc', fontWeight: '600', marginBottom: '16px' }}>Product</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <Link to="/" style={{ color: '#94a3b8', textDecoration: 'none' }}>Features</Link>
                <Link to="/" style={{ color: '#94a3b8', textDecoration: 'none' }}>Documentation</Link>
                <Link to="/" style={{ color: '#94a3b8', textDecoration: 'none' }}>Security</Link>
              </div>
            </div>
            <div>
              <h4 style={{ color: '#f8fafc', fontWeight: '600', marginBottom: '16px' }}>Legal</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <Link to="/" style={{ color: '#94a3b8', textDecoration: 'none' }}>Privacy Policy</Link>
                <Link to="/" style={{ color: '#94a3b8', textDecoration: 'none' }}>Terms of Service</Link>
              </div>
            </div>
          </div>
        </div>
        <div style={{ maxWidth: '1200px', margin: '40px auto 0 auto', borderTop: '1px solid #1e293b', paddingTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>© 2026 NexusForge. All rights reserved.</span>
          <span>Version 2.0</span>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, title, desc }) {
  return (
    <div className="glass-card" style={{ padding: '32px', display: 'flex', flexDirection: 'column', gap: '16px', height: '100%' }}>
      <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {icon}
      </div>
      <h3 style={{ fontSize: '20px', fontWeight: '600', color: '#0f172a' }}>{title}</h3>
      <p style={{ fontSize: '15px', color: '#64748b', lineHeight: '1.6' }}>{desc}</p>
    </div>
  );
}

function CheckItem({ text }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '16px', color: '#334155' }}>
      <CheckCircle2 size={20} color="#059669" />
      {text}
    </div>
  );
}
