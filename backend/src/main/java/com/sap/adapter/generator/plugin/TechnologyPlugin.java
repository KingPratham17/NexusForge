package com.sap.adapter.generator.plugin;

import com.sap.adapter.generator.model.tech.TechnologyDefinition;
import com.sap.adapter.generator.model.spec.AdapterSpecification;
import java.util.List;
import java.util.Map;

/**
 * Interface defining the contract for a technology plugin.
 * A plugin provides a TechnologyDefinition (metadata) and a set of Templates (code structure).
 */
public interface TechnologyPlugin {

    /**
     * @return The technology definition (UI metadata, parameters, capabilities)
     */
    TechnologyDefinition getDefinition();

    /**
     * @param spec The processed adapter specification containing dynamic inputs
     * @return A list of templates to evaluate and write to the generated workspace
     */
    List<TemplateDefinition> getTemplates(AdapterSpecification spec);
    
    /**
     * @param spec The adapter specification
     * @return Additional context parameters specifically required by this plugin's templates
     */
    Map<String, String> getPluginContext(AdapterSpecification spec);
}
