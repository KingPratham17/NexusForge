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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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
                    if (updates.getBuildId() != null) existing.setBuildId(updates.getBuildId());
                    
                    return ResponseEntity.ok(projectRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
