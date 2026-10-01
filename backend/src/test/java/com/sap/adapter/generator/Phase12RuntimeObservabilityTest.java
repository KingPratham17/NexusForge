package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.model.build.BuildJob;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import com.sap.adapter.generator.service.builder.MavenBuildWorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase12RuntimeObservabilityTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @Autowired
    private MavenBuildWorkerService mavenService;

    @Test
    public void generateAndBuildAdapterWithObservabilityLogging() throws Exception {
        String buildId = "build-obs-" + UUID.randomUUID().toString().substring(0, 8);
        Path workspacePath = Paths.get(System.getProperty("user.dir")).getParent().resolve("build-workspaces").resolve(buildId);
        Files.createDirectories(workspacePath);

        AdapterSpecification spec = new AdapterSpecification();
        
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("ObservabilityAdapter");
        adapter.setScheme("observability-custom");
        adapter.setPackagePath("com.poc.obs");
        adapter.setDirection("sender");
        adapter.setVendor("ObservabilityTest");
        adapter.setVersion("1.0.0");
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("ObservabilityTech");
        target.setCategory("test");
        
        // Simulating an AI generated implementation that strictly follows the Observability guidelines
        String consumerImpl = "        // 1. Configuration\n" +
                              "        LOG.info(\"Entering polling implementation for ObservabilityAdapter\");\n" +
                              "        LOG.info(\"Configured dummyParam: {}\", dummyParam);\n" +
                              "\n" +
                              "        // 2. Authentication\n" +
                              "        LOG.info(\"Retrieving credential alias\");\n" +
                              "        // Mock credential retrieval (no secrets logged)\n" +
                              "        LOG.info(\"Successfully retrieved credential\");\n" +
                              "\n" +
                              "        // 3. Client Initialization\n" +
                              "        LOG.info(\"Initializing target client\");\n" +
                              "        // Mock client init\n" +
                              "        LOG.info(\"Successfully initialized target client\");\n" +
                              "\n" +
                              "        // 4. Target Request\n" +
                              "        LOG.info(\"Executing target request\");\n" +
                              "        // Mock request\n" +
                              "        LOG.info(\"Successfully executed target request. Returned 1 document.\");\n" +
                              "\n" +
                              "        // 5 & 6. Result Extraction and Payload Serialization\n" +
                              "        LOG.info(\"Serializing payload. Payload length: 15\");\n" +
                              "        String payload = \"{\\\"key\\\":\\\"value\\\"}\";\n" +
                              "\n" +
                              "        // 7 & 8. Process Message\n" +
                              "        LOG.info(\"Before processMessage()\");\n" +
                              "        processMessage(payload);\n" +
                              "        messagesProcessed++;\n" +
                              "        LOG.info(\"Final messagesProcessed count: {}\", messagesProcessed);\n";
                              
        target.setConsumerImplementation(consumerImpl);
        spec.setTarget(target);

        ConnectionMeta conn = new ConnectionMeta();
        conn.getParameters().add(new ConnectionParameter("dummyParam", "Dummy Param", "string", false, "", "Dummy param"));
        spec.setConnection(conn);

        // Generate files
        generatorService.generateProjectSources(spec, workspacePath);

        // Run Maven Build
        BuildJob job = mavenService.createBuildJob(buildId, adapter.getName(), workspacePath.toString());
        mavenService.executeBuild(buildId, adapter.getScheme());

        // Wait for build to finish
        while (job.getStatus().name().equals("INITIALIZED") || 
               job.getStatus().name().equals("GENERATED") || 
               job.getStatus().name().equals("RUNNING") || 
               job.getStatus().name().equals("PENDING") || 
               job.getStatus().name().equals("COMPILING") ||
               job.getStatus().name().equals("PACKAGING") ||
               job.getStatus().name().equals("ADK_VALIDATION")) {
            Thread.sleep(1000);
            job = mavenService.getJob(buildId);
        }

        System.out.println("BUILD STATUS: " + job.getStatus());
        System.out.println("--- BUILD LOGS ---");
        System.out.println(String.join("\n", job.getBuildLogs()));

        assertEquals("SUCCESS", job.getStatus().name(), "Build must succeed with observability logging code");

        Path consumerPath = workspacePath.resolve("src/main/java/com/poc/obs/ObservabilityAdapterConsumer.java");
        String generatedConsumer = Files.readString(consumerPath);

        // Verify template-owned variables aren't redeclared and secrets aren't logged (based on our AI spec mock)
        assertTrue(generatedConsumer.contains("LOG.info(\"Entering polling implementation for ObservabilityAdapter\");"), "Missing observability log");
        assertFalse(generatedConsumer.contains("int messagesProcessed = 0;") && generatedConsumer.indexOf("int messagesProcessed = 0;") != generatedConsumer.lastIndexOf("int messagesProcessed = 0;"), "Template variable messagesProcessed redeclared");
    }
}
