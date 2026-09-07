package com.sap.adapter.generator.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageMeta {
    private String inputFormat = "JSON";
    private String outputFormat = "TEXT";

    public MessageMeta() {}

    public MessageMeta(String inputFormat, String outputFormat) {
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
}
