package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase10EndpointConsumerTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @TempDir
    Path tempDir;

    @Test
    public void testEndpointConsumerIsConfigured() throws Exception {
        // Arrange
        AdapterSpecification spec = new AdapterSpecification();
        
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("MyPollingAdapter");
        adapter.setScheme("my-polling-adapter");
        adapter.setPackagePath("com.example.adapter");
        adapter.setDirection("sender"); // Must be sender to generate createConsumer
        spec.setAdapter(adapter);

        // Act
        generatorService.generateProjectSources(spec, tempDir);

        // Assert Endpoint Java source
        Path endpointJava = tempDir.resolve("src/main/java/com/example/adapter/MyPollingAdapterEndpoint.java");
        assertTrue(Files.exists(endpointJava), "Endpoint.java should exist at " + endpointJava);
        String javaContent = Files.readString(endpointJava);

        assertTrue(javaContent.contains("public Consumer createConsumer(Processor processor) throws Exception"),
                   "createConsumer method should exist");
        
        // Assert that the consumer is instantiated and configureConsumer is called
        assertTrue(javaContent.contains("MyPollingAdapterConsumer consumer = new MyPollingAdapterConsumer(this, processor);"),
                   "Should instantiate consumer to a local variable");
        assertTrue(javaContent.contains("configureConsumer(consumer);"),
                   "Should configure the consumer for SAP ADK scheduler support");
        assertTrue(javaContent.contains("return consumer;"),
                   "Should return the configured consumer");
    }
}
