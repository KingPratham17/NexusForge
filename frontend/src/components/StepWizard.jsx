import React from 'react';
import { FileText, Sliders, Code2, FolderGit2, Terminal, CheckCheck } from 'lucide-react';

export default function StepWizard({ currentStep, onSelectStep }) {
  const steps = [
    { id: 1, label: '1. Requirement & Target', icon: FileText },
    { id: 2, label: '2. Technology Parameters', icon: Sliders },
    { id: 3, label: '3. Canonical Specification', icon: Code2 },
    { id: 4, label: '4. Source Generator', icon: FolderGit2 },
    { id: 5, label: '5. Build & Validation', icon: Terminal },
    { id: 6, label: '6. Artifact Inspector', icon: CheckCheck }
  ];

  return (
    <div className="glass-panel" style={{ margin: '0 24px 20px 24px', padding: '14px 20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        {steps.map((step, idx) => {
          const Icon = step.icon;
          const isActive = currentStep === step.id;
          const isCompleted = currentStep > step.id;

          return (
            <React.Fragment key={step.id}>
              <div 
                onClick={() => onSelectStep(step.id)}
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '10px', 
                  cursor: 'pointer',
                  opacity: isActive || isCompleted ? 1 : 0.6,
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{
                  width: '34px',
                  height: '34px',
                  borderRadius: '50%',
                  background: isActive 
                    ? 'linear-gradient(135deg, #2563eb, #7c3aed)' 
                    : isCompleted 
                    ? '#ecfdf5' 
                    : '#f1f5f9',
                  border: isCompleted ? '1px solid #10b981' : isActive ? '1px solid #2563eb' : '1px solid var(--border-color)',
                  color: isActive ? 'white' : isCompleted ? '#059669' : '#64748b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '13px',
                  fontWeight: '600'
                }}>
                  <Icon size={16} />
                </div>
                <span style={{ 
                  fontSize: '13.5px', 
                  fontWeight: isActive ? '600' : '500',
                  color: isActive ? '#1d4ed8' : isCompleted ? '#059669' : 'var(--text-secondary)'
                }}>
                  {step.label}
                </span>
              </div>

              {idx < steps.length - 1 && (
                <div style={{ flex: 1, height: '2px', background: isCompleted ? '#a7f3d0' : 'var(--border-color)', margin: '0 12px' }} />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
