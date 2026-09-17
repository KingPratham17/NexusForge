package com.sap.adapter.generator.service.generator;

import org.junit.jupiter.api.Test;
import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.plugin.impl.RestHttpPlugin;
import com.sap.adapter.generator.registry.TechnologyRegistry;
import com.sap.adapter.generator.service.generator.AdapterGeneratorService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestGenerator {
    @Test
    public void testGeneration() throws Exception {
        TechnologyRegistry registry = new TechnologyRegistry(List.of(new RestHttpPlugin()));
        AdapterGeneratorService service = new AdapterGeneratorService();
        java.lang.reflect.Field f = service.getClass().getDeclaredField("registry");
        f.setAccessible(true);
        f.set(service, registry);
        
        AdapterSpecification spec = new AdapterSpecification();
        AdapterMeta adapter = new AdapterMeta();
        adapter.setName("TestRestReceiverAdapter");
        adapter.setDirection("receiver");
        adapter.setScheme("rest-custom");
        adapter.setPackagePath("com.test");
        spec.setAdapter(adapter);
        
        TargetMeta target = new TargetMeta();
        target.setTechnology("rest-http");
        spec.setTarget(target);
        
        Path ws = Paths.get("C:/Users/pratham.wadeiyar/.gemini/antigravity-ide/scratch/backend-test");
        service.generateProjectSources(spec, ws);
        
        System.out.println("Generation complete.");
    }
}
