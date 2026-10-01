package com.sap.adapter.generator;

import com.sap.adapter.generator.controller.AdapterController;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AutoFixTest {

    @Autowired
    private AdapterController adapterController;

    @Autowired
    private AdapterGeneratorService generatorService;

    @Value("${adapter.generator.workspaces-dir:../../build-workspaces}")
    private String workspacesRootDir;

    @Test
    public void testAutoFix() throws Exception {
        // Use a dynamic build ID to prevent corrupting permanent workspaces and ensure repeatability
        String buildId = "build-autofix-" + UUID.randomUUID().toString().substring(0, 8);
        Path workspaceDir = Paths.get(workspacesRootDir, buildId).toAbsolutePath();
        Files.createDirectories(workspaceDir);

        Map<String, Object> request = new HashMap<>();
        request.put("buildId", buildId);
        request.put("errorLog", "GitHubIssuesSenderAdapterConsumer.java:[153,8] error: unreachable statement");
        
        // Build mock spec
        AdapterSpecification spec = new AdapterSpecification();
        
        com.sap.adapter.generator.model.spec.AdapterMeta adapter = new com.sap.adapter.generator.model.spec.AdapterMeta();
        adapter.setName("GitHubIssuesSenderAdapter");
        adapter.setScheme("github-issues-custom");
        adapter.setDirection("sender");
        adapter.setPackagePath("com.sap.github.issues.sender");
        spec.setAdapter(adapter);

        com.sap.adapter.generator.model.spec.TargetMeta target = new com.sap.adapter.generator.model.spec.TargetMeta();
        target.setTechnology("GitHub REST API");
        
        // Intentional buggy implementation that reproduces the original Auto-Fix regression
        String dummyImpl = "        com.sap.it.api.securestore.SecureStoreService secureStoreService = com.sap.it.api.ITApiFactory.getService(com.sap.it.api.securestore.SecureStoreService.class, null);\n" +
                           "        com.sap.it.api.securestore.UserCredential cred = secureStoreService.getUserCredential(\"alias\");\n" +
                           "        messagesProcessed++;\n" +
                           "        return messagesProcessed;";
        
        target.setConsumerImplementation(dummyImpl);
        spec.setTarget(target);
        
        com.sap.adapter.generator.model.spec.ConnectionMeta conn = new com.sap.adapter.generator.model.spec.ConnectionMeta();
        com.sap.adapter.generator.model.spec.ConnectionParameter cp = new com.sap.adapter.generator.model.spec.ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SAP_SECURE_ALIAS", "Credential Alias");
        conn.setParameters(List.of(cp));
        spec.setConnection(conn);

        // 1. Generate the buggy files so Auto-Fix has context
        generatorService.generateProjectSources(spec, workspaceDir);

        Path consumerSourceFile = workspaceDir.resolve("src/main/java/com/sap/github/issues/sender/GitHubIssuesSenderAdapterConsumer.java");
        String initialSource = Files.readString(consumerSourceFile);
        
        // Verify initial generated source actually contains duplicate returns (one from dummyImpl, one from template)
        int initialFirstIndex = initialSource.indexOf("return messagesProcessed;");
        int initialSecondIndex = initialSource.indexOf("return messagesProcessed;", initialFirstIndex + 1);
        assertTrue(initialFirstIndex != -1 && initialSecondIndex != -1, "Initial buggy source should contain duplicate return statements");

        // 2. Map spec back to JSON structure for Controller
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        request.put("specification", mapper.convertValue(spec, Map.class));

        // 3. Trigger Auto-Fix
        Map<String, Object> response = adapterController.autoFixBuild(request);

        // 4. Assertions on the Auto-Fix response
        assertNotNull(response, "Auto-Fix response should not be null");
        assertEquals(buildId, response.get("buildId"), "Response should match the dynamic buildId");
        
        AdapterSpecification fixedSpec = mapper.convertValue(response.get("specification"), AdapterSpecification.class);
        assertNotNull(fixedSpec, "Fixed specification should be returned");
        assertNotNull(fixedSpec.getTarget(), "Fixed target should be present");
        
        String fixedImpl = fixedSpec.getTarget().getConsumerImplementation();
        assertNotNull(fixedImpl, "Auto-Fix should not erase consumer implementation");
        assertFalse(fixedImpl.contains("return messagesProcessed;"), "Auto-Fix should have removed the template-owned 'return messagesProcessed;' from the raw consumer implementation");
        assertTrue(fixedImpl.contains("secureStoreService.getUserCredential"), "Auto-Fix MUST preserve meaningful business logic and not just delete the implementation");

        // 5. Verify the final generated source file no longer has duplicate returns
        String fixedSource = Files.readString(consumerSourceFile);
        
        int fixedFirstIndex = fixedSource.indexOf("return messagesProcessed;");
        int fixedSecondIndex = fixedSource.indexOf("return messagesProcessed;", fixedFirstIndex + 1);
        
        assertTrue(fixedFirstIndex != -1, "The template-owned return statement must still exist exactly once in the final source");
        assertTrue(fixedSecondIndex == -1, "The duplicate return statement should be eliminated from the final regenerated source");
    }
}
