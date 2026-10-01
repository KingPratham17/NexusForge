package com.sap.adapter.generator;

import com.sap.adapter.generator.service.validator.PreMavenValidatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PreMavenValidatorTest {

    private PreMavenValidatorService validator;
    private Path workspaceDir;

    @BeforeEach
    public void setUp() throws IOException {
        validator = new PreMavenValidatorService();
        workspaceDir = Files.createTempDirectory("nexusforge-test-workspace");
        // Create base dirs
        Files.createDirectories(workspaceDir.resolve("metadata"));
        Files.createDirectories(workspaceDir.resolve("OSGI-INF"));
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.walk(workspaceDir)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    private void setupValidUniversalFiles() throws IOException {
        Files.writeString(workspaceDir.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion></project>");
        Files.writeString(workspaceDir.resolve("metadata/metadata.xml"), "<metadata/>");
        Files.writeString(workspaceDir.resolve("OSGI-INF/SUBSYSTEM.MF"), "Manifest-Version: 1.0\n");
    }

    @Test
    public void testValidProject_Passes() throws IOException {
        setupValidUniversalFiles();
        
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertTrue(errors.isEmpty(), "Valid project should have no errors, got: " + errors);
    }

    @Test
    public void testMissingUniversalFile_Fails() throws IOException {
        // Only pom.xml and metadata are present, SUBSYSTEM.MF is missing
        Files.writeString(workspaceDir.resolve("pom.xml"), "<project/>");
        Files.writeString(workspaceDir.resolve("metadata/metadata.xml"), "<metadata/>");
        
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Missing universally required file: OSGI-INF/SUBSYSTEM.MF"));
    }

    @Test
    public void testUnresolvedNexusForgePlaceholder_Fails() throws IOException {
        setupValidUniversalFiles();
        Files.writeString(workspaceDir.resolve("pom.xml"), "<project><name>${adapterName}</name></project>");
        
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertFalse(errors.isEmpty(), "Should fail on unresolved NexusForge placeholder");
        assertTrue(errors.get(0).contains("Unresolved NexusForge template placeholder detected: ${adapterName}"));
    }

    @Test
    public void testValidMavenPlaceholder_Passes() throws IOException {
        setupValidUniversalFiles();
        Files.writeString(workspaceDir.resolve("pom.xml"), "<project><name>${project.version}</name><ver>${camel.version}</ver></project>");
        
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertTrue(errors.isEmpty(), "Valid Maven placeholders should not cause errors, got: " + errors);
    }

    @Test
    public void testMalformedXml_Fails() throws IOException {
        setupValidUniversalFiles();
        // Overwrite with malformed XML
        Files.writeString(workspaceDir.resolve("pom.xml"), "<project><name>test</project>"); // missing </name>
        
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Malformed XML detected in pom.xml")));
    }

    @Test
    public void testSecurityViolation_Fails() throws IOException {
        setupValidUniversalFiles();
        Files.writeString(workspaceDir.resolve("MyComponent.java"), 
            "public class MyComponent { private String x = \"apiKey=12345\"; }");
            
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Security violation: Hardcoded credentials detected")));
    }

    @Test
    public void testValidStubImplementation_Passes() throws IOException {
        setupValidUniversalFiles();
        Files.writeString(workspaceDir.resolve("MyProducer.java"), 
            "public class MyProducer { public void process() { LOG.info(\"Processing message\"); } }");
            
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertTrue(errors.isEmpty(), "Stub implementation should pass validation");
    }

    @Test
    public void testPluginTemplateScenario_Passes() throws IOException {
        setupValidUniversalFiles();
        // A plugin might not generate Component.java or Endpoint.java.
        // It generates a custom file instead.
        Files.writeString(workspaceDir.resolve("MyCustomRoute.java"), 
            "public class MyCustomRoute {}");
            
        List<String> errors = validator.validateWorkspace(workspaceDir);
        assertTrue(errors.isEmpty(), "Plugin scenario with valid universal files and no Component/Endpoint should pass");
    }
}
