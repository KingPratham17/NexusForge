import React, { useState } from 'react';
import Header from './components/Header';
import StepWizard from './components/StepWizard';
import RequirementForm from './components/RequirementForm';
import DynamicTechForm from './components/DynamicTechForm';
import SpecificationViewer from './components/SpecificationViewer';
import SourceCodeViewer from './components/SourceCodeViewer';
import BuildConsole from './components/BuildConsole';
import ArtifactInspector from './components/ArtifactInspector';

import {
  analyzeRequirement,
  generateAdapter,
  triggerBuild,
  fetchBuildStatus,
  autoFixBuild
} from './services/api';

export default function App() {
  const [currentStep, setCurrentStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [autoFixing, setAutoFixing] = useState(false);

  // Workflow state — all driven by AI output
  const [parsedResult, setParsedResult] = useState(null);
  const [specification, setSpecification] = useState(null);
  const [generatedResult, setGeneratedResult] = useState(null);
  const [buildJob, setBuildJob] = useState(null);

  // Step 1: Send prompt to AI, no technology pre-selection
  const handleAnalyze = async (prompt, _ignoredTechId, formMeta) => {
    setLoading(true);
    try {
      const res = await analyzeRequirement(prompt, null);

      // Allow optional user overrides for vendor/version from the form
      if (res?.specification?.adapter) {
        if (formMeta?.vendor) res.specification.adapter.vendor = formMeta.vendor;
        if (formMeta?.version) res.specification.adapter.version = formMeta.version;
      }

      setParsedResult(res);
      setSpecification(res.specification);
      setCurrentStep(2);
    } catch (err) {
      alert('Error calling AI analyzer: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Step 2: User reviews/edits AI-generated params and clicks Next
  const handleParamsNext = (updatedSpec) => {
    setSpecification(updatedSpec);
    setCurrentStep(3);
  };

  // Step 3: Generate project source files from spec
  const handleGenerate = async (finalSpec) => {
    setLoading(true);
    try {
      const res = await generateAdapter(finalSpec);
      setGeneratedResult(res);
      setCurrentStep(4);
    } catch (err) {
      alert('Error generating adapter project: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Step 4: Trigger isolated Maven build
  const handleBuild = async () => {
    if (!generatedResult) return;
    setLoading(true);
    try {
      const { buildId, workspacePath } = generatedResult;
      const adapterName = specification?.adapter?.name || 'CustomAdapter';
      const scheme      = specification?.adapter?.scheme || 'custom-adapter';

      await triggerBuild(buildId, workspacePath, adapterName, scheme);
      setCurrentStep(5);
      pollBuildStatus(buildId);
    } catch (err) {
      alert('Error triggering build: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // AI Auto-Fix & Re-Run handler
  const handleAutoFix = async () => {
    if (!generatedResult || !buildJob) return;
    setAutoFixing(true);
    try {
      const res = await autoFixBuild(
        generatedResult.buildId,
        buildJob.buildLogs,
        specification
      );
      if (res.specification) {
        setSpecification(res.specification);
      }
      if (res.generatedFiles) {
        setGeneratedResult((prev) => ({
          ...prev,
          generatedFiles: res.generatedFiles,
        }));
      }
      pollBuildStatus(generatedResult.buildId);
    } catch (err) {
      alert('AI Auto-Fix error: ' + (err.message || 'Unknown error'));
    } finally {
      setAutoFixing(false);
    }
  };

  // Poll build status every second
  const pollBuildStatus = (buildId) => {
    const interval = setInterval(async () => {
      try {
        const job = await fetchBuildStatus(buildId);
        setBuildJob(job);
        if (job.status === 'SUCCESS' || job.status === 'FAILURE') {
          clearInterval(interval);
        }
      } catch (err) {
        console.error('Status poll error:', err);
        clearInterval(interval);
      }
    }, 1000);
  };

  const handleReset = () => {
    setCurrentStep(1);
    setParsedResult(null);
    setSpecification(null);
    setGeneratedResult(null);
    setBuildJob(null);
    setAutoFixing(false);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Header />
      <StepWizard currentStep={currentStep} onSelectStep={(s) => setCurrentStep(s)} />

      <main style={{ flex: 1, padding: '0 24px 32px 24px' }}>
        {currentStep === 1 && (
          <RequirementForm onAnalyze={handleAnalyze} loading={loading} />
        )}

        {currentStep === 2 && (
          <DynamicTechForm
            parsedResult={parsedResult}
            onNext={handleParamsNext}
            onPrev={() => setCurrentStep(1)}
          />
        )}

        {currentStep === 3 && (
          <SpecificationViewer
            specification={specification}
            onGenerate={handleGenerate}
            onPrev={() => setCurrentStep(2)}
            loading={loading}
          />
        )}

        {currentStep === 4 && (
          <SourceCodeViewer
            generatedResult={generatedResult}
            onBuild={handleBuild}
            onPrev={() => setCurrentStep(3)}
            loading={loading}
          />
        )}

        {currentStep === 5 && (
          <BuildConsole
            buildJob={buildJob}
            onInspect={() => setCurrentStep(6)}
            onPrev={() => setCurrentStep(4)}
            onReRun={handleBuild}
            onAutoFix={handleAutoFix}
            autoFixing={autoFixing}
          />
        )}

        {currentStep === 6 && (
          <ArtifactInspector
            buildJob={buildJob}
            onReset={handleReset}
          />
        )}
      </main>
    </div>
  );
}
