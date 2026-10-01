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
public class Phase13SupportsPollingTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @TempDir
    Path tempDir;

    @Test
    public void testSupportsPollingOnlyOnSender() throws Exception {
        // Arrange
        AdapterSpecification spec = new AdapterSpecification();
        
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("My Polling Adapter");
        adapter.setVendor("Test");
        adapter.setScheme("my-polling");
        adapter.setPackagePath("com.example.adapter");
        adapter.setVersion("1.0.0");
        adapter.setDirection("both"); // Generates both sender and receiver variants
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("Custom Tech");
        target.setCategory("custom");
        target.setProducerImplementation("LOG.info(\"Sending message\");");
        target.setConsumerImplementation("LOG.info(\"Polling message\");");
        spec.setTarget(target);

        // Act
        generatorService.generateProjectSources(spec, tempDir);

        // Assert Metadata XML
        Path metadataXml = tempDir.resolve("src/main/resources/metadata/metadata.xml");
        assertTrue(Files.exists(metadataXml), "metadata.xml should exist");
        String xmlContent = Files.readString(metadataXml);

        // Extract variants
        String senderVariant = xmlContent.substring(xmlContent.indexOf("<Variant VariantName=\"My Polling Adapter Sender\""), xmlContent.indexOf("<Variant VariantName=\"My Polling Adapter Receiver\""));
        String receiverVariant = xmlContent.substring(xmlContent.indexOf("<Variant VariantName=\"My Polling Adapter Receiver\""), xmlContent.indexOf("<AttributeMetadata>"));

        // 1. Sender metadata contains supportsPolling="true"
        assertTrue(senderVariant.contains("supportsPolling=\"true\""), "Sender metadata should contain supportsPolling=\"true\"");

        // 2. Receiver metadata does not contain supportsPolling="true"
        assertFalse(receiverVariant.contains("supportsPolling=\"true\""), "Receiver metadata should NOT contain supportsPolling=\"true\"");

        // 3. Existing scheduler ReferencedComponents remain unchanged for Sender
        assertTrue(senderVariant.contains("<ReferencedComponentId>ctype::ExtensionVariant/cname::sap:Scheduler/version::1.0</ReferencedComponentId>"), "Sender metadata should reference sap:Scheduler");
        assertTrue(senderVariant.contains("<Name>scheduleKey</Name>"), "Sender metadata should have scheduleKey");
        assertTrue(senderVariant.contains("<AttributeBehavior>Scheduler_ScheduleOnDay,Scheduler_ScheduleToRecur</AttributeBehavior>"), "Sender metadata should have correct AttributeBehavior for scheduler");
    }
}
