package com.sap.adapter.generator.model.tech;

import com.sap.adapter.generator.model.spec.ConnectionParameter;
import com.sap.adapter.generator.model.spec.OperationConfig;

import java.util.ArrayList;
import java.util.List;

public class TechnologyDefinition {
    private String id;
    private String displayName;
    private String category;
    private List<String> supportedDirections = new ArrayList<>();
    private List<String> authenticationModels = new ArrayList<>();
    private List<ConnectionParameter> connectionParameters = new ArrayList<>();
    private List<OperationConfig> defaultOperations = new ArrayList<>();
    private String defaultScheme;
    private String description;

    public TechnologyDefinition() {}

    public TechnologyDefinition(String id, String displayName, String category, String defaultScheme, String description) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.defaultScheme = defaultScheme;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getSupportedDirections() { return supportedDirections; }
    public void setSupportedDirections(List<String> supportedDirections) { this.supportedDirections = supportedDirections; }

    public List<String> getAuthenticationModels() { return authenticationModels; }
    public void setAuthenticationModels(List<String> authenticationModels) { this.authenticationModels = authenticationModels; }

    public List<ConnectionParameter> getConnectionParameters() { return connectionParameters; }
    public void setConnectionParameters(List<ConnectionParameter> connectionParameters) { this.connectionParameters = connectionParameters; }

    public List<OperationConfig> getDefaultOperations() { return defaultOperations; }
    public void setDefaultOperations(List<OperationConfig> defaultOperations) { this.defaultOperations = defaultOperations; }

    public String getDefaultScheme() { return defaultScheme; }
    public void setDefaultScheme(String defaultScheme) { this.defaultScheme = defaultScheme; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
