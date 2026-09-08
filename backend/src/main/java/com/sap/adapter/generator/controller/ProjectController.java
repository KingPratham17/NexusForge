package com.sap.adapter.generator.controller;

import com.sap.adapter.generator.model.entity.Project;
import com.sap.adapter.generator.model.entity.User;
import com.sap.adapter.generator.repository.ProjectRepository;
import com.sap.adapter.generator.repository.UserRepository;
import com.sap.adapter.generator.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> saveProject(@RequestBody Map<String, String> payload) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow();

        String name = payload.get("name");
        String targetSpecJson = payload.get("targetSpecJson");
        String buildId = payload.get("buildId");

        Project project = new Project(name, targetSpecJson, buildId, user);
        projectRepository.save(project);

        return ResponseEntity.ok(Map.of("message", "Project saved successfully", "id", project.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Project project = projectRepository.findById(id).orElseThrow();
        if (!project.getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to delete this project"));
        }
        projectRepository.delete(project);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getUserProjects() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Project> projects = projectRepository.findByUserId(userDetails.getId());
        return ResponseEntity.ok(projects);
    }
}
