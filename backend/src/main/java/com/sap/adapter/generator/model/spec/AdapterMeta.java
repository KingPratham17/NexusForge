package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdapterMeta {
    private String name = "CustomAdapter";
    private String symbolicName = "com.custom.adapter";
    private String vendor = "Custom";
    private String version = "1.0.0";
    private String direction = "receiver";
    private String scheme = "custom-adapter";
    private String packagePath = "com.custom.adapter";

    public AdapterMeta() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbolicName() { return symbolicName; }
    public void setSymbolicName(String symbolicName) { this.symbolicName = symbolicName; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }

    public String getPackagePath() { return packagePath; }
    public void setPackagePath(String packagePath) { this.packagePath = packagePath; }
}
