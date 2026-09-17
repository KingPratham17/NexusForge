package com.sap.adapter.generator.registry;

import com.sap.adapter.generator.model.spec.ConnectionParameter;
import com.sap.adapter.generator.model.spec.OperationConfig;
import com.sap.adapter.generator.model.tech.TechnologyDefinition;
import com.sap.adapter.generator.plugin.TechnologyPlugin;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TechnologyRegistry {

    private final Map<String, TechnologyPlugin> plugins = new LinkedHashMap<>();

    public TechnologyRegistry(List<TechnologyPlugin> pluginList) {
        if (pluginList != null) {
            for (TechnologyPlugin plugin : pluginList) {
                plugins.put(plugin.getDefinition().getId(), plugin);
            }
        }
    }

    public Collection<TechnologyDefinition> getAllTechnologies() {
        return plugins.values().stream().map(TechnologyPlugin::getDefinition).toList();
    }

    public Optional<TechnologyDefinition> getById(String id) {
        TechnologyPlugin plugin = plugins.get(id);
        return plugin != null ? Optional.of(plugin.getDefinition()) : Optional.empty();
    }
    
    public Optional<TechnologyPlugin> getPluginById(String id) {
        return Optional.ofNullable(plugins.get(id));
    }
}
