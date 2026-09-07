package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationMeta {
    private String type = "secure-parameter";
    private String credentialAliasProperty = "credentialAlias";

    public AuthenticationMeta() {}

    public AuthenticationMeta(String type, String credentialAliasProperty) {
        this.type = type;
        this.credentialAliasProperty = credentialAliasProperty;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCredentialAliasProperty() { return credentialAliasProperty; }
    public void setCredentialAliasProperty(String credentialAliasProperty) { this.credentialAliasProperty = credentialAliasProperty; }
}
