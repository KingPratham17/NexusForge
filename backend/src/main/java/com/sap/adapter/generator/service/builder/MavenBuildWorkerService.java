package com.sap.adapter.generator.service.builder;

import com.sap.adapter.generator.model.build.BuildJob;
import com.sap.adapter.generator.model.build.InspectionReport;
import com.sap.adapter.generator.service.validator.ArtifactValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MavenBuildWorkerService {

    private static final Logger LOG = LoggerFactory.getLogger(MavenBuildWorkerService.class);
    private final Map<String, BuildJob> activeJobs = new ConcurrentHashMap<>();

    @Autowired
    private ArtifactValidatorService artifactValidatorService;
    
    @Autowired
    private com.sap.adapter.generator.service.validator.PreMavenValidatorService preMavenValidatorService;

    public BuildJob createBuildJob(String id, String adapterName, String workspacePath) {
        String safeId = (id != null && !id.isBlank()) ? id : "build-" + UUID.randomUUID().toString().substring(0, 8);
        String safeName = (adapterName != null && !adapterName.isBlank()) ? adapterName : "CustomAdapter";
        String safePath = (workspacePath != null && !workspacePath.isBlank()) ? workspacePath : "C:/Users/pratham.wadeiyar/.gemini/antigravity-ide/scratch/build-workspaces/" + safeId;

        BuildJob job = new BuildJob(safeId, safeName, safePath);
        activeJobs.put(safeId, job);
        return job;
    }

    public BuildJob getJob(String id) {
        if (id == null) return null;
        return activeJobs.get(id);
    }

    @Async
    public void executeBuild(String jobId, String scheme) {
        BuildJob job = getJob(jobId);
        if (job == null) {
            LOG.error("Job not found for ID: {}", jobId);
            return;
        }

        long startTime = System.currentTimeMillis();
        job.setStatus(BuildJob.Status.COMPILING);
        job.appendLog("=======================================================================");
        job.appendLog("STARTING ISOLATED MAVEN BUILD WORKER FOR ADAPTER: " + job.getAdapterName());
        job.appendLog("Workspace path: " + job.getWorkspacePath());
        job.appendLog("=======================================================================");

        Path workspaceDir = Paths.get(job.getWorkspacePath());

        // --- PRE-MAVEN VALIDATION GATE ---
        java.util.List<String> validationErrors = preMavenValidatorService.validateWorkspace(workspaceDir);
        if (!validationErrors.isEmpty()) {
            job.appendLog("\nPRE-MAVEN VALIDATION FAILED:");
            for (String err : validationErrors) {
                job.appendLog(" - " + err);
            }
            job.appendLog("\nAborting Maven build due to deterministic validation failure.");
            job.setStatus(BuildJob.Status.FAILURE);
            job.setDurationMs(System.currentTimeMillis() - startTime);
            return;
        }

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String mvnCmd = isWindows ? "mvn.cmd" : "mvn";

            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "install", "-DskipTests");
            // removed hardcoded JAVA_HOME
            pb.directory(workspaceDir.toFile());
            pb.redirectErrorStream(true);

            job.appendLog("Executing command: " + mvnCmd + " clean install -DskipTests");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    job.appendLog(line);
                    if (line.contains("BUILD SUCCESS")) {
                        job.setStatus(BuildJob.Status.PACKAGING);
                    } else if (line.contains("COMPILATION ERROR") || line.contains("BUILD FAILURE")) {
                        job.setStatus(BuildJob.Status.FAILURE);
                    }
                }
            }

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                job.setStatus(BuildJob.Status.FAILURE);
                job.appendLog("\nMaven build timed out after 5 minutes. Process forcibly terminated.");
                return;
            }
            int exitCode = process.exitValue();
            job.appendLog("Maven process exited with code: " + exitCode);

            if (exitCode != 0) {
                job.setStatus(BuildJob.Status.FAILURE);
                job.appendLog("\nMaven build failed with exit code " + exitCode + ". Aborting artifact inspection.");
                return;
            }

            job.setStatus(BuildJob.Status.ADK_VALIDATION);
            job.appendLog("\nStarting 12-point SAP ADK Artifact Inspection...");

            try {
                InspectionReport report = artifactValidatorService.inspectArtifacts(workspaceDir, scheme != null ? scheme : "custom-adapter");
                job.setInspectionReport(report);
                job.setStatus(BuildJob.Status.SUCCESS);
                job.appendLog("\n✓ BUILD SUCCESS & ALL ADK ARTIFACT INSPECTION CHECKS PASSED!");
            } catch (Exception e) {
                job.setStatus(BuildJob.Status.FAILURE);
                job.appendLog("\n✗ ADK ARTIFACT INSPECTION CHECKS FAILED: " + e.getMessage());
            }

        } catch (Exception e) {
            LOG.error("Error executing Maven build for job {}: {}", jobId, e.getMessage(), e);
            job.setStatus(BuildJob.Status.FAILURE);
            job.appendLog("EXCEPTION ENCOUNTERED DURING BUILD: " + e.getMessage());
        } finally {
            job.setDurationMs(System.currentTimeMillis() - startTime);
            job.appendLog("Build completed in " + job.getDurationMs() + " ms.");
        }
    }
}
