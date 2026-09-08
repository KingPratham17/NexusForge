import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import StepWizard from '../components/StepWizard';
import RequirementForm from '../components/RequirementForm';
import DynamicTechForm from '../components/DynamicTechForm';
import SpecificationViewer from '../components/SpecificationViewer';
import SourceCodeViewer from '../components/SourceCodeViewer';
import BuildConsole from '../components/BuildConsole';
import ArtifactInspector from '../components/ArtifactInspector';
import { useGenerator } from '../context/GeneratorContext';

import {
  analyzeRequirement,
  generateAdapter,
  triggerBuild,
  fetchBuildStatus,
  autoFixBuild
} from '../services/api';

export default function Generator() {
  const location = useLocation();
  const navigate = useNavigate();

  const {
    currentStep, setCurrentStep,
    loading, setLoading,
    autoFixing, setAutoFixing,
    parsedResult, setParsedResult,
    specification, setSpecification,
    generatedResult, setGeneratedResult,
    buildJob, setBuildJob,
    initializeFromProject,
    resetGenerator
  } = useGenerator();

  // If navigated from Dashboard "Edit", pick up editSpec
  useEffect(() => {
    if (location.state?.editSpec) {
      const spec = location.state.editSpec;
      const buildId = location.state.buildId;
      initializeFromProject(spec, buildId);
      // Clear location state so we don't re-trigger on refresh
      navigate('/generator', { replace: true, state: {} });
    }
  }, [location.state?.editSpec]);

  const handleAnalyze = async (prompt, _ignoredTechId, formMeta) => {
    setLoading(true);
    try {
      const res = await analyzeRequirement(prompt, null);

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

  const handleParamsNext = (updatedSpec) => {
    setSpecification(updatedSpec);
    setCurrentStep(3);
  };

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

  const handleAutoFix = async () => {
    const buildId = generatedResult?.buildId || buildJob?.id;
    if (!buildId || !buildJob) {
      alert("No active build job found to auto-fix.");
      return;
    }

    setAutoFixing(true);
    setBuildJob((prev) => ({
      ...prev,
      status: 'COMPILING',
      buildLogs: (prev?.buildLogs || '') + '\n\n⚡ [NexusForge AI Auto-Fix] Diagnosing Maven build errors and auto-repairing specification...\n'
    }));

    try {
      const res = await autoFixBuild(
        buildId,
        buildJob.buildLogs,
        specification
      );
      if (res.specification) {
        setSpecification(res.specification);
      }
      if (res.generatedFiles) {
        setGeneratedResult((prev) => ({
          ...(prev || {}),
          buildId: buildId,
          generatedFiles: res.generatedFiles,
        }));
      }
      pollBuildStatus(buildId);
    } catch (err) {
      alert('AI Auto-Fix error: ' + (err.message || 'Unknown error'));
    } finally {
      setAutoFixing(false);
    }
  };

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
    resetGenerator();
    navigate('/generator', { replace: true, state: {} }); // Reset navigation state
  };

  return (
    <>
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
            specification={specification}
            onReset={handleReset}
          />
        )}
      </main>
    </>
  );
}
