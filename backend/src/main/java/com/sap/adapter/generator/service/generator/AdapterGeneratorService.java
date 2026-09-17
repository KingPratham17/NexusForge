package com.sap.adapter.generator.service.generator;

import com.sap.adapter.generator.model.spec.AdapterSpecification;
import com.sap.adapter.generator.model.spec.ConnectionParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sap.adapter.generator.registry.TechnologyRegistry;
import com.sap.adapter.generator.plugin.TechnologyPlugin;
import com.sap.adapter.generator.plugin.TemplateDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Generates a valid SAP Cloud Integration Custom Adapter Maven project
 * from an AdapterSpecification, conforming 100% to SAP ADK documentation rules.
 *
 * 100% Dynamic — all Maven SDK dependencies, OSGi manifest import rules,
 * target SDK Java imports, and producer execution code are dynamically derived
 * from the AI specification.
 * Includes automatic version & code sanitization to ensure Maven resolution &
 * compilation success.
 */
@Service
public class AdapterGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(AdapterGeneratorService.class);

    private static final String TEMPLATES = "/templates/adapters/generic/";

    @Autowired
    private TechnologyRegistry registry;

    public Map<String, String> generateProjectSources(AdapterSpecification spec, Path workspaceDir) throws Exception {
        LOG.info("Generating project dynamically for adapter '{}' (scheme={}, target={}) in {}",
                spec.getAdapter().getName(), spec.getAdapter().getScheme(),
                spec.getTarget() != null ? spec.getTarget().getTechnology() : "unknown",
                workspaceDir);

        pruneOldWorkspaces(workspaceDir.getParent());

        Map<String, String> generatedFiles = new LinkedHashMap<>();
        Map<String, String> ctx = buildContext(spec);

        TechnologyPlugin plugin = null;
        if (spec.getTarget() != null && spec.getTarget().getTechnology() != null) {
            plugin = registry.getPluginById(spec.getTarget().getTechnology()).orElse(null);
        }

        if (plugin != null) {
            ctx.putAll(plugin.getPluginContext(spec));
        }

        String packageDirStr = ctx.get("packagePath").replace('.', '/');
        Path javaSrcDir = workspaceDir.resolve("src/main/java/" + packageDirStr);
        Path resourcesDir = workspaceDir.resolve("src/main/resources");
        Path resMetadataDir = resourcesDir.resolve("metadata");
        Path resOsgiInfDir = resourcesDir.resolve("OSGI-INF");
        Path serviceDir = resourcesDir.resolve("META-INF/services/org/apache/camel/component");

        Path rootMetadataDir = workspaceDir.resolve("metadata");
        Path rootOsgiInfDir = workspaceDir.resolve("OSGI-INF");
        Path componentFolder = workspaceDir.resolve("component");
        Path libsFolder = workspaceDir.resolve("libs");

        Files.createDirectories(javaSrcDir);
        Files.createDirectories(resourcesDir);
        Files.createDirectories(resMetadataDir);
        Files.createDirectories(resOsgiInfDir);
        Files.createDirectories(serviceDir);
        Files.createDirectories(rootMetadataDir);
        Files.createDirectories(rootOsgiInfDir);
        Files.createDirectories(componentFolder);
        Files.createDirectories(libsFolder);

        List<TemplateDefinition> templates = (plugin != null) ? plugin.getTemplates(spec) : Collections.emptyList();

        if (!templates.isEmpty()) {
            LOG.info("Using plugin-provided templates for {}...", spec.getTarget().getTechnology());
            for (TemplateDefinition td : templates) {
                String rendered = renderTemplateDynamic(td.getSourcePath(), ctx);
                Path destPath = workspaceDir.resolve(td.getDestinationPath());
                Files.createDirectories(destPath.getParent());
                write(generatedFiles, destPath, rendered, td.getDestinationPath());
            }
        } else {
            LOG.info("Using generic fallback templates...");
            String cls = ctx.get("adapterClassName");

            // 1. Component.java
            write(generatedFiles, javaSrcDir.resolve(cls + "Component.java"),
                    render("component.java.template", ctx),
                    cls + "Component.java");

            // 2. Endpoint.java
            write(generatedFiles, javaSrcDir.resolve(cls + "Endpoint.java"),
                    render("endpoint.java.template", ctx),
                    cls + "Endpoint.java");

            // 3. Producer and/or Consumer
            String direction = spec.getAdapter().getDirection();
            boolean isSender = "sender".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction);
            boolean isReceiver = "receiver".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction);

            if (isSender) {
                write(generatedFiles, javaSrcDir.resolve(cls + "Consumer.java"),
                        render("consumer.java.template", ctx),
                        cls + "Consumer.java");
            }
            if (isReceiver) {
                write(generatedFiles, javaSrcDir.resolve(cls + "Producer.java"),
                        render("producer.java.template", ctx),
                        cls + "Producer.java");
            }

            // 4. Camel service descriptor
            String scheme = ctx.get("scheme");
            write(generatedFiles, serviceDir.resolve(scheme),
                    render("service-descriptor.template", ctx),
                    "META-INF/services/org/apache/camel/component/" + scheme);

            // 5. metadata.xml
            String metadataContent = render("metadata.xml.template", ctx);
            write(generatedFiles, rootMetadataDir.resolve("metadata.xml"), metadataContent, "metadata/metadata.xml");
            Files.writeString(resMetadataDir.resolve("metadata.xml"), metadataContent, StandardCharsets.UTF_8);

            // 6. SUBSYSTEM.MF
            String subsystemContent = render("SUBSYSTEM.MF.template", ctx);
            write(generatedFiles, rootOsgiInfDir.resolve("SUBSYSTEM.MF"), subsystemContent, "OSGI-INF/SUBSYSTEM.MF");
            Files.writeString(resOsgiInfDir.resolve("SUBSYSTEM.MF"), subsystemContent, StandardCharsets.UTF_8);

            // 7. config.adk
            write(generatedFiles, workspaceDir.resolve("config.adk"),
                    render("config.adk.template", ctx),
                    "config.adk");

            // 8. pom.xml
            write(generatedFiles, workspaceDir.resolve("pom.xml"),
                    render("pom.xml.template", ctx),
                    "pom.xml");
        }



        LOG.info("Generated {} source files in {}", generatedFiles.size(), workspaceDir);
        return generatedFiles;
    }

    private void pruneOldWorkspaces(Path workspacesRoot) {
        if (workspacesRoot == null || !Files.exists(workspacesRoot))
            return;
        try (Stream<Path> stream = Files.list(workspacesRoot)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("build-"))
                    .forEach(p -> {
                        try {
                            deleteDirectoryRecursively(p);
                        } catch (Exception e) {
                            LOG.warn("Could not delete old build workspace {}: {}", p, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            LOG.warn("Error pruning old workspaces: {}", e.getMessage());
        }
    }

    private void deleteDirectoryRecursively(Path dir) throws Exception {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private Map<String, String> buildContext(AdapterSpecification spec) {
        Map<String, String> ctx = new HashMap<>();

        String name = spec.getAdapter().getName();
        String cls = toClassName(name);
        String scheme = spec.getAdapter().getScheme();
        String pkg = spec.getAdapter().getPackagePath();
        String vendor = spec.getAdapter().getVendor();
        String version = spec.getAdapter().getVersion();

        String artifactId = scheme.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");

        ctx.put("adapterName", name);
        ctx.put("adapterClassName", cls);
        ctx.put("scheme", scheme);
        ctx.put("packagePath", pkg);
        ctx.put("vendor", vendor);
        ctx.put("adapterVersion", version);
        ctx.put("artifactId", artifactId);
        ctx.put("targetTechnology", spec.getTarget() != null ? spec.getTarget().getTechnology() : "Target System");
        ctx.put("camelVersion", spec.getRuntime().getCamelVersion());
        ctx.put("adkVersion", spec.getRuntime().getAdkVersion());

        // 100% Dynamic AI-driven SDK dependencies, OSGi import rules, and Producer code
        String rawDependencies = (spec.getTarget() != null && spec.getTarget().getCustomDependencies() != null
                && !spec.getTarget().getCustomDependencies().isBlank())
                        ? spec.getTarget().getCustomDependencies()
                        : "    <!-- Target SDK dependencies for "
                                + (spec.getTarget() != null ? spec.getTarget().getTechnology() : "Target System")
                                + " -->";

        String customDependencies = sanitizeCustomDependencies(rawDependencies);

        String excludedImports = (spec.getTarget() != null && spec.getTarget().getExcludedImports() != null)
                ? spec.getTarget().getExcludedImports()
                : "";

        String rawProducerImports = (spec.getTarget() != null && spec.getTarget().getProducerImports() != null)
                ? spec.getTarget().getProducerImports()
                : "";

        String producerImports = rawProducerImports + "\nimport java.nio.charset.StandardCharsets;\nimport javax.net.ssl.SSLContext;\nimport javax.net.ssl.SSLSocketFactory;\n";

        List<ConnectionParameter> params = getParams(spec);

        String rawProducerImpl = (spec.getTarget() != null && spec.getTarget().getProducerImplementation() != null
                && !spec.getTarget().getProducerImplementation().isBlank())
                        ? spec.getTarget().getProducerImplementation()
                        : "        LOG.info(\"Processing message exchange payload for "
                                + (spec.getTarget() != null ? spec.getTarget().getTechnology() : "Target System")
                                + "\");";

        String producerImpl = sanitizeProducerImplementation(rawProducerImpl, params);

        String rawConsumerImports = (spec.getTarget() != null && spec.getTarget().getConsumerImports() != null)
                ? spec.getTarget().getConsumerImports()
                : "";
        String consumerImports = rawConsumerImports + "\nimport java.nio.charset.StandardCharsets;\nimport javax.net.ssl.SSLContext;\nimport javax.net.ssl.SSLSocketFactory;\n";

        String rawConsumerImpl = (spec.getTarget() != null && spec.getTarget().getConsumerImplementation() != null
                && !spec.getTarget().getConsumerImplementation().isBlank())
                        ? spec.getTarget().getConsumerImplementation()
                        : "        LOG.info(\"Polling logic for "
                                + (spec.getTarget() != null ? spec.getTarget().getTechnology() : "Target System")
                                + "\");";
        String consumerImpl = sanitizeProducerImplementation(rawConsumerImpl, params);

        String direction = spec.getAdapter().getDirection();
        String endpointProducer = "";
        String endpointConsumer = "";
        
        String senderVariant = "";
        String receiverVariant = "";

        if ("sender".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction)) {
            endpointConsumer = "    @Override\n" +
                    "    public Consumer createConsumer(Processor processor) throws Exception {\n" +
                    "        return new " + cls + "Consumer(this, processor);\n" +
                    "    }";
            if ("sender".equalsIgnoreCase(direction)) {
                endpointProducer = "    @Override\n" +
                        "    public Producer createProducer() throws Exception {\n" +
                        "        throw new UnsupportedOperationException(\"Adapter does not support producing messages\");\n" +
                        "    }";
            }
            
            senderVariant = "    <Variant VariantName=\"${adapterName} Sender\"\n" +
                    "             VariantId=\"ctype::AdapterVariant/cname::${adapterName}/vendor::${vendor}/tp::${scheme}/mp::${scheme}/direction::Sender\"\n" +
                    "             MetadataVersion=\"2.0\"\n" +
                    "             gen:RuntimeComponentBaseUri=\"${scheme}\"\n" +
                    "             AttachmentBehavior=\"Preserve\">\n" +
                    "        <OutputContent Cardinality=\"1\" Scope=\"outsidepool\" MessageCardinality=\"1\" isStreaming=\"false\">\n" +
                    "            <Content>\n" +
                    "                <ContentType>Any</ContentType>\n" +
                    "                <Schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\"/>\n" +
                    "            </Content>\n" +
                    "        </OutputContent>\n" +
                    "        <Tab id=\"connection\">\n" +
                    "            <GuiLabels guid=\"947ffd8b-60ab-4a87-8f97-772697aa14f5\">\n" +
                    "                <Label language=\"EN\">Connection</Label>\n" +
                    "                <Label language=\"DE\">Connection</Label>\n" +
                    "            </GuiLabels>\n" +
                    "            <AttributeGroup id=\"defaultUriParameter\">\n" +
                    "                <Name xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">URI Setting</Name>\n" +
                    "                <GuiLabels guid=\"b2b1d216-254a-474b-bb0e-35d60a9e1fd2\">\n" +
                    "                    <Label language=\"EN\">URI Setting</Label>\n" +
                    "                    <Label language=\"DE\">URI Setting</Label>\n" +
                    "                </GuiLabels>\n" +
                    "            </AttributeGroup>\n" +
                    "            <AttributeGroup id=\"${adapterName}Endpoint\">\n" +
                    "                <Name xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">${adapterName}</Name>\n" +
                    "                <GuiLabels guid=\"9ec2c525-219c-4792-8206-5b67001fbbff\">\n" +
                    "                    <Label language=\"EN\">${adapterName} Endpoint</Label>\n" +
                    "                    <Label language=\"DE\">${adapterName} Endpoint</Label>\n" +
                    "                </GuiLabels>\n" +
                    "${senderAttributeReferences}\n" +
                    "            </AttributeGroup>\n" +
                    "        </Tab>\n" +
                    "        <ReferencedComponents>\n" +
                    "            <ReferencedComponent>\n" +
                    "                <ReferencedComponentId>ctype::ExtensionVariant/cname::sap:Scheduler/version::1.0</ReferencedComponentId>\n" +
                    "                <AttributeMetadataConfiguration>\n" +
                    "                    <Name>scheduleKey</Name>\n" +
                    "                    <AttributeBehavior>Scheduler_ScheduleOnDay,Scheduler_ScheduleToRecur</AttributeBehavior>\n" +
                    "                </AttributeMetadataConfiguration>\n" +
                    "            </ReferencedComponent>\n" +
                    "        </ReferencedComponents>\n" +
                    "    </Variant>";
        }

        if ("receiver".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction)) {
            endpointProducer = "    @Override\n" +
                    "    public Producer createProducer() throws Exception {\n" +
                    "        return new " + cls + "Producer(this);\n" +
                    "    }";
            if ("receiver".equalsIgnoreCase(direction)) {
                endpointConsumer = "    @Override\n" +
                        "    public Consumer createConsumer(Processor processor) throws Exception {\n" +
                        "        throw new UnsupportedOperationException(\"Adapter does not support consuming messages\");\n" +
                        "    }";
            }
            
            receiverVariant = "    <Variant VariantName=\"${adapterName} Receiver\"\n" +
                    "             VariantId=\"ctype::AdapterVariant/cname::${adapterName}/vendor::${vendor}/tp::${scheme}/mp::${scheme}/direction::Receiver\"\n" +
                    "             IsRequestResponse=\"true\"\n" +
                    "             MetadataVersion=\"2.0\"\n" +
                    "             gen:RuntimeComponentBaseUri=\"${scheme}\"\n" +
                    "             AttachmentBehavior=\"Preserve\">\n" +
                    "        <InputContent Cardinality=\"1\" Scope=\"outsidepool\" MessageCardinality=\"1\" isStreaming=\"false\">\n" +
                    "            <Content>\n" +
                    "                <ContentType>Any</ContentType>\n" +
                    "                <Schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\"/>\n" +
                    "            </Content>\n" +
                    "        </InputContent>\n" +
                    "        <OutputContent Cardinality=\"1\" Scope=\"outsidepool\" MessageCardinality=\"1\" isStreaming=\"false\">\n" +
                    "            <Content>\n" +
                    "                <ContentType>Any</ContentType>\n" +
                    "                <Schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\"/>\n" +
                    "            </Content>\n" +
                    "        </OutputContent>\n" +
                    "        <Tab id=\"connection\">\n" +
                    "            <GuiLabels guid=\"ac70982c-5d11-4b97-95c5-2d59cb4f7754\">\n" +
                    "                <Label language=\"EN\">Connection</Label>\n" +
                    "                <Label language=\"DE\">Connection</Label>\n" +
                    "            </GuiLabels>\n" +
                    "            <AttributeGroup id=\"defaultUriParameter\">\n" +
                    "                <Name xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">URI Setting</Name>\n" +
                    "                <GuiLabels guid=\"304decfc-5103-4f4b-a258-8c81f7b8663f\">\n" +
                    "                    <Label language=\"EN\">URI Setting</Label>\n" +
                    "                    <Label language=\"DE\">URI Setting</Label>\n" +
                    "                </GuiLabels>\n" +
                    "            </AttributeGroup>\n" +
                    "            <AttributeGroup id=\"${adapterName}Endpoint\">\n" +
                    "                <Name xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">${adapterName}</Name>\n" +
                    "                <GuiLabels guid=\"820ea6ce-9811-4375-9c16-989c8086af14\">\n" +
                    "                    <Label language=\"EN\">${adapterName} Endpoint</Label>\n" +
                    "                    <Label language=\"DE\">${adapterName} Endpoint</Label>\n" +
                    "                </GuiLabels>\n" +
                    "${receiverAttributeReferences}\n" +
                    "            </AttributeGroup>\n" +
                    "        </Tab>\n" +
                    "    </Variant>";
        }
        
        if (!senderVariant.isEmpty()) {
            senderVariant = senderVariant.replace("${adapterName}", name)
                                         .replace("${vendor}", vendor)
                                         .replace("${scheme}", scheme)
                                         .replace("${senderAttributeReferences}", buildAttributeReferences(params));
        }
        if (!receiverVariant.isEmpty()) {
            receiverVariant = receiverVariant.replace("${adapterName}", name)
                                             .replace("${vendor}", vendor)
                                             .replace("${scheme}", scheme)
                                             .replace("${receiverAttributeReferences}", buildAttributeReferences(params));
        }

        ctx.put("createProducerMethod", endpointProducer);
        ctx.put("createConsumerMethod", endpointConsumer);
        ctx.put("senderVariant", senderVariant);
        ctx.put("receiverVariant", receiverVariant);

        ctx.put("customDependencies", customDependencies);
        ctx.put("excludedImports", excludedImports);
        ctx.put("producerImports", producerImports);
        ctx.put("producerImplementation", producerImpl);
        ctx.put("consumerImports", consumerImports);
        ctx.put("consumerImplementation", consumerImpl);

        ctx.put("uriParamFields", buildUriParamFields(params));
        ctx.put("uriParamGettersSetters", buildGettersSetters(params));
        ctx.put("paramReadLines", buildParamReadLines(params));
        ctx.put("paramValidations", buildParamValidations(params));
        ctx.put("senderAttributeReferences", buildAttributeReferences(params));
        ctx.put("receiverAttributeReferences", buildAttributeReferences(params));
        ctx.put("attributeMetadataEntries", buildAttributeMetadata(params));

        return ctx;
    }

    /**
     * Sanitizes AI-generated dependency XML using GENERIC structural rules only.
     * No hardcoded technology-specific version fixes — works for ANY target system.
     */
    private String sanitizeCustomDependencies(String rawDeps) {
        if (rawDeps == null || rawDeps.isBlank())
            return rawDeps;

        String deps = rawDeps;

        // GENERIC RULE 1: Strip any org.apache.camel:camel-* dependencies.
        // The Camel core is already provided by the SAP ADK parent POM.
        // AI frequently hallucinates camel wrapper modules that were removed in Camel
        // 3.x.
        deps = deps.replaceAll(
                "(?s)<dependency>\\s*<groupId>org\\.apache\\.camel</groupId>\\s*<artifactId>camel-[^<]+</artifactId>.*?</dependency>",
                "");

        // GENERIC RULE 2: Strip any provided-scope or test-scope dependencies.
        // Our bundle needs compile-scope only; provided/test scopes cause OSGi
        // packaging issues.
        deps = deps.replaceAll("(?s)<dependency>.*?<scope>(?:provided|test)</scope>.*?</dependency>", "");

        // GENERIC RULE 3: Clean up leftover blank lines from stripped dependencies
        deps = deps.replaceAll("(?m)^\\s*$\\n", "");

        return deps;
    }

    /**
     * Sanitizes AI-generated producer implementation code using GENERIC structural
     * rules only.
     * No hardcoded technology-specific fixes — works for ANY target system.
     *
     * Rules applied:
     * 1. Strip duplicate variable declarations for connection parameters already in
     * method scope
     * 2. Fix common type-mismatch patterns (Map doesn't have getBytes, String
     * params used as int)
     * 3. Fully-qualify unresolved short class names from known SDK patterns
     */
    private String sanitizeProducerImplementation(String rawImpl, List<ConnectionParameter> params) {
        if (rawImpl == null || rawImpl.isBlank())
            return rawImpl;

        String impl = rawImpl;

        // GENERIC RULE 1: Strip duplicate variable declarations.
        // The template's Section 1 already declares all connection params as "String
        // varName = endpoint.getVarName();"
        // If AI re-declares "String varName = ...", Java throws "variable already
        // defined" errors.
        if (params != null) {
            for (ConnectionParameter p : params) {
                String varName = toCamelCase(p.getName());
                // Strip "String varName =" → "varName =" (re-assignment, not re-declaration)
                impl = impl.replaceAll("(?m)^(\\s*)String\\s+" + varName + "\\s*=", "$1" + varName + " =");
                // Strip "int varName =" → rename to avoid conflict
                impl = impl.replaceAll("(?m)^(\\s*)int\\s+" + varName + "\\s*=", "$1" + varName + " =");
            }
        }

        // Payload parsing is strictly deferred to the AI.
        // We no longer attempt to auto-correct data.getBytes() or body.getXXX()
        // since the generic template no longer enforces Map/String types.

        // GENERIC RULE 4: Auto-wrap String connection params passed to methods
        // expecting int.
        // Pattern: any method argument matching a known String param name in an int
        // context
        // e.g. client.publish(topic, payload, qosLevel, false) →
        // Integer.parseInt(qosLevel)
        // We detect: ", <paramName>," or ", <paramName>)" where paramName is a known
        // String connection param
        if (params != null) {
            for (ConnectionParameter p : params) {
                if ("integer".equalsIgnoreCase(p.getType()) || "int".equalsIgnoreCase(p.getType())) {
                    continue; // already int type, no conversion needed
                }
                String varName = toCamelCase(p.getName());
                // Only wrap params whose names suggest numeric values (port, qos, timeout,
                // retries, etc.)
                if (varName.toLowerCase().matches(
                        ".*(port|qos|timeout|retries|retry|count|size|level|interval|batch|limit|ttl|max|min|num|delay).*")) {
                    // Wrap standalone usage: ", varName," or ", varName)"
                    impl = impl.replaceAll(",\\s*" + varName + "\\s*,", ", Integer.parseInt(" + varName + "),");
                    impl = impl.replaceAll(",\\s*" + varName + "\\s*\\)", ", Integer.parseInt(" + varName + "))");
                }
            }
        }



        return impl;
    }

    private List<ConnectionParameter> getParams(AdapterSpecification spec) {
        if (spec.getConnection() != null
                && spec.getConnection().getParameters() != null
                && !spec.getConnection().getParameters().isEmpty()) {
            return spec.getConnection().getParameters();
        }
        List<ConnectionParameter> defaults = new ArrayList<>();
        defaults.add(new ConnectionParameter(
                "credentialAlias", "Credential Alias", "secure-alias",
                true, "", "SAP Secure Store alias for credentials"));
        defaults.add(new ConnectionParameter(
                "operation", "Operation", "string",
                true, "SEND", "Operation to perform"));
        return defaults;
    }

    private String buildUriParamFields(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (ConnectionParameter p : params) {
            sb.append("    /**\n");
            sb.append("     * ").append(p.getDescription() != null ? p.getDescription() : p.getLabel()).append("\n");
            sb.append("     */\n");
            sb.append("    @UriParam\n");
            sb.append("    private String ").append(toCamelCase(p.getName())).append(";\n\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildGettersSetters(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (ConnectionParameter p : params) {
            String field = toCamelCase(p.getName());
            String capField = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("    public String get").append(capField).append("() {\n");
            sb.append("        return ").append(field).append(";\n");
            sb.append("    }\n\n");
            sb.append("    public void set").append(capField).append("(String ").append(field).append(") {\n");
            sb.append("        this.").append(field).append(" = ").append(field).append(";\n");
            sb.append("    }\n\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildParamReadLines(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (ConnectionParameter p : params) {
            String field = toCamelCase(p.getName());
            String capField = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("        String ").append(field)
                    .append(" = endpoint.get").append(capField).append("();\n");
            sb.append("        LOG.info(\"").append(p.getLabel())
                    .append("             : {}\", ").append(field).append(");\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildParamValidations(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (ConnectionParameter p : params) {
            if (!p.isRequired())
                continue;
            String field = toCamelCase(p.getName());
            sb.append("        if (").append(field).append(" == null || ").append(field)
                    .append(".trim().isEmpty()) {\n");
            sb.append("            throw new IllegalArgumentException(\n");
            sb.append("                    \"").append(p.getLabel()).append(" must be configured\");\n");
            sb.append("        }\n\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildAttributeReferences(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (ConnectionParameter p : params) {
            sb.append("                <AttributeReference>\n");
            sb.append("                    <ReferenceName>").append(p.getName()).append("</ReferenceName>\n");
            sb.append("                    <description>").append(escapeXml(p.getDescription()))
                    .append("</description>\n");
            sb.append("                </AttributeReference>\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildAttributeMetadata(List<ConnectionParameter> params) {
        StringBuilder sb = new StringBuilder();
        UUID uuid = UUID.randomUUID();
        for (ConnectionParameter p : params) {
            uuid = UUID.nameUUIDFromBytes(p.getName().getBytes(StandardCharsets.UTF_8));
            String dataType = mapDataType(p);
            sb.append("    <AttributeMetadata>\n");
            sb.append("        <Name>").append(p.getName()).append("</Name>\n");
            sb.append("        <Usage>false</Usage>\n");
            sb.append("        <DataType>").append(dataType).append("</DataType>\n");
            sb.append("        <Default>").append(p.getDefaultValue() != null ? p.getDefaultValue() : "")
                    .append("</Default>\n");
            sb.append("        <Length/>\n");
            sb.append("        <isparameterized>true</isparameterized>\n");
            sb.append("        <GuiLabels guid=\"").append(uuid).append("\">\n");
            sb.append("            <Label language=\"EN\">").append(escapeXml(p.getLabel())).append("</Label>\n");
            sb.append("            <Label language=\"DE\">").append(escapeXml(p.getLabel())).append("</Label>\n");
            sb.append("        </GuiLabels>\n");
            sb.append("    </AttributeMetadata>\n");
        }
        return sb.toString().stripTrailing();
    }

    private String renderTemplateDynamic(String path, Map<String, String> context) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                LOG.error("Template not found: {}", path);
                return "";
            }
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : context.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                String val = entry.getValue() != null ? entry.getValue() : "";
                template = template.replace(key, val);
            }
            return template;
        }
    }

    private String render(String templateName, Map<String, String> context) throws Exception {
        String path = TEMPLATES + templateName;
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Template not found: " + path);
        }
        String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : context.entrySet()) {
            template = template.replace("${" + e.getKey() + "}", e.getValue());
        }
        return template;
    }

    private String renderCommon(String templateName, Map<String, String> context) throws Exception {
        String path = "/templates/adapters/common/" + templateName;
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Template not found: " + path);
        }
        String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : context.entrySet()) {
            template = template.replace("${" + e.getKey() + "}", e.getValue());
        }
        return template;
    }

    private void write(Map<String, String> files, Path dest, String content, String displayKey) throws Exception {
        Files.writeString(dest, content, StandardCharsets.UTF_8);
        files.put(displayKey, content);
    }

    private String toClassName(String input) {
        if (input == null || input.isBlank())
            return "CustomAdapter";
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            } else {
                nextUpper = true;
            }
        }
        String result = sb.toString();
        if (result.endsWith("Component"))
            result = result.substring(0, result.length() - "Component".length());
        if (result.endsWith("Adapter"))
            result = result.substring(0, result.length() - "Adapter".length());
        return result + "Adapter";
    }

    private String toCamelCase(String name) {
        if (name == null || name.isBlank())
            return "field";
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == '.') {
                nextUpper = true;
            } else {
                sb.append(nextUpper && sb.length() > 0 ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    private String mapDataType(ConnectionParameter p) {
        if (p == null) return "xsd:string";
        String type = p.getType() != null ? p.getType().toLowerCase() : "";
        String name = p.getName() != null ? p.getName().toLowerCase() : "";
        String defVal = p.getDefaultValue() != null ? p.getDefaultValue().toLowerCase().trim() : "";

        // Check if parameter is a boolean (by type, default value "true"/"false", or boolean param name)
        if ("boolean".equalsIgnoreCase(type)
                || "true".equals(defVal)
                || "false".equals(defVal)
                || name.matches(".*(cleansession|ssl|tls|reconnect|enabled|disabled|useauth).*")) {
            return "xsd:boolean";
        }

        if ("integer".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type)) {
            return "xsd:integer";
        }
        if ("long".equalsIgnoreCase(type)) {
            return "xsd:long";
        }

        return "xsd:string";
    }

    private String escapeXml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
