package com.sap.adapter.generator.service.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.adapter.generator.model.spec.*;
import com.sap.adapter.generator.registry.TechnologyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.adapter.generator.service.ai.LLMIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TechnologyRegistry registry;
    private final LLMIntegrationService llmService;

    @Autowired
    public AiRequirementParserService(TechnologyRegistry registry, LLMIntegrationService llmService) {
        this.registry = registry;
        this.llmService = llmService;
    }

    public Map<String, Object> parseRequirement(String userPrompt, String technologyId) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return errorResult("Please provide a requirement description.");
        }

        String token = getEffectiveToken();

        if (llmEnabled && token != null && !token.isBlank()) {
            try {
                LOG.info("Calling Claude AI (model={}, proxy={}) for dynamic requirement analysis & SDK code generation...", modelName, baseUrl);
                AdapterSpecification llmSpec = callClaudeAi(userPrompt.trim(), token, technologyId);
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

    public TargetMeta autoFixRequirement(String errorLog, AdapterSpecification currentSpec, java.nio.file.Path workspaceDir) {
        String fileContext = extractFailingSourceCode(errorLog, workspaceDir);
        if (fileContext == null || fileContext.isBlank()) {
            LOG.warn("Auto-Fix could not extract source context. Falling back to heuristic auto-fix.");
        }
        String prompt = buildAutoFixPrompt(errorLog, currentSpec, fileContext);
        TargetMeta resultTarget = currentSpec != null ? currentSpec.getTarget() : null;

        try {
            String token = getEffectiveToken();
            if (token == null || token.isBlank()) {
                LOG.warn("No API token found, skipping Claude AI Auto-Fix.");
                return applyHeuristicAutoFix(errorLog, currentSpec);
            }

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
                    resultTarget = fixed;
                }
            }
        } catch (Exception e) {
            LOG.warn("Claude AI Auto-Fix failed: {}", e.getMessage());
        }

        AdapterSpecification tempSpec = new AdapterSpecification();
        tempSpec.setTarget(resultTarget);
        return applyHeuristicAutoFix(errorLog, tempSpec);
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

    private String extractFailingSourceCode(String errorLog, java.nio.file.Path workspaceDir) {
        if (errorLog == null || workspaceDir == null) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("([a-zA-Z0-9_]+(Producer|Consumer)\\.java)");
            java.util.regex.Matcher m = p.matcher(errorLog);
            if (m.find()) {
                String fileName = m.group(1);
                LOG.info("Auto-Fix identified failing target implementation file: {}", fileName);
                
                java.nio.file.Path javaSrcDir = workspaceDir.resolve("src/main/java");
                if (java.nio.file.Files.exists(javaSrcDir)) {
                    java.util.Optional<java.nio.file.Path> foundFile = java.nio.file.Files.walk(javaSrcDir)
                            .filter(path -> java.nio.file.Files.isRegularFile(path) && path.getFileName().toString().equals(fileName))
                            .findFirst();
                            
                    if (foundFile.isPresent()) {
                        LOG.info("Auto-Fix reading source context from: {}", foundFile.get());
                        return java.nio.file.Files.readString(foundFile.get(), StandardCharsets.UTF_8);
                    } else {
                        LOG.warn("Auto-Fix could not find file {} in {}", fileName, javaSrcDir);
                    }
                }
            } else {
                LOG.warn("Auto-Fix could not identify a valid Producer or Consumer .java file in the error log.");
            }
        } catch (Exception e) {
            LOG.error("Failed to extract source code for Auto-Fix context: {}", e.getMessage());
        }
        return "";
    }

    private String buildAutoFixPrompt(String errorLog, AdapterSpecification currentSpec, String fileContext) {
        String tech = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getTechnology() : "Target System";
        String currentImpl = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getProducerImplementation() : "";
        String currentConsumerImpl = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getConsumerImplementation() : "";
        String currentDeps = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getCustomDependencies() : "";
        String currentImports = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getProducerImports() : "";
        String currentConsumerImports = currentSpec != null && currentSpec.getTarget() != null ? currentSpec.getTarget().getConsumerImports() : "";

        String specParams = "";
        try {
            if (currentSpec != null && currentSpec.getConnection() != null) {
                specParams = objectMapper.writeValueAsString(currentSpec.getConnection().getParameters());
            }
        } catch (Exception e) {}

        return """
                You are an expert SAP ADK and Java compiler engineer.
                A Maven build failed for adapter project '%s'.
                
                --- MAVEN BUILD COMPILATION ERROR LOG ---
                %s
                --- END ERROR LOG ---

                --- TEMPLATE-OWNED / READ-ONLY SOURCE CONTEXT ---
                %s
                --- END READ-ONLY SOURCE CONTEXT ---

                --- ADAPTER SPECIFICATION PARAMETERS ---
                %s
                --- END SPECIFICATION PARAMETERS ---

                --- AI-EDITABLE TARGET IMPLEMENTATION ---
                Producer Imports: %s
                Consumer Imports: %s
                Custom Dependencies: %s
                Producer Implementation: %s
                Consumer Implementation: %s
                --- END AI-EDITABLE TARGET IMPLEMENTATION ---

                Analyze the exact compilation/build error within the context of the provided full source file.
                The full source file is READ-ONLY context. You cannot change it directly.
                You can ONLY output changes to the AI-EDITABLE TARGET IMPLEMENTATION snippets (Imports, Dependencies, Implementation).
                
                CRITICAL RULES:
                1. If a dependency failed to download ("Could not find artifact ... in central"), verify the exact groupId/artifactId or replace it with standard Apache HttpClient ('org.apache.httpcomponents:httpclient:4.5.14'). For Salesforce REST, use 'com.frejo:force-rest-api:0.0.45' or standard Apache HttpClient. Never use 'com.force.api:force-rest-api'. For Google Drive API, use known version 'v3-rev20240809-2.0.0'. Never hallucinate unreleased dates like 'v3-rev20240815-2.0.0'.
                2. Do not include jackson-databind or slf4j in customDependencies (they are already provided by the base POM).
                3. Ensure all Java classes used in the implementations (e.g. IOException) are explicitly imported.
                4. Variables declared in the TEMPLATE-OWNED section (like parameter strings declared in paramReadLines) ALREADY exist and are immutable. Do NOT redefine them in your AI-EDITABLE snippet (e.g., if `String maxMessages` is already declared in the template, do not redeclare it, and do not assign an `int` to it). If you need an integer, declare a NEW distinct variable name (e.g. `int maxMessagesLimit = Integer.parseInt(...)`).
                
                Return ONLY valid JSON matching this structure (no markdown, no preamble):
                {
                  "technology": "%s",
                  "category": "custom",
                  "customDependencies": "<corrected pom.xml dependencies>",
                  "excludedImports": "<corrected OSGi exclusions>",
                  "producerImports": "<corrected Java imports for producer>",
                  "producerImplementation": "<corrected Java producer code>",
                  "consumerImports": "<corrected Java imports for consumer>",
                  "consumerImplementation": "<corrected Java consumer code>"
                }
                """.formatted(
                        currentSpec != null && currentSpec.getAdapter() != null ? currentSpec.getAdapter().getName() : "CustomAdapter",
                        errorLog != null && errorLog.length() > 3000 ? errorLog.substring(errorLog.length() - 3000) : errorLog,
                        fileContext != null && !fileContext.isBlank() ? fileContext : "(No file context available)",
                        specParams,
                        currentImports,
                        currentConsumerImports,
                        currentDeps,
                        currentImpl,
                        currentConsumerImpl,
                        tech
                );
    }

    private AdapterSpecification callClaudeAi(String userPrompt, String token, String technologyId) throws Exception {
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

        String systemPrompt = buildSystemPrompt(userPrompt, technologyId);

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

    private String buildSystemPrompt(String userPrompt, String technologyId) {
        StringBuilder pluginsJson = new StringBuilder();
        try {
            if (technologyId != null && !technologyId.isBlank() && !technologyId.equals("null")) {
                pluginsJson.append(objectMapper.writeValueAsString(List.of(registry.getById(technologyId).orElse(null))));
            } else {
                pluginsJson.append(objectMapper.writeValueAsString(registry.getAllTechnologies()));
            }
        } catch (Exception e) {
            pluginsJson.append("[]");
        }

        return """
                You are an expert SAP Cloud Integration ADK architect and Apache Camel developer.
                Analyze the user's natural language requirement for building a custom SAP Cloud Integration adapter.

                We have the following REGISTERED PLUGINS (Technologies) available:
                """ + pluginsJson.toString() + """

                If the user's request matches one of the registered plugins, YOU MUST return the exact connection parameters defined in that plugin's TechnologyDefinition! Set `target.technology` to the plugin's `id`.
                Do NOT hallucinate implementation code if a plugin is matched, because the plugin will provide its own templates. Just fill in the `connection.parameters` with the EXACT values requested by the user, matching the plugin's defined parameter keys.

                If the user's request DOES NOT match any registered plugin, generate a completely dynamic AdapterSpecification JSON (Fallback Mode).
                In Fallback Mode, infer the exact target technology, its required connection parameters, its Maven SDK dependencies, its OSGi import exclusions, and its Java Producer/Consumer processing logic.

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
                    "producerImplementation": "        LOG.info(\\"Executing target processing logic for payload\\");",
                    "consumerImports": "import com.google.cloud.pubsub.v1.Subscriber;",
                    "consumerImplementation": "        LOG.info(\\"Executing polling/listening logic\\");"
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
                        "type": "<string, integer, boolean, secure-alias, dropdown>",
                        "options": ["<choice 1>", "<choice 2>"],
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
                   Prefer the smallest stable client/library that satisfies the target technology requirements. Avoid unnecessarily monolithic SDK bundles when a smaller supported client provides equivalent functionality.
                   Consider transitive dependency size, Java compatibility, Camel compatibility, and SAP ADK/OSGi packaging constraints when selecting dependencies. Do not assume a particular library unless required by the target technology.
                   NEVER include any org.apache.camel:camel-* dependencies. The Camel core is already provided by the SAP ADK parent pom.
                   Use STABLE, verified release versions that actually exist on Maven Central. Do NOT hallucinate version numbers.
                2. excludedImports: List all root package prefixes of the embedded SDK prefixed with ! and suffixed with .* (e.g. !com.example.sdk.*).
                3. producerImports / consumerImports: Include all necessary import statements for SDK classes used in the implementation. NEVER import org.apache.camel.* — they are already available.
                4. IMPLEMENTATION (SENDER OR RECEIVER):
                   - IF DIRECTION IS RECEIVER (Sending to target):
                     Populate `producerImplementation`. The ONLY job is to PUBLISH / SEND / WRITE the incoming Camel payload to the target system. NEVER write subscription or listener logic.
                   - IF DIRECTION IS SENDER (Polling/Listening from target):
                     Populate `consumerImplementation`. The ONLY job is to POLL or LISTEN for incoming data and push it into the Camel route by calling `processMessage(payloadString)`.
                     CRITICAL SENDER RULES:
                     * You MUST initialize `ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();` and use it to convert Maps/Objects to proper JSON strings before calling `processMessage`. NEVER use `.toString()` to serialize payloads!
                     * You MUST enforce limits! If there is a parameter like `maxDocuments` or `maxResults`, you MUST parse it (e.g. `int limit = Integer.parseInt(endpoint.getMaxDocuments());`) and apply it to the query.
                     * You MUST increment `messagesProcessed++` inside the loop for every document processed, otherwise the poll method will return 0 and break.
                     Example: `for (Object obj : results) { String payload = objectMapper.writeValueAsString(obj); processMessage(payload); messagesProcessed++; }`
                     Do NOT populate `producerImplementation` if it is a Sender.
                   - URI PROTOCOL: Connection URLs (e.g. `brokerUrl`, `serverUrl`) must be checked for protocol schemes. If `!brokerUrl.contains("://")`, prepend `"tcp://"` (e.g. `String serverURI = brokerUrl.contains("://") ? brokerUrl : "tcp://" + brokerUrl;`).
                   - METHOD VARIABLES: The following variables ALREADY exist in method scope and must NOT be re-declared:
                     * `endpoint` (Adapter Endpoint), `LOG` (SLF4J Logger), `messagesProcessed` (Integer — ONLY available in consumerImplementation)
                     * Connection parameters as String variables (e.g. `brokerUrl`, `topic`, `qos`)
                   - CREDENTIALS (ZERO RAW CREDENTIALS POLICY): If the target system requires authentication, DO NOT declare raw keys as parameters! Instead, declare a SINGLE connection parameter named `credentialAlias` of type `secure-alias`. Then, retrieve the credentials dynamically using the SAP SecureStore API:
                     `com.sap.it.api.securestore.SecureStoreService secureStoreService = com.sap.it.api.ITApiFactory.getService(com.sap.it.api.securestore.SecureStoreService.class, null);`
                     For Username/Password:
                     `com.sap.it.api.securestore.UserCredential cred = secureStoreService.getUserCredential(endpoint.getCredentialAlias());`
                     `String user = cred.getUsername(); String pass = new String(cred.getPassword());`
                     For API Key/JSON Secret:
                     `com.sap.it.api.securestore.UserCredential cred = secureStoreService.getUserCredential(endpoint.getCredentialAlias());`
                     `String secret = new String(cred.getPassword());`
                     DO NOT USE `new String(password, StandardCharsets.UTF_8)` when passing a `char[]`. Just use `new String(password)`.
                   - CLEANUP: Always disconnect and close client instances before returning.
                5. Connection parameters: Generate 3-6 parameters specific to the target technology. For any boolean flag parameters, set `"type": "boolean"` and `"defaultValue": "true"` or `"false"` so SAP Integration Suite UI renders a native Checkbox control. If max limit is needed, generate `maxDocuments` parameter.
                6. ZERO-RAW-CREDENTIALS POLICY (MANDATORY): In SAP Cloud Integration (CPI), NEVER ask for raw passwords, raw private keys, or raw JSON files. All authentication MUST use a Credential Alias parameter (`"type": "secure-alias"`, `"name": "credentialAlias"`, `"label": "Credential Alias"`, `"defaultValue": "SAP_SECURE_ALIAS"`, `"description": "Deployed Security Material alias in SAP Cloud Integration").
                7. DROPDOWNS (MANDATORY): If a connection parameter has a fixed set of allowed values (e.g. HTTP Method, QoS Level, Environment), set `"type": "dropdown"` and provide those exact choices in an `"options": ["Choice A", "Choice B"]` array.
                8. PAYLOAD ACCESS: The generated implementation MUST NOT assume that variables such as body, payload, message, data, or requestBody already exist in scope. If the implementation needs the Camel message payload, it MUST explicitly obtain it from the provided Exchange object. Examples: `String body = exchange.getIn().getBody(String.class);` or `byte[] body = exchange.getIn().getBody(byte[].class);` or `Object body = exchange.getIn().getBody();` Choose the representation appropriate for the target technology. The generic Producer/Consumer templates must remain payload-agnostic. Payload extraction and conversion belong inside the AI-generated technology-specific implementation.
                9. SLF4J / SAP ADK COMPATIBILITY: Generated code must remain compatible with the SLF4J version available in SAP Integration Suite / SAP ADK (1.6.1). Do NOT assume modern SLF4J varargs APIs. Avoid unsupported logging signatures. For multiple dynamic values, prefer string concatenation when necessary: `LOG.info("Request URL: " + url + ", Method: " + method);` Do not generate logging APIs that require a newer SLF4J version.
                10. JAVA/API CORRECTNESS: Generated code must use real Java 8 APIs and real methods from the declared dependencies. Never invent: methods, classes, constructors, variables, APIs. Before producing the implementation, ensure that every referenced: variable is declared, method actually exists, class actually exists, required import exists, required dependency is available, method signature matches the actual API. Do NOT redeclare a local variable in the same scope. For example, never generate: `String username = ...; ... String username = ...;` Instead reuse the existing variable or use a different name.
                11. COMPILATION CONTRACT: The generated implementation is inserted directly into a Java method: `process(Exchange exchange)`. Therefore the generated implementation MUST be valid Java code for that exact scope. It MUST NOT assume hidden variables, hidden helper methods, hidden imports, or code outside the supplied template. The implementation must compile with Java 8 and the dependencies declared for the generated adapter.
                12. JAVA STRING AND REGEX ESCAPING: All generated Java string literals MUST use valid Java escaping rules. When using regular expressions inside Java string literals, remember that backslashes must be escaped for Java before being interpreted by the regex engine. For example, WRONG: "^\\\\/" CORRECT: "^\\\\\\\\/". However, prefer avoiding unnecessary regex when a simpler Java API can perform the operation safely. For example, instead of `resourcePath.replaceAll("^\\\\\\\\/", "")`, prefer a simpler implementation such as `resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath` when the requirement is simply to remove one leading slash. The generated implementation MUST compile as valid Java 8 source code.
                13. IMPORT CONSISTENCY: Every non-Java-standard class referenced by producerImplementation or consumerImplementation MUST be either: (1) explicitly imported in producerImports/consumerImports, OR (2) referenced using its fully qualified class name. Examples: If implementation uses `ITApiFactory`, `SecureStoreService`, `UserCredential`, then the generated imports MUST contain the corresponding imports (e.g., `import com.sap.it.api.ITApiFactory; import com.sap.it.api.securestore.SecureStoreService; import com.sap.it.api.securestore.UserCredential;`). If implementation uses `HttpClients`, `CloseableHttpClient`, `HttpPost`, then the corresponding Apache imports MUST be present. Do not assume imports are automatically added by the template. Do not generate implementation code that references undeclared classes. The implementation and import lists are a single compilation contract.

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
        LOG.info("Delegating to LLMIntegrationService for prompt: {}", userPrompt);
        AdapterSpecification spec = llmService.generateDynamicSpecification(userPrompt);

        Map<String, Object> result = new HashMap<>();
        result.put("specification", spec);
        result.put("technology", buildTechSummary(spec));
        result.put("missingFields", List.of());
        result.put("parsedPromptSummary", "Dynamically created specification for: " + userPrompt);
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
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }
}
