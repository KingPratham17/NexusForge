package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionMeta {
    private List<ConnectionParameter> parameters = new ArrayList<>();

    public ConnectionMeta() {}

    public ConnectionMeta(List<ConnectionParameter> parameters) {
        this.parameters = parameters;
    }

    public List<ConnectionParameter> getParameters() { return parameters; }
    public void setParameters(List<ConnectionParameter> parameters) { this.parameters = parameters; }
}
