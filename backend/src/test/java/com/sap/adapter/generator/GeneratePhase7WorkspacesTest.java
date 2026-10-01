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
public class GeneratePhase7WorkspacesTest {

    @Autowired
    private AdapterGeneratorService generatorService;

    @org.springframework.beans.factory.annotation.Value("${adapter.generator.workspaces-dir:../../build-workspaces}")
    private String workspacesDir;

    @Test
    public void generateWorkspaces() throws Exception {
        Path baseDir = Paths.get(workspacesDir).toAbsolutePath();
        
        String safeCode = "SecureStoreService secureStoreService = null; UserCredential credential = null;";

        // 1. REST Receiver
        AdapterSpecification restSpec = new AdapterSpecification();
        AdapterMeta restAdapter = new AdapterMeta();
        restAdapter.setName("RestReceiverAdapter");
        restAdapter.setScheme("rest-custom");
        restSpec.setAdapter(restAdapter);
        TargetMeta restTarget = new TargetMeta();
        restTarget.setTechnology("REST API");
        restTarget.setCategory("custom");
        restTarget.setCustomDependencies("<dependency>\n  <groupId>org.apache.httpcomponents</groupId>\n  <artifactId>httpclient</artifactId>\n  <version>4.5.14</version>\n</dependency>");
        restTarget.setProducerImplementation(safeCode);
        restSpec.setTarget(restTarget);
        Path restDir = baseDir.resolve("build-rest-receiver");
        generatorService.generateProjectSources(restSpec, restDir);

        // 2. Slack Sender
        AdapterSpecification slackSpec = new AdapterSpecification();
        AdapterMeta slackAdapter = new AdapterMeta();
        slackAdapter.setName("SlackSenderAdapter");
        slackAdapter.setScheme("slack-custom");
        slackSpec.setAdapter(slackAdapter);
        TargetMeta slackTarget = new TargetMeta();
        slackTarget.setTechnology("Slack Web API");
        slackTarget.setCategory("custom");
        slackTarget.setCustomDependencies("<dependency>\n  <groupId>com.slack.api</groupId>\n  <artifactId>slack-api-client</artifactId>\n  <version>1.30.0</version>\n</dependency>");
        slackTarget.setConsumerImplementation(safeCode);
        slackTarget.setProducerImplementation(safeCode);
        slackSpec.setTarget(slackTarget);
        Path slackDir = baseDir.resolve("build-slack-sender");
        generatorService.generateProjectSources(slackSpec, slackDir);
        
        // 3. GitHub Issues Sender
        AdapterSpecification githubSpec = new AdapterSpecification();
        AdapterMeta githubAdapter = new AdapterMeta();
        githubAdapter.setName("GitHubIssuesSenderAdapter");
        githubAdapter.setScheme("github-issues-custom");
        githubSpec.setAdapter(githubAdapter);
        TargetMeta githubTarget = new TargetMeta();
        githubTarget.setTechnology("GitHub REST API");
        githubTarget.setCategory("custom");
        githubTarget.setCustomDependencies("");
        githubTarget.setProducerImports("import com.google.gson.Gson;\nimport com.google.gson.JsonObject;");
        githubTarget.setConsumerImplementation(safeCode);
        githubTarget.setProducerImplementation(safeCode + " Gson gson = new Gson();");
        githubSpec.setTarget(githubTarget);
        Path githubDir = baseDir.resolve("build-github-sender");
        generatorService.generateProjectSources(githubSpec, githubDir);
    }
}
