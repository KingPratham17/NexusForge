package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class Phase17ConsumerContractTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    private Path testWorkspace;

    @BeforeEach
    public void setup() throws Exception {
        testWorkspace = Paths.get("build-phase17-contract");
    }

    private AdapterSpecification createDummySpec() {
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
        target.setConsumerImplementation(
                "        LOG.info(\"=== Dummy Polling Adapter poll() invoked ===\");\n" +
                "        processMessage(\"Hello from Dummy Polling Adapter\");\n" +
                "        messagesProcessed++;"
        );
        spec.setTarget(target);

        ConnectionMeta conn = new ConnectionMeta();
        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("dummyParam", "Dummy Param", "string", false, "", ""));
        conn.setParameters(params);
        spec.setConnection(conn);

        RuntimeMeta runtime = new RuntimeMeta();
        runtime.setCamelVersion("3.14.7");
        runtime.setAdkVersion("2.2.0");
        spec.setRuntime(runtime);

        return spec;
    }

    private AdapterSpecification createFirebaseSpec() {
        AdapterSpecification spec = new AdapterSpecification();
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("FirebaseFirestoreSender");
        adapter.setScheme("firebase-firestore-custom");
        adapter.setPackagePath("com.poc.firebase");
        adapter.setVendor("Custom");
        adapter.setVersion("1.0.0");
        adapter.setDirection("sender");
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("Firebase Firestore");
        target.setCategory("cloud-storage");
        
        target.setConsumerImplementation(
                "        LOG.info(\"Initializing Firebase connection\");\n" +
                "        // Target-specific Firebase logic here\n" +
                "        processMessage(\"{ 'test': 'data' }\");\n" +
                "        messagesProcessed++;"
        );
        spec.setTarget(target);

        ConnectionMeta conn = new ConnectionMeta();
        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("projectId", "Project ID", "string", true, "", ""));
        params.add(new ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SAP_SECURE_ALIAS", ""));
        conn.setParameters(params);
        spec.setConnection(conn);

        RuntimeMeta runtime = new RuntimeMeta();
        runtime.setCamelVersion("3.14.7");
        runtime.setAdkVersion("2.2.0");
        spec.setRuntime(runtime);

        return spec;
    }

    @Test
    public void testDummyAndFirebaseConsumerContract() throws Exception {
        // 1. Generate Dummy
        AdapterSpecification dummySpec = createDummySpec();
        Map<String, String> dummyFiles = generatorService.generateProjectSources(dummySpec, testWorkspace.resolve("dummy"));
        System.out.println("Dummy files: " + dummyFiles.keySet());
        String dummyConsumer = dummyFiles.get("DummyPollingAdapterConsumer.java");
        assertNotNull(dummyConsumer, "Available files: " + dummyFiles.keySet());

        // 2. Generate Firebase
        AdapterSpecification fbSpec = createFirebaseSpec();
        Map<String, String> fbFiles = generatorService.generateProjectSources(fbSpec, testWorkspace.resolve("firebase"));
        String fbConsumer = fbFiles.get("FirebaseFirestoreSenderAdapterConsumer.java");
        assertNotNull(fbConsumer, "Available Firebase files: " + fbFiles.keySet());

        // 3. Verify exactly one messagesProcessed declaration
        assertEquals(1, countMatches(dummyConsumer, "int messagesProcessed = 0;"), "Dummy should have exactly one messagesProcessed declaration");
        assertEquals(1, countMatches(fbConsumer, "int messagesProcessed = 0;"), "Firebase should have exactly one messagesProcessed declaration");

        // 4. Verify exactly one final return
        assertEquals(1, countMatches(dummyConsumer, "return messagesProcessed;"), "Dummy should have exactly one return messagesProcessed statement");
        assertEquals(1, countMatches(fbConsumer, "return messagesProcessed;"), "Firebase should have exactly one return messagesProcessed statement");

        // 5. Verify no duplicate parameter declarations
        assertFalse(dummyConsumer.contains("String dummyParam = endpoint.getDummyParam();\n        String dummyParam ="), "Should not have duplicate dummyParam");
        assertFalse(fbConsumer.contains("String projectId = endpoint.getProjectId();\n        String projectId ="), "Should not have duplicate projectId");

        // 6. Verify ScheduledPollConsumer inheritance
        assertTrue(dummyConsumer.contains("extends ScheduledPollConsumer"), "Dummy must extend ScheduledPollConsumer");
        assertTrue(fbConsumer.contains("extends ScheduledPollConsumer"), "Firebase must extend ScheduledPollConsumer");
        
        // 7. Check Endpoint contains configureConsumer
        String fbEndpoint = fbFiles.get("FirebaseFirestoreSenderAdapterEndpoint.java");
        assertTrue(fbEndpoint.contains("configureConsumer(consumer);"), "Endpoint must configure the consumer");

        // 8. Both use generic consumer structure (processMessage, etc)
        assertTrue(dummyConsumer.contains("protected void processMessage(Object payload)"), "Dummy must own processMessage");
        assertTrue(fbConsumer.contains("protected void processMessage(Object payload)"), "Firebase must own processMessage");

        // 9. Firebase-specific code only inside target-specific fragment
        assertTrue(fbConsumer.contains("Initializing Firebase connection"));
        assertFalse(dummyConsumer.contains("Initializing Firebase connection"));
    }
    
    @Test
    public void testConsumerSanitizerRejectsInvalidConstructs() throws Exception {
        AdapterSpecification spec = createFirebaseSpec();
        
        // Test Return Rejection
        spec.getTarget().setConsumerImplementation("return messagesProcessed;");
        assertThrows(IllegalArgumentException.class, () -> {
            generatorService.generateProjectSources(spec, testWorkspace.resolve("fail-return"));
        });
        
        // Test Class Declaration Rejection
        spec.getTarget().setConsumerImplementation("class MyFirebaseHelper {}");
        assertThrows(IllegalArgumentException.class, () -> {
            generatorService.generateProjectSources(spec, testWorkspace.resolve("fail-class"));
        });
        
        // Test ProcessMessage overriding Rejection
        spec.getTarget().setConsumerImplementation("public void processMessage(Object payload) {}");
        assertThrows(IllegalArgumentException.class, () -> {
            generatorService.generateProjectSources(spec, testWorkspace.resolve("fail-method"));
        });
        
        // Test messagesProcessed declaration Rejection
        spec.getTarget().setConsumerImplementation("int messagesProcessed = 0; messagesProcessed++;");
        assertThrows(IllegalArgumentException.class, () -> {
            generatorService.generateProjectSources(spec, testWorkspace.resolve("fail-var"));
        });
    }

    private int countMatches(String str, String target) {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }
}
