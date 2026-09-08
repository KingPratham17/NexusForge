package com.sap.adapter.generator.service.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.adapter.generator.model.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI Requirement Parser — powered by Claude AI via local Omniroute proxy.
 *
 * Dynamically analyzes ANY connectivity requirement for ANY target technology
 * and generates:
 *   1. Target system metadata & connection parameters
 *   2. Target SDK Maven <dependency> XML tags for pom.xml
 *   3. OSGi !import package exclusions for OSGi bundle embedding
 *   4. Java SDK imports & execution code for Camel Producer
 */
@Service
public class AiRequirementParserService {

    private static final Logger LOG = LoggerFactory.getLogger(AiRequirementParserService.class);

    @Value("${ai.llm.enabled:true}")
    private boolean llmEnabled;

    @Value("${ai.llm.provider:anthropic}")
    private String llmProvider;

    @Value("${ai.llm.base-url:http://localhost:20128}")
    private String baseUrl;

    @Value("${ai.llm.auth-token:}")
    private String authToken;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:Pratham}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> parseRequirement(String userPrompt, String ignoredTechId) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return errorResult("Please provide a requirement description.");
        }

        String token = getEffectiveToken();

        if (llmEnabled && token != null && !token.isBlank()) {
            try {
                LOG.info("Calling Claude AI (model={}, proxy={}) for dynamic requirement analysis & SDK code generation...", modelName, baseUrl);
                AdapterSpecification llmSpec = callClaudeAi(userPrompt.trim(), token);
                if (llmSpec != null) {
                    LOG.info("Claude AI successfully generated specification for target system '{}' (scheme={})",
                            llmSpec.getTarget() != null ? llmSpec.getTarget().getTechnology() : "custom",
                            llmSpec.getAdapter().getScheme());

                    Map<String, Object> result = new HashMap<>();
                    result.put("specification", llmSpec);
                    result.put("technology", buildTechSummary(llmSpec));
                    result.put("missingFields", List.of());
                    result.put("parsedPromptSummary",
                            "Claude AI dynamically analyzed your requirement and generated target Maven SDK dependencies, OSGi manifest rules, connection parameters, and Java producer code for: " +
                            (llmSpec.getTarget() != null ? llmSpec.getTarget().getTechnology() : "Target System"));
                    result.put("aiEngine", "Claude AI (" + modelName + " @ " + baseUrl + ")");
                    return result;
                }
            } catch (Exception e) {
                LOG.warn("Claude AI call failed: {}. Falling back to dynamic spec generator.", e.getMessage());
            }
        } else {
            LOG.info("AI token not configured — using dynamic spec generator.");
        }

        return buildDynamicFallback(userPrompt);
    }

    public TargetMeta autoFixRequirement(String errorLog, AdapterSpecification currentSpec) {
        String token = getEffectiveToken();
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            LOG.info("Calling Claude AI to analyze build errors and auto-fix specification for {}...",
                    currentSpec != null && currentSpec.getAdapter() != null ? currentSpec.getAdapter().getName() : "CustomAdapter");

            String prompt = buildAutoFixPrompt(errorLog, currentSpec);

            String endpoint = baseUrl.endsWith("/") ? baseUrl + "v1/messages" : baseUrl + "/v1/messages";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", token);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(45000);
            conn.setDoOutput(true);

            Map<String, Object> body = Map.of(
                    "model", modelName,
                    "max_tokens", 4096,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            byte[] payload = objectMapper.writeValueAsBytes(body);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int status = conn.getResponseCode();
            LOG.info("Claude AI Auto-Fix proxy status: {}", status);
            if (status == 200) {
                String respStr = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(respStr);
                String text = "";
                JsonNode content = root.path("content");
                if (content.isArray() && !content.isEmpty()) {
                    text = content.get(0).path("text").asText();
                }
                if (text.isBlank()) {
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        text = choices.get(0).path("message").path("content").asText();
                    }
                }
                if (!text.isBlank()) {
                    text = stripJsonMarkdown(text);
                    TargetMeta fixed = objectMapper.readValue(text.trim(), TargetMeta.class);
                    LOG.info("Claude AI successfully returned auto-fix TargetMeta: tech={}, imports={}, implLength={}",
                            fixed.getTechnology(), fixed.getProducerImports(), fixed.getProducerImplementation() != null ? fixed.getProducerImplementation().length() : 0);
                    return fixed;
                }
            }
        } catch (Exception e) {
            LOG.warn("Claude AI Auto-Fix failed: {}", e.getMessage());
        }
        return applyHeuristicAutoFix(errorLog, currentSpec);
    }

    public TargetMeta applyHeuristicAutoFix(String errorLog, AdapterSpecification currentSpec) {
        if (currentSpec == null || currentSpec.getTarget() == null) {
            return null;
        }
        TargetMeta target = currentSpec.getTarget();
        String deps = target.getCustomDependencies() != null ? target.getCustomDependencies() : "";
        String imports = target.getProducerImports() != null ? target.getProducerImports() : "";
        String impl = target.getProducerImplementation() != null ? target.getProducerImplementation() : "";

        boolean modified = false;

        // 1. Fix invalid com.force.api groupId or missing Salesforce SDK
        if (deps.contains("com.force.api") || (errorLog != null && errorLog.contains("force-rest-api"))) {
            deps = deps.replaceAll("<groupId>com\\.force\\.api</groupId>", "<groupId>com.frejo</groupId>");
            if (!deps.contains("httpclient")) {
                deps += "\n    <dependency>\n      <groupId>org.apache.httpcomponents</groupId>\n      <artifactId>httpclient</artifactId>\n      <version>4.5.14</version>\n    </dependency>";
            }
            modified = true;
        }

        // 1b. Fix invalid Google Drive version hallucination (e.g. non-existent v3-rev20240815)
        if (deps.contains("google-api-services-drive") || (errorLog != null && errorLog.contains("google-api-services-drive"))) {
            if (deps.contains("v3-rev20240815") || (errorLog != null && errorLog.contains("v3-rev20240815"))) {
                deps = deps.replaceAll("v3-rev20240815-[^<]+", "v3-rev20240809-2.0.0");
                modified = true;
            } else if (errorLog != null && errorLog.contains("Could not resolve dependencies")) {
                deps = deps.replaceAll("(?s)(<artifactId>google-api-services-drive</artifactId>\\s*<version>)[^<]+(</version>)", "$1v3-rev20240809-2.0.0$2");
                modified = true;
            }
        }

        // 2. Strip duplicate jackson-databind if in customDependencies
        if (deps.contains("jackson-databind")) {
            deps = deps.replaceAll("(?s)<dependency>\\s*<groupId>com\\.fasterxml\\.jackson\\.core</groupId>\\s*<artifactId>jackson-databind</artifactId>.*?</dependency>", "");
            modified = true;
        }

        // 3. Fix missing common imports reported in errorLog or implementation
        if (errorLog != null || impl != null) {
            if ((errorLog != null && errorLog.contains("IOException")) && !imports.contains("java.io.IOException")) {
                imports += "\nimport java.io.IOException;";
                modified = true;
            }
            if ((errorLog != null && errorLog.contains("GeneralSecurityException")) && !imports.contains("GeneralSecurityException")) {
                imports += "\nimport java.security.GeneralSecurityException;";
                modified = true;
            }
            if (((errorLog != null && errorLog.contains("ApiFuture")) || (impl != null && impl.contains("ApiFuture"))) && !imports.contains("ApiFuture")) {
                imports += "\nimport com.google.api.core.ApiFuture;";
                modified = true;
            }
            if (((errorLog != null && errorLog.contains("ExecutionException")) || (impl != null && impl.contains("ExecutionException"))) && !imports.contains("ExecutionException")) {
                imports += "\nimport java.util.concurrent.ExecutionException;";
                modified = true;
            }
        }

        // 4. Fix common Java code hallucinations (e.g., duplicate variables, this.* references)
        if (errorLog != null && errorLog.contains("cannot find symbol") && impl.contains("this.instanceUrl")) {
            impl = impl.replace("this.instanceUrl", "instanceUrl");
            modified = true;
        }
        if (errorLog != null && errorLog.contains("is already defined in method process") && impl.contains("String clientSecret =")) {
            // Replace the second declaration with an assignment if it already exists
            impl = impl.replaceFirst("(?s)String\\s+clientSecret\\s*=\\s*([^;]+;)(.*?)String\\s+clientSecret\\s*=", "String clientSecret = $1$2clientSecret =");
            modified = true;
        }
        if (errorLog != null && errorLog.contains("is already defined in method process") && impl.contains("String token =")) {
            impl = impl.replaceFirst("(?s)String\\s+token\\s*=\\s*([^;]+;)(.*?)String\\s+token\\s*=", "String token = $1$2token =");
            modified = true;
        }

        if (modified) {
            LOG.info("Heuristic auto-fix applied successfully for target: {}", target.getTechnology());
            TargetMeta fixed = new TargetMeta();
            fixed.setTechnology(target.getTechnology());
            fixed.setCategory(target.getCategory());
            fixed.setCustomDependencies(deps);
            fixed.setExcludedImports(target.getExcludedImports());
            fixed.setProducerImports(imports);
            fixed.setProducerImplementation(impl);
            return fixed;
        }

        return target;
    }

    private String buildAutoFixPrompt(String errorLog, AdapterSpecification currentSpec) {
        String tech = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getTechnology() : "Target System";
        String currentImpl = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getProducerImplementation() : "";
        String currentDeps = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getCustomDependencies() : "";
        String currentImports = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getProducerImports() : "";

        return """
                You are an expert SAP ADK and Java compiler engineer.
                A Maven build failed for adapter project '%s'.
                
                --- MAVEN BUILD COMPILATION ERROR LOG ---
                %s
                --- END ERROR LOG ---

                --- CURRENT TARGET METADATA ---
                Technology: %s
                Producer Imports: %s
                Custom Dependencies: %s
                Producer Implementation: %s
                --- END CURRENT TARGET METADATA ---

                Analyze the exact compilation/build error.
                Fix missing imports, invalid method calls, duplicate variable declarations, or missing Maven dependencies.
                
                CRITICAL RULES:
                1. If a dependency failed to download ("Could not find artifact ... in central"), verify the exact groupId/artifactId or replace it with standard Apache HttpClient ('org.apache.httpcomponents:httpclient:4.5.14'). For Salesforce REST, use 'com.frejo:force-rest-api:0.0.45' or standard Apache HttpClient. Never use 'com.force.api:force-rest-api'. For Google Drive API, use known version 'v3-rev20240809-2.0.0'. Never hallucinate unreleased dates like 'v3-rev20240815-2.0.0'.
                2. Do not include jackson-databind or slf4j in customDependencies (they are already provided by the base POM).
                3. Ensure all Java classes used in producerImplementation (e.g. IOException) are explicitly imported in producerImports.
                
                Return ONLY valid JSON matching this structure (no markdown, no preamble):
                {
                  "technology": "%s",
                  "category": "custom",
                  "customDependencies": "<corrected pom.xml dependencies>",
                  "excludedImports": "<corrected OSGi exclusions>",
                  "producerImports": "<corrected Java imports>",
                  "producerImplementation": "<corrected Java producer code>"
                }
                """.formatted(
                        currentSpec != null && currentSpec.getAdapter() != null ? currentSpec.getAdapter().getName() : "CustomAdapter",
                        errorLog != null && errorLog.length() > 3000 ? errorLog.substring(errorLog.length() - 3000) : errorLog,
                        tech,
                        currentImports,
                        currentDeps,
                        currentImpl,
                        tech
                );
    }

    private AdapterSpecification callClaudeAi(String userPrompt, String token) throws Exception {
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "v1/messages" : baseUrl + "/v1/messages";
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", token);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);

        String systemPrompt = buildSystemPrompt(userPrompt);

        Map<String, Object> body = Map.of(
                "model", modelName,
                "max_tokens", 4096,
                "messages", List.of(Map.of("role", "user", "content", systemPrompt))
        );

        byte[] payload = objectMapper.writeValueAsBytes(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        LOG.info("Claude AI proxy HTTP status: {}", status);

        if (status == 200) {
            String respStr = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return extractSpecFromResponse(respStr);
        }

        String err = conn.getErrorStream() != null
                ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                : "(no error body)";
        LOG.warn("Claude AI returned HTTP {}: {}", status, err);
        return null;
    }

    private String buildSystemPrompt(String userPrompt) {
        return """
                You are an expert SAP Cloud Integration ADK architect and Apache Camel developer.
                Analyze the user's natural language requirement for building a custom SAP Cloud Integration adapter.

                Generate a complete, dynamic AdapterSpecification JSON.
                Infer the exact target technology (e.g. Cassandra, Firebase, MongoDB, AWS S3, Redis, Snowflake, Oracle, Salesforce, Kafka, SFTP, etc.), its required connection parameters, its Maven SDK dependencies, its OSGi import exclusions, and its Java Producer processing logic.

                Output ONLY valid JSON with this EXACT structure (no markdown code blocks, no text before or after):
                {
                  "adapter": {
                    "name": "<PascalCase adapter name, e.g. CassandraReceiverAdapter>",
                    "symbolicName": "<com.poc.targetadapter>",
                    "vendor": "Custom",
                    "version": "1.0.0",
                    "direction": "<sender or receiver>",
                    "scheme": "<lowercase-hyphenated-scheme, e.g. cassandra-custom>",
                    "packagePath": "<com.poc.targetadapter>"
                  },
                  "target": {
                    "technology": "<Exact target system name, e.g. Apache Cassandra NoSQL>",
                    "category": "<database, cloud-storage, rest-api, event-broker, file-transfer>",
                    "customDependencies": "    <dependency>\\n      <groupId>com.google.cloud</groupId>\\n      <artifactId>google-cloud-firestore</artifactId>\\n      <version>3.7.0</version>\\n    </dependency>",
                    "excludedImports": "              !com.google.cloud.*,\\n              !com.google.auth.*,",
                    "producerImports": "import com.google.cloud.firestore.*;\\nimport com.google.auth.oauth2.*;",
                    "producerImplementation": "        LOG.info(\\"Executing target processing logic for payload\\");"
                  },
                  "authentication": {
                    "type": "secure-parameter",
                    "credentialAliasProperty": "credentialAlias"
                  },
                  "connection": {
                    "parameters": [
                      {
                        "name": "<camelCaseParamName>",
                        "label": "<Human Readable Label>",
                        "type": "<string, integer, boolean, secure-alias>",
                        "required": true,
                        "defaultValue": "<sensible placeholder value>",
                        "description": "<one sentence parameter description>"
                      }
                    ]
                  },
                  "operations": [
                    { "name": "<OPERATION_NAME>", "enabled": true, "description": "<operation description>" }
                  ],
                  "runtime": {
                    "camelVersion": "3.14.7",
                    "adkVersion": "2.2.0",
                    "javaTarget": "1.8"
                  }
                }

                Critical Rules:
                1. customDependencies: Generate ONLY raw target SDK <dependency> XML elements — the actual Java client libraries published on Maven Central.
                   NEVER include any org.apache.camel:camel-* dependencies. The Camel core is already provided by the SAP ADK parent pom.
                   Use STABLE, verified release versions that actually exist on Maven Central. Do NOT hallucinate version numbers.
                2. excludedImports: List all root package prefixes of the embedded SDK prefixed with ! and suffixed with .* (e.g. !com.example.sdk.*).
                3. producerImports: Include all necessary import statements for SDK classes used in producerImplementation. NEVER import org.apache.camel.* — they are already available.
                4. producerImplementation:
                   - ROLE: You are writing code for a Camel Producer (Receiver Adapter in SAP CI architecture). The Producer's ONLY job is to PUBLISH / SEND / WRITE the incoming Camel `body` payload to the target system.
                     NEVER write subscription or listener logic (`client.subscribe(...)`, `client.setCallback(...)`, `KafkaConsumer`, etc.). Always call publish/send/put methods.
                   - URI PROTOCOL: Connection URLs (e.g. `brokerUrl`, `serverUrl`) must be checked for protocol schemes. If `!brokerUrl.contains("://")`, prepend `"tcp://"` (e.g. `String serverURI = brokerUrl.contains("://") ? brokerUrl : "tcp://" + brokerUrl;`).
                   - METHOD VARIABLES: The following variables ALREADY exist in method scope and must NOT be re-declared:
                     * `exchange` (Camel Exchange), `endpoint` (Adapter Endpoint)
                     * `body` (String — raw payload, use `body.getBytes(StandardCharsets.UTF_8)`)
                     * `data` (Map<String, Object> — parsed JSON map)
                     * `LOG` (SLF4J Logger)
                     * Connection parameters as String variables (e.g. `brokerUrl`, `topic`, `qos`)
                   - CREDENTIALS (ZERO RAW CREDENTIALS POLICY): If the target system requires authentication (username, password, client_id, client_secret, API key), DO NOT declare them as connection parameters! Instead, declare a SINGLE connection parameter named `credentialAlias` of type `secure-alias`. Then, inside `producerImplementation`, retrieve the credentials dynamically using the SAP SecureStore API:
                     For Username/Password:
                     `com.sap.it.api.securestore.UserCredential cred = com.sap.it.api.ITApiFactory.getApi(com.sap.it.api.securestore.SecureStoreService.class, null).getUserCredential(endpoint.getCredentialAlias());`
                     `String username = cred.getUsername(); String password = new String(cred.getPassword());`
                     For ClientID/Secret:
                     `com.sap.it.api.securestore.OAuth2ClientCredential oauth = com.sap.it.api.ITApiFactory.getApi(com.sap.it.api.securestore.SecureStoreService.class, null).getOAuth2ClientCredential(endpoint.getCredentialAlias());`
                     `String clientId = oauth.getClientId(); String clientSecret = new String(oauth.getClientSecret());`
                     For API Key/Token:
                     `com.sap.it.api.securestore.UserCredential cred = com.sap.it.api.ITApiFactory.getApi(com.sap.it.api.securestore.SecureStoreService.class, null).getUserCredential(endpoint.getCredentialAlias());`
                     `String token = new String(cred.getPassword());`
                   - CLEANUP: Always disconnect and close client instances before returning from `process()`.
                5. Connection parameters: Generate 3-6 parameters specific to the target technology. For any boolean flag parameters (e.g. `cleanSession`, `useSsl`, `autoReconnect`), set `"type": "boolean"` and `"defaultValue": "true"` or `"false"` so SAP Integration Suite UI renders a native Checkbox control.
                6. ZERO-RAW-CREDENTIALS POLICY (MANDATORY): In SAP Cloud Integration (CPI), NEVER ask for raw passwords, raw private keys, or raw JSON files. All authentication MUST use a Credential Alias parameter (`"type": "secure-alias"`, `"name": "credentialAlias"`, `"label": "Credential Alias"`, `"defaultValue": "SAP_SECURE_ALIAS"`, `"description": "Deployed Security Material alias in SAP Cloud Integration").

                User Requirement Prompt: """ + userPrompt;
    }

    private AdapterSpecification extractSpecFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        String text = "";
        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            text = content.get(0).path("text").asText();
        }

        if (text.isBlank()) {
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                text = choices.get(0).path("message").path("content").asText();
            }
        }

        if (text.isBlank()) return null;

        text = stripJsonMarkdown(text);
        AdapterSpecification spec = objectMapper.readValue(text.trim(), AdapterSpecification.class);

        // Normalize parameters: enforce Zero Raw Credentials Policy
        if (spec != null && spec.getConnection() != null && spec.getConnection().getParameters() != null) {
            for (ConnectionParameter p : spec.getConnection().getParameters()) {
                String name = p.getName() != null ? p.getName().toLowerCase() : "";
                boolean isSensitive = "secure-alias".equalsIgnoreCase(p.getType())
                        || name.contains("password")
                        || name.contains("secret")
                        || name.contains("key")
                        || name.contains("token")
                        || name.contains("credential")
                        || name.contains("serviceaccount");
                if (isSensitive) {
                    p.setType("secure-alias");
                    if (p.getDefaultValue() == null || p.getDefaultValue().isBlank()) {
                        p.setDefaultValue("SAP_SECURE_ALIAS");
                    }
                    if (p.getDescription() == null || p.getDescription().isBlank() || !p.getDescription().toLowerCase().contains("alias")) {
                        p.setDescription("SAP Cloud Integration Security Material alias (Zero Raw Credentials Policy)");
                    }
                }
            }
        }

        return spec;
    }

    private Map<String, Object> buildDynamicFallback(String userPrompt) {
        AdapterSpecification spec = new AdapterSpecification();

        String promptLower = userPrompt.toLowerCase();
        String techName = inferTechnologyName(userPrompt);
        String schemeName = techName.toLowerCase().replaceAll("[^a-z0-9]", "") + "-custom";
        String adapterClassName = toPascalCase(techName) + "Adapter";

        AdapterMeta meta = new AdapterMeta();
        meta.setName(adapterClassName);
        meta.setSymbolicName("com.poc." + schemeName.replace("-", ""));
        meta.setVendor("Custom");
        meta.setVersion("1.0.0");
        meta.setDirection(promptLower.contains("sender") ? "sender" : "receiver");
        meta.setScheme(schemeName);
        meta.setPackagePath("com.poc." + schemeName.replace("-", ""));
        spec.setAdapter(meta);

        TargetMeta target = new TargetMeta(techName, "custom");
        target.setCustomDependencies("    <!-- Target SDK dependencies dynamically requested for " + techName + " -->");
        target.setExcludedImports("");
        target.setProducerImports("");
        target.setProducerImplementation("        LOG.info(\"Processing exchange message body payload for " + techName + "\");");
        spec.setTarget(target);

        spec.setAuthentication(new AuthenticationMeta("secure-parameter", "credentialAlias"));
        spec.setRuntime(new RuntimeMeta("3.14.7", "2.2.0", "1.8"));

        List<ConnectionParameter> params = new ArrayList<>();
        params.add(new ConnectionParameter("endpointUrl", "Endpoint URL / Host", "string", true, "https://target.example.com", "Target system connection URL or host"));
        params.add(new ConnectionParameter("credentialAlias", "Credential Alias", "secure-alias", true, "SECURE_CREDS", "SAP Secure Parameter Store alias"));
        params.add(new ConnectionParameter("targetResource", "Target Resource / Table / Collection", "string", true, "default_resource", "Target payload destination"));
        ConnectionMeta conn = new ConnectionMeta();
        conn.setParameters(params);
        spec.setConnection(conn);

        spec.setOperations(List.of(new OperationConfig("EXECUTE", true, "Execute operation on " + techName)));
        spec.setMessage(new MessageMeta("JSON", "TEXT"));

        Map<String, Object> result = new HashMap<>();
        result.put("specification", spec);
        result.put("technology", buildTechSummary(spec));
        result.put("missingFields", List.of());
        result.put("parsedPromptSummary", "Dynamically created specification for: " + techName);
        result.put("aiEngine", "Dynamic Requirement Analyzer");
        return result;
    }

    private String inferTechnologyName(String prompt) {
        if (prompt == null || prompt.isBlank()) return "Custom Target System";
        String p = prompt.trim();
        String[] words = p.split("\\s+");
        if (words.length <= 4) return p;
        return words[0] + " " + words[1] + " " + words[2];
    }

    private String toPascalCase(String text) {
        if (text == null || text.isBlank()) return "Custom";
        StringBuilder sb = new StringBuilder();
        for (String word : text.split("[^a-zA-Z0-9]+")) {
            if (!word.isBlank()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private Map<String, Object> buildTechSummary(AdapterSpecification spec) {
        Map<String, Object> tech = new LinkedHashMap<>();
        tech.put("id", spec.getAdapter().getScheme());
        tech.put("displayName", spec.getTarget().getTechnology());
        tech.put("category", spec.getTarget().getCategory());
        tech.put("description", "Dynamically generated specification for " + spec.getTarget().getTechnology());
        return tech;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", message);
        result.put("specification", null);
        result.put("aiEngine", "None");
        return result;
    }

    private String getEffectiveToken() {
        if (authToken != null && !authToken.isBlank()) return authToken.trim();
        if (apiKey != null && !apiKey.isBlank()) return apiKey.trim();
        return null;
    }

    private String stripJsonMarkdown(String text) {
        if (text == null) return "";
        text = text.strip();
        if (text.startsWith("```json")) {
            text = text.substring(7);
            int end = text.lastIndexOf("```");
            if (end != -1) text = text.substring(0, end);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
            int end = text.lastIndexOf("```");
            if (end != -1) text = text.substring(0, end);
        }
        return text.strip();
    }
}
