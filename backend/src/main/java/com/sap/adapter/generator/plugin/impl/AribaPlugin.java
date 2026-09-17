package com.sap.adapter.generator.plugin.impl;

import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.model.spec.ConnectionParameter;
import com.sap.adapter.generator.model.spec.OperationConfig;
import com.sap.adapter.generator.model.tech.TechnologyDefinition;
import com.sap.adapter.generator.plugin.TechnologyPlugin;
import com.sap.adapter.generator.plugin.TemplateDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AribaPlugin implements TechnologyPlugin {

    private final TechnologyDefinition definition;

    public AribaPlugin() {
        definition = new TechnologyDefinition(
                "ariba-network",
                "SAP Ariba Network",
                "procurement",
                "ariba-custom",
                "SAP Ariba Network Integration Adapter"
        );
        definition.setSupportedDirections(List.of("receiver", "sender"));
        definition.setAuthenticationModels(List.of("secure-parameter", "oauth2"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("aribaRealm", "Ariba Realm", "string", true, "my-realm", "SAP Ariba Realm ID"));
        params.add(new ConnectionParameter("apiKeyAlias", "API Key Secure Alias", "secure-alias", true, "ARIBA_API_KEY", "Secure alias for Ariba API Key"));
        definition.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("SEND_DOCUMENT", true, "Send procurement document to Ariba"));
        ops.add(new OperationConfig("POLL_STATUS", false, "Poll document status from Ariba"));
        definition.setDefaultOperations(ops);
    }

    @Override
    public TechnologyDefinition getDefinition() {
        return definition;
    }

    @Override
    public List<TemplateDefinition> getTemplates(AdapterSpecification spec) {
        return new ArrayList<>(); // Use generic templates inside AdapterGeneratorService
    }

    @Override
    public Map<String, String> getPluginContext(AdapterSpecification spec) {
        return new HashMap<>();
    }
}
