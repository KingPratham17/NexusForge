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
public class Phase18RealFirebaseGenerationTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    private Path testWorkspace;

    @BeforeEach
    public void setup() throws Exception {
        testWorkspace = Paths.get("build-phase18-firebase-real");
        if (Files.exists(testWorkspace)) {
            Files.walk(testWorkspace)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> {
                     try { Files.delete(p); } catch (Exception e) {}
                 });
        }
        Files.createDirectories(testWorkspace);
    }

    private AdapterSpecification createFirebaseSpec() {
        AdapterSpecification spec = new AdapterSpecification();
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("RealFirebaseFirestoreSender");
        adapter.setScheme("real-firebase-firestore-custom");
        adapter.setPackagePath("com.poc.firebase");
        adapter.setVendor("Custom");
        adapter.setVersion("1.0.0");
        adapter.setDirection("sender");
        spec.setAdapter(adapter);

        TargetMeta target = new TargetMeta();
        target.setTechnology("Firebase Firestore");
        target.setCategory("cloud-storage");
        
        target.setConsumerImplementation(
                "        LOG.info(\"FIREBASE_DIAG: FIREBASE AUTH START\");\n" +
                "        com.sap.it.api.securestore.SecureStoreService secureStore = \n" +
                "                com.sap.it.api.ITApiFactory.getService(com.sap.it.api.securestore.SecureStoreService.class, null);\n" +
                "        com.sap.it.api.securestore.UserCredential credential = secureStore.getUserCredential(credentialAlias);\n" +
                "        if (credential == null) {\n" +
                "            throw new IllegalStateException(\"Secure store credential not found for alias: \" + credentialAlias);\n" +
                "        }\n" +
                "        \n" +
                "        LOG.info(\"FIREBASE_DIAG: FIREBASE AUTH SUCCESS\");\n" +
                "        LOG.info(\"FIREBASE_DIAG: FIRESTORE CLIENT INIT START\");\n" +
                "\n" +
                "        String serviceAccountJson = new String(credential.getPassword());\n" +
                "        com.google.auth.oauth2.GoogleCredentials credentials = \n" +
                "                com.google.auth.oauth2.GoogleCredentials.fromStream(\n" +
                "                        new java.io.ByteArrayInputStream(serviceAccountJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)));\n" +
                "\n" +
                "        com.google.cloud.firestore.FirestoreOptions firestoreOptions = \n" +
                "                com.google.cloud.firestore.FirestoreOptions.newBuilder()\n" +
                "                .setCredentials(credentials)\n" +
                "                .setProjectId(projectId)\n" +
                "                .build();\n" +
                "                \n" +
                "        try (com.google.cloud.firestore.Firestore firestore = firestoreOptions.getService()) {\n" +
                "            LOG.info(\"FIREBASE_DIAG: FIRESTORE CLIENT INIT SUCCESS\");\n" +
                "            LOG.info(\"FIREBASE_DIAG: FIRESTORE QUERY START\");\n" +
                "\n" +
                "            com.google.cloud.firestore.CollectionReference collection = firestore.collection(collectionName);\n" +
                "            com.google.cloud.firestore.Query query = collection;\n" +
                "            \n" +
                "            if (maxDocuments != null && !maxDocuments.trim().isEmpty()) {\n" +
                "                int limit = Integer.parseInt(maxDocuments);\n" +
                "                query = query.limit(limit);\n" +
                "            }\n" +
                "\n" +
                "            com.google.api.core.ApiFuture<com.google.cloud.firestore.QuerySnapshot> querySnapshotFuture = query.get();\n" +
                "            com.google.cloud.firestore.QuerySnapshot querySnapshot = querySnapshotFuture.get();\n" +
                "            \n" +
                "            LOG.info(\"FIREBASE_DIAG: FIRESTORE QUERY SUCCESS. Retrieved {} documents.\", querySnapshot.size());\n" +
                "\n" +
                "            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();\n" +
                "\n" +
                "            for (com.google.cloud.firestore.QueryDocumentSnapshot document : querySnapshot.getDocuments()) {\n" +
                "                LOG.info(\"FIREBASE_DIAG: PROCESS MESSAGE START\");\n" +
                "                java.util.Map<String, Object> data = new java.util.HashMap<>(document.getData());\n" +
                "                data.put(\"__documentId\", document.getId());\n" +
                "                \n" +
                "                String payload = mapper.writeValueAsString(data);\n" +
                "                processMessage(payload);\n" +
                "                messagesProcessed++;\n" +
                "                LOG.info(\"FIREBASE_DIAG: PROCESS MESSAGE SUCCESS\");\n" +
                "            }\n" +
                "        }"
        );
        target.setCustomDependencies(
            "    <dependency>\n" +
            "      <groupId>com.google.cloud</groupId>\n" +
            "      <artifactId>google-cloud-firestore</artifactId>\n" +
            "      <version>3.7.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>com.google.auth</groupId>\n" +
            "      <artifactId>google-auth-library-oauth2-http</artifactId>\n" +
            "      <version>1.12.1</version>\n" +
            "    </dependency>\n"
        );
        target.setExcludedImports(
            "              !com.google.firebase.*,\n" +
            "              !com.google.auth.*,\n" +
            "              !com.google.cloud.*,\n" +
            "              !com.google.api.*,\n" +
            "              !com.google.protobuf.*,\n" +
            "              !com.google.common.*,\n" +
            "              !com.google.gson.*,\n" +
            "              !io.grpc.*,\n" +
            "              !io.netty.*,\n" +
            "              !io.opencensus.*,\n" +
            "              !io.perfmark.*,\n" +
            "              !org.threeten.*,\n" +
            "              !org.conscrypt.*,\n" +
            "              !org.apache.http.*,\n" +
            "              !com.fasterxml.jackson.*,\n"
        );
        
        spec.setTarget(target);

        ConnectionMeta conn = new ConnectionMeta();
        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("projectId", "Project ID", "string", true, "", ""));
        params.add(new ConnectionParameter("collectionName", "Collection Name", "string", true, "", ""));
        params.add(new ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SAP_SECURE_ALIAS", ""));
        params.add(new ConnectionParameter("maxDocuments", "Max Documents", "integer", false, "100", ""));
        conn.setParameters(params);
        spec.setConnection(conn);

        RuntimeMeta runtime = new RuntimeMeta();
        runtime.setCamelVersion("3.14.7");
        runtime.setAdkVersion("2.2.0");
        spec.setRuntime(runtime);

        return spec;
    }

    @Test
    public void testGenerateRealFirebaseAdapter() throws Exception {
        AdapterSpecification spec = createFirebaseSpec();
        Map<String, String> files = generatorService.generateProjectSources(spec, testWorkspace);
        
        String consumer = files.get("RealFirebaseFirestoreSenderAdapterConsumer.java");
        assertNotNull(consumer, "Failed to get consumer");
        
        System.out.println("TEST COMPLETED. SUCCESS.");
    }
}
