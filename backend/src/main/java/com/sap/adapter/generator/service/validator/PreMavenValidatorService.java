package com.sap.adapter.generator.service.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class PreMavenValidatorService {

    private static final Logger LOG = LoggerFactory.getLogger(PreMavenValidatorService.class);

    // Derived whitelist from pom.xml.template
    private static final String[] MAVEN_PLACEHOLDER_PREFIXES = {
            "project.",
            "camel.version",
            "adk.version",
            "generic.api.version",
            "env.",
            "settings.",
            "maven-resources"
    };

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public List<String> validateWorkspace(Path workspaceDir) {
        List<String> errors = new ArrayList<>();
        
        if (!Files.exists(workspaceDir)) {
            errors.add("Workspace directory does not exist: " + workspaceDir);
            return errors;
        }

        // 1. Universal Files Check
        checkRequiredFile(workspaceDir, "pom.xml", errors);
        checkRequiredFile(workspaceDir, "metadata/metadata.xml", errors);
        checkRequiredFile(workspaceDir, "OSGI-INF/SUBSYSTEM.MF", errors);

        // 2. Malformed XML Check
        checkXmlFormat(workspaceDir.resolve("pom.xml"), errors);
        checkXmlFormat(workspaceDir.resolve("metadata/metadata.xml"), errors);

        // 3. Placeholder and Security Checks (scan all relevant text files)
        try (Stream<Path> stream = Files.walk(workspaceDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> isScannableFile(p))
                  .forEach(p -> scanFileContent(p, errors));
        } catch (Exception e) {
            LOG.error("Failed to walk workspace directory: {}", e.getMessage());
            errors.add("Failed to scan workspace files: " + e.getMessage());
        }

        return errors;
    }

    private void checkRequiredFile(Path workspaceDir, String relativePath, List<String> errors) {
        if (!Files.exists(workspaceDir.resolve(relativePath))) {
            errors.add("Missing universally required file: " + relativePath);
        }
    }

    private void checkXmlFormat(Path xmlPath, List<String> errors) {
        if (!Files.exists(xmlPath)) {
            return; // Already caught by required files check
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Prevent XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(xmlPath.toFile());
        } catch (Exception e) {
            errors.add("Malformed XML detected in " + xmlPath.getFileName() + ": " + e.getMessage());
        }
    }

    private boolean isScannableFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".mf") || name.endsWith(".adk");
    }

    private void scanFileContent(Path file, List<String> errors) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            
            // 3.1 Security check (only for Java source files to prevent false positives in binary or generated XML if applicable)
            if (file.toString().endsWith(".java") && SecurityScanner.containsHardcodedCredentials(content)) {
                errors.add("Security violation: Hardcoded credentials detected in " + file.getFileName());
            }

            // 3.2 Placeholder check
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
            while (matcher.find()) {
                String variable = matcher.group(1).trim();
                if (!isLegitimateMavenExpression(variable)) {
                    errors.add("Unresolved NexusForge template placeholder detected: ${" + variable + "} in " + file.getFileName());
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read file {} for scanning: {}", file, e.getMessage());
        }
    }

    private boolean isLegitimateMavenExpression(String variable) {
        for (String prefix : MAVEN_PLACEHOLDER_PREFIXES) {
            if (variable.equals(prefix) || variable.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
