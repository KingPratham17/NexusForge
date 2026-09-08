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

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String mvnCmd = isWindows ? "mvn.cmd" : "mvn";

            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "install", "-DskipTests", "-U");
            pb.directory(workspaceDir.toFile());
            pb.redirectErrorStream(true);

            job.appendLog("Executing command: " + mvnCmd + " clean install -DskipTests -U");
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

            int exitCode = process.waitFor();
            job.appendLog("Maven process exited with code: " + exitCode);

            job.setStatus(BuildJob.Status.ADK_VALIDATION);
            job.appendLog("\nStarting 12-point SAP ADK Artifact Inspection...");

            InspectionReport report = artifactValidatorService.inspectArtifacts(workspaceDir, scheme != null ? scheme : "custom-adapter");
            job.setInspectionReport(report);

            if (exitCode == 0 && report != null && report.isOverallSuccess()) {
                job.setStatus(BuildJob.Status.SUCCESS);
                job.appendLog("\n✓ BUILD SUCCESS & ALL ADK ARTIFACT INSPECTION CHECKS PASSED!");
            } else {
                job.setStatus(BuildJob.Status.FAILURE);
                job.appendLog("\n✗ BUILD FAILURE OR ADK ARTIFACT INSPECTION CHECKS FAILED.");
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
