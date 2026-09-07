package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetMeta {
    private String technology = "Custom Target System";
    private String category = "custom";
    private String customDependencies = "";
    private String excludedImports = "";
    private String producerImports = "";
    private String producerImplementation = "";

    public TargetMeta() {}

    public TargetMeta(String technology, String category) {
        this.technology = technology;
        this.category = category;
    }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCustomDependencies() { return customDependencies; }
    public void setCustomDependencies(String customDependencies) { this.customDependencies = customDependencies; }

    public String getExcludedImports() { return excludedImports; }
    public void setExcludedImports(String excludedImports) { this.excludedImports = excludedImports; }

    public String getProducerImports() { return producerImports; }
    public void setProducerImports(String producerImports) { this.producerImports = producerImports; }

    public String getProducerImplementation() { return producerImplementation; }
    public void setProducerImplementation(String producerImplementation) { this.producerImplementation = producerImplementation; }
}
