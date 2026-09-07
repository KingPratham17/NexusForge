package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeMeta {
    private String camelVersion = "3.14.7";
    private String adkVersion = "2.2.0";
    private String javaTarget = "1.8";

    public RuntimeMeta() {}

    public RuntimeMeta(String camelVersion, String adkVersion, String javaTarget) {
        this.camelVersion = camelVersion;
        this.adkVersion = adkVersion;
        this.javaTarget = javaTarget;
    }

    public String getCamelVersion() { return camelVersion; }
    public void setCamelVersion(String camelVersion) { this.camelVersion = camelVersion; }

    public String getAdkVersion() { return adkVersion; }
    public void setAdkVersion(String adkVersion) { this.adkVersion = adkVersion; }

    public String getJavaTarget() { return javaTarget; }
    public void setJavaTarget(String javaTarget) { this.javaTarget = javaTarget; }
}
