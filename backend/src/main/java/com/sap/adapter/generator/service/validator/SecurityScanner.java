package com.sap.adapter.generator.service.validator;

public class SecurityScanner {
    
    /**
     * Scans generated file content for hardcoded credentials.
     * This is a generic technology-agnostic security check.
     */
    public static boolean containsHardcodedCredentials(String content) {
        if (content == null) {
            return false;
        }
        return content.contains("private_key") || 
               content.contains("-----BEGIN PRIVATE KEY-----") || 
               content.contains("password=") || 
               content.contains("apiKey=");
    }
}
