package com.sap.adapter.generator.controller;

import com.sap.adapter.generator.model.build.BuildJob;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.service.analyzer.AiRequirementParserService;
import com.sap.adapter.generator.service.builder.MavenBuildWorkerService;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class AdapterController {

    private static final Logger LOG = LoggerFactory.getLogger(AdapterController.class);

    @Value("${adapter.generator.workspaces-dir:C:/Users/pratham.wadeiyar/.gemini/antigravity-ide/scratch/build-workspaces}")
    private String workspacesRootDir;

    @Autowired
    private AiRequirementParserService aiRequirementParserService;

    @Autowired
    private AdapterGeneratorService adapterGeneratorService;

    @Autowired
    private MavenBuildWorkerService mavenBuildWorkerService;

    /**
     * Analyze a free-form natural language requirement using Claude AI.
     * Returns the AI-generated AdapterSpecification — no hardcoded technology list.
     */
    @PostMapping("/requirements/analyze")
    public Map<String, Object> analyzeRequirement(@RequestBody Map<String, String> request) {
        String prompt = request.getOrDefault("prompt", "");
        String technologyId = request.get("technologyId"); // optional hint, can be null
        return aiRequirementParserService.parseRequirement(prompt, technologyId);
    }

    /**
     * Generate the Maven project files from an AI-produced AdapterSpecification.
     */
    @PostMapping("/adapters/generate")
    public Map<String, Object> generateAdapter(@RequestBody AdapterSpecification spec) throws Exception {
        String buildId = "build-" + UUID.randomUUID().toString().substring(0, 8);
        Path workspaceDir = Paths.get(workspacesRootDir, buildId);
        Files.createDirectories(workspaceDir);

        Map<String, String> generatedFiles = adapterGeneratorService.generateProjectSources(spec, workspaceDir);

        Map<String, Object> response = new HashMap<>();
        response.put("buildId", buildId);
        response.put("workspacePath", workspaceDir.toAbsolutePath().toString());
        response.put("filesCount", generatedFiles.size());
        response.put("generatedFiles", generatedFiles);
        response.put("specification", spec);
        return response;
    }

    /**
     * Trigger an isolated Maven build for the generated adapter project.
     */
    @PostMapping("/adapters/build")
    public Map<String, Object> triggerBuild(@RequestBody Map<String, String> request) {
        String buildId    = request.getOrDefault("buildId", "build-unknown");
        String workspacePath = request.getOrDefault("workspacePath", "");
        String adapterName   = request.getOrDefault("adapterName", "CustomAdapter");
        String scheme        = request.getOrDefault("scheme", "custom-adapter"); // always from spec, never hardcoded

        BuildJob job = mavenBuildWorkerService.createBuildJob(buildId, adapterName, workspacePath);
        mavenBuildWorkerService.executeBuild(buildId, scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("buildId", buildId);
        response.put("status", job.getStatus());
        response.put("message", "Maven build job started for adapter: " + adapterName);
        return response;
    }

    /**
     * AI Auto-Fix Endpoint: Sends Maven build logs + current spec to Claude AI to heal code errors,
     * re-generate workspace source files, and re-trigger Maven build.
     */
    @PostMapping("/adapters/autofix")
    public Map<String, Object> autoFixBuild(@RequestBody Map<String, Object> request) throws Exception {
        String buildId = (String) request.get("buildId");
        String errorLog = (String) request.get("errorLog");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AdapterSpecification spec = mapper.convertValue(request.get("specification"), AdapterSpecification.class);

        Path workspaceDir = Paths.get(workspacesRootDir, buildId);

        // 1. Call Claude AI to diagnose errorLog and get fixed TargetMeta
        com.sap.adapter.generator.model.spec.TargetMeta fixedTarget = aiRequirementParserService.autoFixRequirement(errorLog, spec);

        if (fixedTarget != null && spec != null) {
            spec.setTarget(fixedTarget);
        }

        // 2. Re-generate project files in workspace
        Map<String, String> generatedFiles = adapterGeneratorService.generateProjectSources(spec, workspaceDir);

        // 3. Re-trigger build
        String adapterName = spec != null && spec.getAdapter() != null ? spec.getAdapter().getName() : "CustomAdapter";
        String scheme = spec != null && spec.getAdapter() != null ? spec.getAdapter().getScheme() : "custom-adapter";

        BuildJob job = mavenBuildWorkerService.createBuildJob(buildId, adapterName, workspaceDir.toString());
        mavenBuildWorkerService.executeBuild(buildId, scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("buildId", buildId);
        response.put("status", job.getStatus());
        response.put("message", "AI auto-fixed code and re-triggered Maven build for: " + adapterName);
        response.put("generatedFiles", generatedFiles);
        response.put("specification", spec);
        return response;
    }

    /**
     * Poll the status and build logs of a running or completed build job.
     */
    @GetMapping("/adapters/build/{buildId}/status")
    public ResponseEntity<BuildJob> getBuildStatus(@PathVariable String buildId) {
        BuildJob job = mavenBuildWorkerService.getJob(buildId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    /**
     * Download the generated .esa adapter bundle artifact.
     */
    @GetMapping("/adapters/build/{buildId}/download/esa")
    public ResponseEntity<Resource> downloadEsa(@PathVariable String buildId) {
        BuildJob job = mavenBuildWorkerService.getJob(buildId);
        if (job == null || job.getInspectionReport() == null || job.getInspectionReport().getEsaPath() == null) {
            return ResponseEntity.notFound().build();
        }

        File esaFile = new File(job.getInspectionReport().getEsaPath());
        if (!esaFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + esaFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(esaFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(esaFile));
    }
}
