package com.sap.adapter.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.adapter.generator.controller.AdapterController;
import com.sap.adapter.generator.model.build.BuildJob;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.service.builder.MavenBuildWorkerService;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class PrintJsonTest {
    @Autowired
    private AdapterController adapterController;

    @MockBean
    private MavenBuildWorkerService mavenBuildWorkerService;

    @MockBean
    private AdapterGeneratorService adapterGeneratorService;

    @Test
    public void test() throws Exception {
        Mockito.when(mavenBuildWorkerService.createBuildJob(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(new BuildJob());

        Map<String, Object> request = new HashMap<>();
        request.put("buildId", "build-autofix-test");
        request.put("errorLog", "GitHubIssuesSenderAdapterConsumer.java:[153,8] error: unreachable statement\nreturn messagesProcessed;\n^");
        
        AdapterSpecification spec = new AdapterSpecification();
        com.sap.adapter.generator.model.spec.AdapterMeta adapter = new com.sap.adapter.generator.model.spec.AdapterMeta();
        adapter.setName("GitHubIssuesSenderAdapter");
        adapter.setScheme("github-issues-custom");
        spec.setAdapter(adapter);

        com.sap.adapter.generator.model.spec.TargetMeta target = new com.sap.adapter.generator.model.spec.TargetMeta();
        target.setTechnology("GitHub REST API");
        target.setConsumerImplementation("        messagesProcessed++;\n        return messagesProcessed;");
        spec.setTarget(target);
        
        ObjectMapper mapper = new ObjectMapper();
        request.put("specification", mapper.convertValue(spec, Map.class));

        Map<String, Object> response = adapterController.autoFixBuild(request);
        AdapterSpecification fixedSpec = mapper.convertValue(response.get("specification"), AdapterSpecification.class);
        System.out.println("CLAUDE RETURNED IMPL: [" + fixedSpec.getTarget().getConsumerImplementation() + "]");
    }
}
