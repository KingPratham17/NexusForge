package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)  // Accept any extra fields AI may include
public class ConnectionParameter {
    private String name;
    private String label;
    private String type; // string, secure-alias, select, boolean, integer
    private java.util.List<String> options;
    private boolean required;

    @JsonAlias({"default", "defaultValue"})  // Claude returns "default", we use "defaultValue"
    @JsonProperty("defaultValue")
    private String defaultValue;
    private String description;

    public ConnectionParameter() {}

    public ConnectionParameter(String name, String label, String type, boolean required, String defaultValue, String description) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.util.List<String> getOptions() { return options; }
    public void setOptions(java.util.List<String> options) { this.options = options; }
}
