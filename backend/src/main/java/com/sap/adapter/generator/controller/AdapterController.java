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
import org.springframework.util.FileSystemUtils;
import com.sap.adapter.generator.repository.ProjectRepository;
import com.sap.adapter.generator.repository.UserRepository;
import com.sap.adapter.generator.model.User;
import com.sap.adapter.generator.model.Project;
import com.sap.adapter.generator.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

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

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) return null;
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    private void checkOwnership(String buildId) {
        User user = getCurrentUser();
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        Project project = projectRepository.findByBuildId(buildId).orElse(null);
        if (project == null || !project.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not authorized to access this build artifact");
        }
    }

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
    public Map<String, Object> generateAdapter(
            @RequestParam("projectId") Long projectId,
            @RequestBody AdapterSpecification spec) throws Exception {
        
        User user = getCurrentUser();
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }

        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Project not found or not authorized"));

        if (project.getBuildId() != null && workspacesRootDir != null) {
            try {
                Path root = Paths.get(workspacesRootDir).normalize().toAbsolutePath();
                Path oldWorkspace = root.resolve(project.getBuildId()).normalize();
                if (oldWorkspace.startsWith(root)) {
                    FileSystemUtils.deleteRecursively(oldWorkspace);
                }
            } catch (Exception e) {
                LOG.warn("Failed to delete old workspace", e);
            }
        }

        String buildId = "build-" + UUID.randomUUID().toString().substring(0, 8);
        Path workspaceDir = Paths.get(workspacesRootDir, buildId);
        Files.createDirectories(workspaceDir);

        Map<String, String> generatedFiles = adapterGeneratorService.generateProjectSources(spec, workspaceDir);

        project.setBuildId(buildId);
        projectRepository.save(project);

        Map<String, Object> response = new HashMap<>();
        response.put("buildId", buildId);
        // Removed workspacePath to prevent absolute path exposure
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
        String adapterName   = request.getOrDefault("adapterName", "CustomAdapter");
        String scheme        = request.getOrDefault("scheme", "custom-adapter");

        checkOwnership(buildId);

        Path resolvedWorkspace = Paths.get(workspacesRootDir, buildId).normalize().toAbsolutePath();
        if (!resolvedWorkspace.startsWith(Paths.get(workspacesRootDir).normalize().toAbsolutePath())) {
            throw new IllegalArgumentException("Invalid buildId: Path traversal detected");
        }
        String workspacePath = resolvedWorkspace.toString();

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
        checkOwnership(buildId);
        String errorLog = (String) request.get("errorLog");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AdapterSpecification spec = mapper.convertValue(request.get("specification"), AdapterSpecification.class);

        Path resolvedWorkspace = Paths.get(workspacesRootDir, buildId).normalize().toAbsolutePath();
        if (!resolvedWorkspace.startsWith(Paths.get(workspacesRootDir).normalize().toAbsolutePath())) {
            throw new IllegalArgumentException("Invalid buildId: Path traversal detected");
        }
        Path workspaceDir = resolvedWorkspace;

        // 1. Call Claude AI to diagnose errorLog and get fixed TargetMeta
        com.sap.adapter.generator.model.spec.TargetMeta fixedTarget = aiRequirementParserService.autoFixRequirement(errorLog, spec, workspaceDir);

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
     * Get generated files from the workspace directory.
     */
    @GetMapping("/adapters/build/{buildId}/files")
    public Map<String, String> getGeneratedFiles(@PathVariable String buildId) throws Exception {
        checkOwnership(buildId);
        Path resolvedWorkspace = Paths.get(workspacesRootDir, buildId).normalize().toAbsolutePath();
        if (!resolvedWorkspace.startsWith(Paths.get(workspacesRootDir).normalize().toAbsolutePath())) {
            throw new IllegalArgumentException("Invalid buildId: Path traversal detected");
        }
        
        Map<String, String> files = new HashMap<>();
        if (Files.exists(resolvedWorkspace)) {
            try (java.util.stream.Stream<Path> stream = Files.walk(resolvedWorkspace)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> !p.toString().contains("target")) // exclude maven target output
                      .forEach(p -> {
                          try {
                              String relPath = resolvedWorkspace.relativize(p).toString().replace('\\', '/');
                              String content = Files.readString(p, StandardCharsets.UTF_8);
                              files.put(relPath, content);
                          } catch (Exception ignored) {}
                      });
            }
        }
        return files;
    }

    /**
     * Poll the status and build logs of a running or completed build job.
     */
    @GetMapping("/adapters/build/{buildId}/status")
    public ResponseEntity<BuildJob> getBuildStatus(@PathVariable String buildId) {
        checkOwnership(buildId);
        BuildJob job = mavenBuildWorkerService.getJob(buildId);
        if (job == null) {
            // Fallback removed to prevent fake SUCCESS status inference
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    /**
     * Download the generated .esa adapter bundle artifact.
     */
    @GetMapping("/adapters/build/{buildId}/download/esa")
    public ResponseEntity<Resource> downloadEsa(@PathVariable String buildId) throws Exception {
        checkOwnership(buildId);
        Path resolvedWorkspace = Paths.get(workspacesRootDir, buildId).normalize().toAbsolutePath();
        if (!resolvedWorkspace.startsWith(Paths.get(workspacesRootDir).normalize().toAbsolutePath())) {
            throw new IllegalArgumentException("Invalid buildId: Path traversal detected");
        }

        File esaFile = null;
        BuildJob job = mavenBuildWorkerService.getJob(buildId);
        
        if (job == null || job.getStatus() != BuildJob.Status.SUCCESS) {
            return ResponseEntity.status(403).build(); // Block ESA download if not SUCCESS
        }
        
        if (job.getInspectionReport() != null && job.getInspectionReport().getEsaPath() != null) {
            esaFile = new File(job.getInspectionReport().getEsaPath());
        }

        if (esaFile == null || !esaFile.exists()) {
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

    /**
     * Download the generated source code as a ZIP archive.
     */
    @GetMapping("/adapters/build/{buildId}/download/source")
    public ResponseEntity<byte[]> downloadSource(@PathVariable String buildId) throws Exception {
        checkOwnership(buildId);
        Path resolvedWorkspace = Paths.get(workspacesRootDir, buildId).normalize().toAbsolutePath();
        if (!resolvedWorkspace.startsWith(Paths.get(workspacesRootDir).normalize().toAbsolutePath())) {
            throw new IllegalArgumentException("Invalid buildId: Path traversal detected");
        }

        if (!Files.exists(resolvedWorkspace)) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos);
             java.util.stream.Stream<Path> stream = Files.walk(resolvedWorkspace)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String relPath = resolvedWorkspace.relativize(p).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(relPath);
                zos.putNextEntry(entry);
                zos.write(Files.readAllBytes(p));
                zos.closeEntry();
            }
        }

        byte[] zipBytes = baos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + buildId + "-source.zip");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipBytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }
}
