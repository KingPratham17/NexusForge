package com.sap.adapter.generator;

import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.model.spec.AdapterMeta;
import com.sap.adapter.generator.model.spec.TargetMeta;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
public class Phase13GenerateFirebaseTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @org.springframework.beans.factory.annotation.Value("${adapter.generator.workspaces-dir:../../build-workspaces}")
    private String workspacesDir;

    @Test
    public void generateFirebaseWorkspace() throws Exception {
        Path baseDir = Paths.get(workspacesDir).toAbsolutePath();
        
        AdapterSpecification spec = new AdapterSpecification();
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("Firebase Firestore Sender");
        adapter.setScheme("firebase-firestore-custom");
        adapter.setPackagePath("com.poc.firebasefirestore");
        adapter.setVersion("1.0.0");
        adapter.setDirection("sender"); // Only Sender for this phase
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("Firebase Firestore");
        target.setCategory("custom");
        target.setCustomDependencies(
                "<dependency>\n" +
                "  <groupId>com.google.firebase</groupId>\n" +
                "  <artifactId>firebase-admin</artifactId>\n" +
                "  <version>9.1.1</version>\n" +
                "</dependency>"
        );
        target.setConsumerImports(
                "import com.google.firebase.FirebaseApp;\n" +
                "import com.google.firebase.FirebaseOptions;\n" +
                "import com.google.firebase.cloud.FirestoreClient;\n" +
                "import com.google.cloud.firestore.Firestore;\n" +
                "import com.google.cloud.firestore.QueryDocumentSnapshot;\n" +
                "import com.google.cloud.firestore.QuerySnapshot;\n" +
                "import com.google.auth.oauth2.GoogleCredentials;\n" +
                "import java.io.ByteArrayInputStream;\n" +
                "import java.nio.charset.StandardCharsets;"
        );
        target.setConsumerImplementation(
                "int messagesProcessed = 0;\n" +
                "try {\n" +
                "    String projectId = \"cpi-custom-adapter-poc\";\n" +
                "    LOG.info(\"Initializing Firebase connection\");\n" +
                "    // Dummy initialization code for generation\n" +
                "} catch (Exception e) {\n" +
                "    LOG.error(\"Error\", e);\n" +
                "}"
        );
        spec.setTarget(target);
        
        Path targetDir = baseDir.resolve("build-firebase-firestore");
        generatorService.generateProjectSources(spec, targetDir);
    }
}
