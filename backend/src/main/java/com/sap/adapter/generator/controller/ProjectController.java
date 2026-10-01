package com.sap.adapter.generator.controller;

import com.sap.adapter.generator.model.Project;
import com.sap.adapter.generator.model.User;
import com.sap.adapter.generator.repository.ProjectRepository;
import com.sap.adapter.generator.repository.UserRepository;
import com.sap.adapter.generator.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileSystemUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Value("${adapter.generator.workspaces-dir:C:/Users/pratham.wadeiyar/.gemini/antigravity-ide/scratch/build-workspaces}")
    private String workspacesRootDir;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) return null;
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getUserProjects() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(projectRepository.findByUserIdOrderByLastUpdatedDesc(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project projectReq) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Project project = new Project();
        project.setName(projectReq.getName() != null ? projectReq.getName() : "Untitled Project");
        project.setUser(user);
        project.setCurrentStep(1);
        project.setStatus("DRAFT");
        
        return ResponseEntity.ok(projectRepository.save(project));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        return projectRepository.findById(id)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @RequestBody Project updates) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        return projectRepository.findById(id)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .map(existing -> {
                    if (updates.getName() != null) existing.setName(updates.getName());
                    if (updates.getTechnology() != null) existing.setTechnology(updates.getTechnology());
                    if (updates.getDirection() != null) existing.setDirection(updates.getDirection());
                    if (updates.getCurrentStep() != null) existing.setCurrentStep(updates.getCurrentStep());
                    if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
                    if (updates.getRequirement() != null) existing.setRequirement(updates.getRequirement());
                    if (updates.getAdapterSpecificationJson() != null) existing.setAdapterSpecificationJson(updates.getAdapterSpecificationJson());
                    
                    return ResponseEntity.ok(projectRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        return projectRepository.findById(id)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .map(existing -> {
                    if (existing.getBuildId() != null && workspacesRootDir != null) {
                        try {
                            Path root = Paths.get(workspacesRootDir).normalize().toAbsolutePath();
                            Path workspacePath = root.resolve(existing.getBuildId()).normalize();
                            if (workspacePath.startsWith(root)) {
                                FileSystemUtils.deleteRecursively(workspacePath);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    projectRepository.delete(existing);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
