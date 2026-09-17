package com.sap.adapter.generator.plugin;

public class TemplateDefinition {
    private String sourcePath;
    private String destinationPath;
    
    public TemplateDefinition(String sourcePath, String destinationPath) {
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }
    
    public String getSourcePath() { return sourcePath; }
    public String getDestinationPath() { return destinationPath; }
}
