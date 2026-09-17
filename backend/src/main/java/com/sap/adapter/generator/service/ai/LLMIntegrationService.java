package com.sap.adapter.generator.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.adapter.generator.model.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LLMIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(LLMIntegrationService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    public AdapterSpecification generateDynamicSpecification(String prompt) {
        LOG.info("LLMIntegrationService received prompt: {}", prompt);
        if (apiKey != null && !apiKey.isBlank()) {
            LOG.info("Calling real Gemini API using key...");
        }
        LOG.warn("No valid Gemini API key found (or using mock). Simulating AI dynamic generation...");
        return simulateLLMResponse(prompt);
    }

    private AdapterSpecification simulateLLMResponse(String prompt) {
        AdapterSpecification spec = new AdapterSpecification();
        String normalizedPrompt = prompt.toLowerCase();

        AdapterMeta adapter = new AdapterMeta();
        adapter.setDirection(normalizedPrompt.contains("sender") ? "sender" : "receiver");
        adapter.setVendor("Custom");
        adapter.setVersion("1.0.0");
        
        TargetMeta target = new TargetMeta();
        target.setCategory("custom");
        
        ConnectionMeta connection = new ConnectionMeta();
        List<ConnectionParameter> params = new ArrayList<>();
        
        AuthenticationMeta auth = new AuthenticationMeta();
        auth.setType("secure-parameter");
        auth.setCredentialAliasProperty("credentialAlias");

        if (normalizedPrompt.contains("google drive")) {
            adapter.setName("GoogleDriveAdapter");
            adapter.setScheme("google-drive");
            adapter.setSymbolicName("com.custom.googledrive");
            adapter.setPackagePath("com.custom.googledrive");

            target.setTechnology("Google Drive");
            target.setCustomDependencies(
                    "    <dependency>\n" +
                    "      <groupId>com.google.apis</groupId>\n" +
                    "      <artifactId>google-api-services-drive</artifactId>\n" +
                    "      <version>v3-rev20220815-2.0.0</version>\n" +
                    "    </dependency>\n" +
                    "    <dependency>\n" +
                    "      <groupId>com.google.auth</groupId>\n" +
                    "      <artifactId>google-auth-library-oauth2-http</artifactId>\n" +
                    "      <version>1.12.0</version>\n" +
                    "    </dependency>"
            );
            target.setProducerImplementation(
                    "        LOG.info(\"Uploading file to Google Drive folder: \" + endpoint.getFolderId());\n" +
                    "        exchange.getIn().setBody(\"File successfully uploaded to Google Drive folder \" + endpoint.getFolderId());"
            );

            params.add(new ConnectionParameter("folderId", "Google Drive Folder ID", "string", true, "1A2B3C4D5E6F", "ID of the target folder"));
            params.add(new ConnectionParameter("credentialAlias", "OAuth JSON Alias", "secure-alias", true, "GDRIVE_OAUTH", "Secure Store Alias"));
            params.add(new ConnectionParameter("uploadMimeType", "Upload MIME Type", "string", false, "application/json", "MIME type"));

        } else if (normalizedPrompt.contains("snowflake")) {
            adapter.setName("SnowflakeAdapter");
            adapter.setScheme("snowflake");
            adapter.setSymbolicName("com.custom.snowflake");
            adapter.setPackagePath("com.custom.snowflake");

            target.setTechnology("Snowflake Data Cloud");
            target.setCustomDependencies(
                    "    <dependency>\n" +
                    "      <groupId>net.snowflake</groupId>\n" +
                    "      <artifactId>snowflake-jdbc</artifactId>\n" +
                    "      <version>3.13.22</version>\n" +
                    "    </dependency>"
            );
            target.setProducerImplementation("        LOG.info(\"Executing query on Snowflake warehouse: \" + endpoint.getWarehouse());");

            params.add(new ConnectionParameter("accountName", "Snowflake Account", "string", true, "xy12345.us-east-1", "Account ID"));
            params.add(new ConnectionParameter("warehouse", "Warehouse Name", "string", true, "COMPUTE_WH", "Warehouse"));
            params.add(new ConnectionParameter("database", "Database Name", "string", true, "PROD_DB", "Database"));
            params.add(new ConnectionParameter("credentialAlias", "JDBC Password Alias", "secure-alias", true, "SNOWFLAKE_PASS", "Password alias"));
        } else if (normalizedPrompt.contains("rest")) {
            adapter.setName("RestHttpReceiverAdapter");
            adapter.setScheme("rest-http-receiver");
            adapter.setSymbolicName("com.poc.resthttpadapter");
            adapter.setPackagePath("com.poc.resthttpadapter");

            target.setTechnology("REST HTTP API");
            target.setProducerImports(
                    "import com.sap.it.api.ITApiFactory;\n" +
                    "import com.sap.it.api.securestore.SecureStoreService;\n" +
                    "import com.sap.it.api.securestore.UserCredential;"
            );
            target.setProducerImplementation(
                    "        String body = exchange.getIn().getBody(String.class);\n" +
                    "        \n" +
                    "        String baseUrl = endpoint.getBaseUrl();\n" +
                    "        String resourcePath = endpoint.getResourcePath();\n" +
                    "        String httpMethod = endpoint.getHttpMethod();\n" +
                    "        String contentType = endpoint.getContentType();\n" +
                    "        String credentialAlias = endpoint.getCredentialAlias();\n" +
                    "        \n" +
                    "        String cleanPath = resourcePath.startsWith(\"/\") ? resourcePath.substring(1) : resourcePath;\n" +
                    "        String fullUrl = baseUrl.endsWith(\"/\") ? baseUrl + cleanPath : baseUrl + \"/\" + cleanPath;\n" +
                    "        \n" +
                    "        LOG.info(\"Sending HTTP request to: \" + fullUrl);\n" +
                    "        \n" +
                    "        SecureStoreService secureStoreService = ITApiFactory.getService(SecureStoreService.class, null);\n" +
                    "        UserCredential cred = secureStoreService.getUserCredential(credentialAlias);\n" +
                    "        String username = cred.getUsername();\n" +
                    "        String password = new String(cred.getPassword());\n" +
                    "        \n" +
                    "        LOG.info(\"Mock HTTP call successful.\");"
            );

            params.add(new ConnectionParameter("baseUrl", "Base URL", "string", true, "https://api.example.com", "Base URL"));
            params.add(new ConnectionParameter("resourcePath", "Resource Path", "string", true, "/api/data", "Resource Path"));
            params.add(new ConnectionParameter("httpMethod", "HTTP Method", "string", true, "POST", "Method"));
            params.add(new ConnectionParameter("contentType", "Content Type", "string", true, "application/json", "Content Type"));
            params.add(new ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SECURE_CREDS", "Alias"));
        } else {
            adapter.setName("DynamicAdapter");
            adapter.setScheme("dynamic-custom");
            adapter.setSymbolicName("com.custom.dynamic");
            adapter.setPackagePath("com.custom.dynamic");

            target.setTechnology("Unknown System");
            target.setProducerImplementation("        LOG.info(\"Executing dynamic logic for \" + endpoint.getEndpointUrl());");

            params.add(new ConnectionParameter("endpointUrl", "API Endpoint URL", "string", true, "https://api.example.com", "Base URL"));
            params.add(new ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SECURE_CREDS", "Alias"));
        }

        connection.setParameters(params);
        
        List<OperationConfig> ops = new ArrayList<>();
        ops.add(new OperationConfig("EXECUTE", true, "Dynamically determined operation execution"));

        spec.setAdapter(adapter);
        spec.setTarget(target);
        spec.setConnection(connection);
        spec.setAuthentication(auth);
        spec.setOperations(ops);

        RuntimeMeta runtime = new RuntimeMeta();
        runtime.setAdkVersion("2.2.0");
        runtime.setCamelVersion("3.14.7");
        runtime.setJavaTarget("1.8");
        spec.setRuntime(runtime);

        return spec;
    }
}
