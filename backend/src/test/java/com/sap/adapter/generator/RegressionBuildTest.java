package com.sap.adapter.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.adapter.generator.model.Project;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.repository.ProjectRepository;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
public class RegressionBuildTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AdapterGeneratorService generator;

    @Test
    public void generateAllProjects() throws Exception {
        System.out.println(">>> STARTING REGRESSION GENERATION <<<");
        List<Project> projects = projectRepository.findAll();
        ObjectMapper mapper = new ObjectMapper();
        
        for (Project p : projects) {
            if (p.getAdapterSpecificationJson() != null && !p.getAdapterSpecificationJson().isEmpty()) {
                AdapterSpecification spec = mapper.readValue(p.getAdapterSpecificationJson(), AdapterSpecification.class);
                Path ws = Paths.get("../../build-workspaces/" + p.getBuildId()).toAbsolutePath().normalize();
                
                System.out.println("Generating project: " + p.getName() + " in " + ws);
                try {
                    generator.generateProjectSources(spec, ws);
                    System.out.println("SUCCESS generated: " + p.getName());
                } catch (Exception e) {
                    System.out.println("FAILED generation: " + p.getName());
                    e.printStackTrace();
                }
            }
        }
        System.out.println(">>> REGRESSION GENERATION COMPLETE <<<");
    }
}
