package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.model.spec.AdapterMeta;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class Phase15ADKConsistencyTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @Test
    public void testAdkConsistencyCMDAndManifest() throws Exception {
        // Given an adapter with spaces and special characters to ensure sanitization is handled correctly
        AdapterSpecification spec = new AdapterSpecification();
        AdapterMeta meta = new AdapterMeta();
        meta.setName("My Custom Adapter 2.0!"); // Needs sanitization for ID
        meta.setScheme("my-custom");
        meta.setPackagePath("com.sap.custom");
        meta.setVendor("SAP SE!"); // Needs sanitization for ID
        meta.setVersion("1.0.5");
        meta.setDirection("both");
        spec.setAdapter(meta);

        Path workspaceDir = Paths.get("build-workspaces/build-adk-consistency");

        // When we generate the adapter
        generatorService.generateProjectSources(spec, workspaceDir);

        // Then we extract config.adk
        Path configAdkPath = workspaceDir.resolve("config.adk");
        assertTrue(Files.exists(configAdkPath), "config.adk must exist");
        
        Properties configAdk = new Properties();
        configAdk.load(Files.newInputStream(configAdkPath));
        
        String configAdapterId = configAdk.getProperty("Adapter-ID");
        String configAdapterName = configAdk.getProperty("Adapter-Name");
        String configAdapterVendor = configAdk.getProperty("Adapter-Vendor");
        String configAdapterVersion = configAdk.getProperty("Adapter-Version");

        assertEquals("My_Custom_Adapter_2.0_", configAdapterId, "Adapter-ID in config.adk must be sanitized");
        assertEquals("My Custom Adapter 2.0!", configAdapterName, "Adapter-Name in config.adk must be display name");
        assertEquals("SAP_SE_", configAdapterVendor, "Adapter-Vendor in config.adk must be sanitized");
        assertEquals("1.0.5", configAdapterVersion, "Adapter-Version in config.adk must be correct");

        // And we extract metadata.xml
        Path metadataXmlPath = workspaceDir.resolve("metadata").resolve("metadata.xml");
        assertTrue(Files.exists(metadataXmlPath), "metadata.xml must exist");
        String metadataContent = Files.readString(metadataXmlPath);

        // Parse ComponentId="ctype::Adapter/cname::${adapterNameId}/vendor::${vendorId}/version::${adapterVersion}"
        Pattern componentIdPattern = Pattern.compile("ComponentId=\"ctype::Adapter/cname::([^/]+)/vendor::([^/]+)/version::([^\"]+)\"");
        Matcher matcher = componentIdPattern.matcher(metadataContent);
        assertTrue(matcher.find(), "metadata.xml must contain ComponentId with expected format");
        
        String metaCname = matcher.group(1);
        String metaVendor = matcher.group(2);
        String metaVersion = matcher.group(3);
        
        // Parse ComponentName and ComponentDisplayName
        Pattern componentNamePattern = Pattern.compile("ComponentName=\"([^\"]+)\"");
        Matcher nameMatcher = componentNamePattern.matcher(metadataContent);
        assertTrue(nameMatcher.find());
        String metaName = nameMatcher.group(1);
        
        // Ensure consistency between config.adk and metadata.xml
        assertEquals(configAdapterId, metaCname, "Adapter ID must match between config.adk and metadata.xml");
        assertEquals(configAdapterVendor, metaVendor, "Adapter Vendor must match between config.adk and metadata.xml");
        assertEquals(configAdapterName, metaName, "Adapter Name must match between config.adk and metadata.xml");
        assertEquals(configAdapterVersion, metaVersion, "Adapter Version must match between config.adk and metadata.xml");
        
        // Ensure version is consistent with specification
        assertEquals("1.0.5", metaVersion, "Version must match spec version");
        
        // Also verify pom.xml uses the same version
        Path pomPath = workspaceDir.resolve("pom.xml");
        String pomContent = Files.readString(pomPath);
        assertTrue(pomContent.contains("<version>1.0.5</version>"), "pom.xml must contain the exact generated version");
    }
}
