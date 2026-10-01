package com.sap.adapter.generator;

import com.sap.adapter.generator.model.build.InspectionReport;
import com.sap.adapter.generator.service.validator.ArtifactValidatorService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class Check6Test {

    @Autowired
    private ArtifactValidatorService artifactValidatorService;

    @Value("${adapter.generator.workspaces-dir:../../build-workspaces}")
    private String workspacesDir;

    @Test
    public void testCheck6_GitHub() {
        Path workspace = Paths.get(workspacesDir, "build-github-sender").toAbsolutePath();
        
        Assumptions.assumeTrue(Files.exists(workspace), "Skipping test: GitHub workspace not found at " + workspace);

        InspectionReport report = artifactValidatorService.inspectArtifacts(workspace, "github-issues-custom");
        
        System.out.println("========== CASE A (GitHub) ==========");
        System.out.println("Check 6 passed: " + report.getCheckItems().get(5).isPassed());
        System.out.println("Check 6 detail: " + report.getCheckItems().get(5).getDetails());
        System.out.println("Overall Score: " + report.getPassedChecks() + " / " + report.getTotalChecks());
        
        assertTrue(report.getCheckItems().get(5).isPassed(), "Check 6 should pass for GitHub adapter");
    }

    @Test
    public void testCheck6_Firebase() {
        // Firebase is typically located adjacent to build-workspaces in the scratch directory
        Path workspace = Paths.get(workspacesDir).getParent()
                .resolve("firebase-firestore-adapter")
                .resolve("firebase-firestore-adapter")
                .toAbsolutePath();
        
        Assumptions.assumeTrue(Files.exists(workspace), "Skipping test: Firebase workspace not found at " + workspace);

        InspectionReport report = artifactValidatorService.inspectArtifacts(workspace, "firebase-firestore");
        
        System.out.println("========== CASE B (Firebase) ==========");
        System.out.println("Check 6 passed: " + report.getCheckItems().get(5).isPassed());
        System.out.println("Check 6 detail: " + report.getCheckItems().get(5).getDetails());
        
        assertTrue(report.getCheckItems().get(5).isPassed(), "Check 6 should pass for Firebase adapter");
    }
}
