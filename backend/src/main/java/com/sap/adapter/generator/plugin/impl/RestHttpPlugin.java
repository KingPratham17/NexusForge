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
public class RestHttpPlugin implements TechnologyPlugin {

    private final TechnologyDefinition definition;

    public RestHttpPlugin() {
        definition = new TechnologyDefinition(
                "rest-http",
                "REST / HTTP Endpoint",
                "http-api",
                "rest-custom",
                "Generic REST HTTP integration adapter supporting JSON/XML REST web services."
        );
        definition.setSupportedDirections(List.of("receiver", "sender"));
        definition.setAuthenticationModels(List.of("secure-parameter", "oauth2", "basic", "none"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("baseUrl", "Base URL", "string", true, "https://api.example.com", "Target REST endpoint base URL"));
        params.add(new ConnectionParameter("credentialAlias", "Secure Parameter Alias", "secure-alias", false, "REST_CREDS", "Credential alias for API authentication"));
        params.add(new ConnectionParameter("resourcePath", "Resource Path", "string", true, "/v1/resource", "API path endpoint"));
        definition.setConnectionParameters(params);

        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("POST", true, "Send HTTP POST request"));
        ops.add(new OperationConfig("GET", false, "Send HTTP GET request"));
        ops.add(new OperationConfig("PUT", false, "Send HTTP PUT request"));
        definition.setDefaultOperations(ops);
    }

    @Override
    public TechnologyDefinition getDefinition() {
        return definition;
    }

    @Override
    public List<TemplateDefinition> getTemplates(AdapterSpecification spec) {
        return new ArrayList<>(); // Legacy plugins fallback to generic templates inside AdapterGeneratorService
    }

    @Override
    public Map<String, String> getPluginContext(AdapterSpecification spec) {
        return new HashMap<>();
    }
}
