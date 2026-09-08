import React, { createContext, useContext, useState } from 'react';
import { fetchBuildStatus } from '../services/api';

export const GeneratorContext = createContext();

export function GeneratorProvider({ children }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [autoFixing, setAutoFixing] = useState(false);
  const [parsedResult, setParsedResult] = useState(null);
  const [specification, setSpecification] = useState(null);
  const [generatedResult, setGeneratedResult] = useState(null);
  const [buildJob, setBuildJob] = useState(null);

  // Method to initialize state when editing an existing project
  const initializeFromProject = async (spec, buildId) => {
    setSpecification(spec);
    setParsedResult({
      specification: spec,
      aiEngine: 'NexusForge ADK Compiler (Loaded from Project)',
      parsedPromptSummary: `Loaded from saved project: ${spec?.adapter?.name || 'Custom Adapter'}`
    });
    
    if (buildId) {
      // If we have a buildId, we assume it's already built or generated
      setGeneratedResult({
        buildId: buildId,
        workspacePath: `(Persisted Workspace)`
      });
      
      try {
        const job = await fetchBuildStatus(buildId);
        setBuildJob(job);
        setCurrentStep(6);
      } catch (err) {
        // If server restarted, job is gone from memory.
        // We set a mock job so the inspector can still show download buttons
        setBuildJob({
          id: buildId,
          status: 'SUCCESS',
          adapterName: spec?.adapter?.name || 'Custom Adapter',
          isArchived: true
        });
        setCurrentStep(6);
      }
    } else {
      setCurrentStep(3); // Just specification reviewing if no build
    }
  };

  const resetGenerator = () => {
    setCurrentStep(1);
    setParsedResult(null);
    setSpecification(null);
    setGeneratedResult(null);
    setBuildJob(null);
    setAutoFixing(false);
  };

  return (
    <GeneratorContext.Provider value={{
      currentStep, setCurrentStep,
      loading, setLoading,
      autoFixing, setAutoFixing,
      parsedResult, setParsedResult,
      specification, setSpecification,
      generatedResult, setGeneratedResult,
      buildJob, setBuildJob,
      initializeFromProject,
      resetGenerator
    }}>
      {children}
    </GeneratorContext.Provider>
  );
}

export function useGenerator() {
  return useContext(GeneratorContext);
}
