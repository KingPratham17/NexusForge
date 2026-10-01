import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import StepWizard from '../components/StepWizard';
import RequirementForm from '../components/RequirementForm';
import DynamicTechForm from '../components/DynamicTechForm';
import SpecificationViewer from '../components/SpecificationViewer';
import SourceCodeViewer from '../components/SourceCodeViewer';
import BuildConsole from '../components/BuildConsole';
import ArtifactInspector from '../components/ArtifactInspector';

import {
  fetchProject,
  createProject,
  updateProject,
  analyzeRequirement,
  generateAdapter,
  triggerBuild,
  fetchBuildStatus,
  fetchGeneratedFiles,
  autoFixBuild
} from '../services/api';

export default function GenerateAdapterPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  
  const [currentStep, setCurrentStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [autoFixing, setAutoFixing] = useState(false);
  const [project, setProject] = useState(null);

  // Workflow state — all driven by AI output
  const [parsedResult, setParsedResult] = useState(null);
  const [specification, setSpecification] = useState(null);
  const [generatedResult, setGeneratedResult] = useState(null);
  const [buildJob, setBuildJob] = useState(null);

  useEffect(() => {
    loadProject();
  }, [projectId]);

  const loadProject = async () => {
    if (projectId === 'new') return; // New project, nothing to load yet
    
    try {
      const data = await fetchProject(projectId);
      setProject(data);
      if (data.currentStep) {
        setCurrentStep(data.currentStep);
      }
      if (data.adapterSpecificationJson) {
        setSpecification(JSON.parse(data.adapterSpecificationJson));
        setParsedResult({ specification: JSON.parse(data.adapterSpecificationJson) });
      }
      if (data.buildId) {
        const job = await fetchBuildStatus(data.buildId);
        setBuildJob(job);
        if (data.currentStep >= 4) { 
          try {
            const files = await fetchGeneratedFiles(data.buildId);
            setGeneratedResult({ workspacePath: job.workspacePath, buildId: data.buildId, generatedFiles: files, filesCount: Object.keys(files).length });
          } catch (e) {
            setGeneratedResult({ workspacePath: job.workspacePath, buildId: data.buildId });
          }
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const saveProjectState = async (step, updates = {}) => {
    setCurrentStep(step);
    try {
      // If it's a new project and this is the first save, create it in the backend
      if (projectId === 'new' && (!project || !project.id)) {
        const p = await createProject({ name: updates.name || 'New Custom Adapter' });
        setProject(p);
        await updateProject(p.id, { currentStep: step, ...updates });
        navigate(`/app/projects/${p.id}/generate`, { replace: true });
      } else {
        const idToUpdate = project?.id || projectId;
        if (idToUpdate !== 'new') {
          await updateProject(idToUpdate, { currentStep: step, ...updates });
        }
      }
    } catch (e) {
      console.error('Failed to save state to project', e);
    }
  };

  // Step 1: Send prompt to AI, no technology pre-selection
  const handleAnalyze = async (prompt, _ignoredTechId, formMeta) => {
    setLoading(true);
    try {
      const res = await analyzeRequirement(prompt, null);

      if (res.error) {
        alert(res.error);
        setLoading(false);
        return;
      }

      if (res?.specification?.adapter) {
        if (formMeta?.vendor) res.specification.adapter.vendor = formMeta.vendor;
        if (formMeta?.version) res.specification.adapter.version = formMeta.version;
      }

      setParsedResult(res);
      setSpecification(res.specification);
      
      const tech = res.specification?.target?.technology || 'Custom';
      const dir = res.specification?.adapter?.direction || 'Sender';
      const adapterName = res.specification?.adapter?.name || 'Custom Adapter';
      
      await saveProjectState(2, { 
        requirement: prompt, 
        adapterSpecificationJson: JSON.stringify(res.specification),
        technology: tech,
        direction: dir,
        name: adapterName
      });
      setProject(prev => prev ? { ...prev, name: adapterName, technology: tech, direction: dir } : prev);
    } catch (err) {
      alert('Error calling AI analyzer: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Step 2: User reviews/edits AI-generated params and clicks Next
  const handleParamsNext = async (updatedSpec) => {
    setSpecification(updatedSpec);
    await saveProjectState(3, { adapterSpecificationJson: JSON.stringify(updatedSpec) });
  };

  // Step 3: Generate project source files from spec
  const handleGenerate = async (finalSpec) => {
    setLoading(true);
    try {
      const res = await generateAdapter(project?.id || projectId, finalSpec);
      setGeneratedResult(res);
      await saveProjectState(4);
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
      await saveProjectState(5, { status: 'IN_PROGRESS' });
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
        await updateProject(projectId, { adapterSpecificationJson: JSON.stringify(res.specification) });
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
          await updateProject(projectId, { status: job.status });
        }
      } catch (err) {
        console.error('Status poll error:', err);
        clearInterval(interval);
      }
    }, 1000);
  };

  const handleReset = async () => {
    setParsedResult(null);
    setSpecification(null);
    setGeneratedResult(null);
    setBuildJob(null);
    setAutoFixing(false);
    await saveProjectState(1, { status: 'DRAFT', adapterSpecificationJson: null });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <div style={{ padding: '24px 24px 0 24px', maxWidth: '1400px', margin: '0 auto', width: '100%' }}>
        <Link to={projectId === 'new' ? '/app' : `/app/projects/${projectId}`} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: 'var(--text-secondary)', textDecoration: 'none', marginBottom: '16px' }}>
          <ArrowLeft size={14} /> {projectId === 'new' ? 'Back to Dashboard' : 'Back to Project Workspace'}
        </Link>
      </div>

      <StepWizard currentStep={currentStep} onSelectStep={(s) => saveProjectState(s)} />

      <div style={{ flex: 1, padding: '0 24px 32px 24px', maxWidth: '1400px', margin: '0 auto', width: '100%' }}>
        {currentStep === 1 && (
          <RequirementForm onAnalyze={handleAnalyze} loading={loading} />
        )}

        {currentStep === 2 && (
          <DynamicTechForm
            parsedResult={parsedResult}
            onNext={handleParamsNext}
            onPrev={() => saveProjectState(1)}
          />
        )}

        {currentStep === 3 && (
          <SpecificationViewer
            specification={specification}
            onGenerate={handleGenerate}
            onPrev={() => saveProjectState(2)}
            loading={loading}
          />
        )}

        {currentStep === 4 && (
          <SourceCodeViewer
            generatedResult={generatedResult}
            onBuild={handleBuild}
            onPrev={() => saveProjectState(3)}
            loading={loading}
          />
        )}

        {currentStep === 5 && (
          <BuildConsole
            buildJob={buildJob}
            onInspect={() => saveProjectState(6, { status: 'SAVED' })}
            onPrev={() => saveProjectState(4)}
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
      </div>
    </div>
  );
}
