package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.model.build.BuildJob;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import com.sap.adapter.generator.service.builder.MavenBuildWorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@SpringBootTest
public class Phase11IsolationTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @Autowired
    private MavenBuildWorkerService mavenService;

    @Test
    public void generateAndBuildDummyAdapter() throws Exception {
        String buildId = "build-dummy-" + UUID.randomUUID().toString().substring(0, 8);
        Path workspacePath = Paths.get(System.getProperty("user.dir")).getParent().resolve("build-workspaces").resolve(buildId);
        Files.createDirectories(workspacePath);

        AdapterSpecification spec = new AdapterSpecification();
        
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("DummyPollingAdapter");
        adapter.setScheme("dummy-polling-custom");
        adapter.setPackagePath("com.poc.dummy");
        adapter.setDirection("sender");
        adapter.setVendor("IsolationTest");
        adapter.setVersion("1.0.0");
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("DummyTech");
        target.setCategory("test");
        target.setConsumerImplementation("        LOG.info(\"=== Dummy Polling Adapter poll() invoked ===\");\n" +
                                         "        processMessage(\"Hello from Dummy Polling Adapter\");\n" +
                                         "        messagesProcessed = 1;");
        spec.setTarget(target);

        ConnectionMeta conn = new ConnectionMeta();
        conn.getParameters().add(new ConnectionParameter("dummyParam", "Dummy Param", "string", false, "", "Dummy param to bypass defaults"));
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
            job = mavenService.getJob(buildId); // Need to refresh the job status!
        }

        System.out.println("================================================");
        System.out.println("BUILD STATUS: " + job.getStatus());
        System.out.println("--- BUILD LOGS ---");
        System.out.println(String.join("\n", job.getBuildLogs()));
        System.out.println("================================================");

        if (job.getInspectionReport() != null) {
            System.out.println("ESA PATH: " + job.getInspectionReport().getEsaPath());
        }

        Path endpointPath = workspacePath.resolve("src/main/java/com/poc/dummy/DummyPollingAdapterEndpoint.java");
        System.out.println("--- GENERATED ENDPOINT ---");
        System.out.println(Files.readString(endpointPath));

        Path metadataPath = workspacePath.resolve("src/main/resources/metadata/metadata.xml");
        System.out.println("--- GENERATED METADATA ---");
        System.out.println(Files.readString(metadataPath));
    }
}
