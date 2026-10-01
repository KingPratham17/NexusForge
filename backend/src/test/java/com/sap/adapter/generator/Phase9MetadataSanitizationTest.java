package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import com.sap.adapter.generator.service.builder.MavenBuildWorkerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase9MetadataSanitizationTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @Autowired
    private MavenBuildWorkerService mavenService;

    @TempDir
    Path tempDir;

    @Test
    public void testMetadataSanitizationWithSpaces() throws Exception {
        // Arrange
        AdapterSpecification spec = new AdapterSpecification();
        
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("My Adapter Name!"); // spaces and punctuation
        adapter.setVendor("N Pratham Wadeiyar"); // spaces
        adapter.setScheme("my-adapter");
        adapter.setPackagePath("com.example.adapter");
        adapter.setVersion("1.0.0");
        adapter.setDirection("both");
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("Custom Tech");
        target.setCategory("custom");
        target.setProducerImports("import java.util.List;");
        target.setProducerImplementation("LOG.info(\"Sending message\");");
        target.setConsumerImports("import java.util.List;");
        target.setConsumerImplementation("LOG.info(\"Polling message\");");
        spec.setTarget(target);

        // Act
        generatorService.generateProjectSources(spec, tempDir);

        // Assert Metadata XML
        Path metadataXml = tempDir.resolve("src/main/resources/metadata/metadata.xml");
        assertTrue(Files.exists(metadataXml), "metadata.xml should exist at " + metadataXml);
        String xmlContent = Files.readString(metadataXml);

        // 1. generated ComponentId contains the sanitized vendor identifier
        assertTrue(xmlContent.contains("ComponentId=\"ctype::Adapter/cname::My_Adapter_Name_/vendor::N_Pratham_Wadeiyar/version::1.0.0\""), 
                   "ComponentId should use sanitized machine identifiers: " + xmlContent);

        // 2. generated Sender/Receiver VariantId contains the sanitized vendor identifier
        assertTrue(xmlContent.contains("VariantId=\"ctype::AdapterVariant/cname::My_Adapter_Name_/vendor::N_Pratham_Wadeiyar/tp::my-adapter/mp::my-adapter/direction::Sender\""),
                   "Sender VariantId should use sanitized machine identifiers");
        assertTrue(xmlContent.contains("VariantId=\"ctype::AdapterVariant/cname::My_Adapter_Name_/vendor::N_Pratham_Wadeiyar/tp::my-adapter/mp::my-adapter/direction::Receiver\""),
                   "Receiver VariantId should use sanitized machine identifiers");

        // 3. human-readable display fields retain original values
        assertTrue(xmlContent.contains("ComponentName=\"My Adapter Name!\""), "ComponentName should retain original spaces");
        assertTrue(xmlContent.contains("ComponentDisplayName=\"My Adapter Name!\""), "ComponentDisplayName should retain original spaces");
        assertTrue(xmlContent.contains("VariantName=\"My Adapter Name! Sender\""), "Sender VariantName should retain original spaces");
        assertTrue(xmlContent.contains("VariantName=\"My Adapter Name! Receiver\""), "Receiver VariantName should retain original spaces");

        // 4. no unresolved placeholders remain
        assertFalse(xmlContent.contains("${adapterNameId}"), "Should not have ${adapterNameId} unresolved");
        assertFalse(xmlContent.contains("${vendorId}"), "Should not have ${vendorId} unresolved");
        assertFalse(xmlContent.contains("${adapterName}"), "Should not have ${adapterName} unresolved");
        // 5. ESA generation will be verified via real build
    }
}
