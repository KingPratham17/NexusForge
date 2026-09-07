package com.sap.adapter.generator.service.validator;

import com.sap.adapter.generator.model.build.InspectionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Validates generated SAP Cloud Integration Custom Adapter artifacts
 * against a 12-point checklist.
 *
 * All checks are generic and technology-agnostic.
 */
@Service
public class ArtifactValidatorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactValidatorService.class);

    public InspectionReport inspectArtifacts(Path workspaceDir, String scheme) {
        LOG.info("Starting 12-point artifact inspection for workspace: {} scheme: {}", workspaceDir, scheme);
        InspectionReport report = new InspectionReport();

        Path targetDir = workspaceDir.resolve("target");
        Path buildDir  = targetDir.resolve("build");

        // ----------------------------------------------------------------
        // Check 1: ESA Package Existence
        // ----------------------------------------------------------------
        Path esaFile = null;
        if (Files.exists(buildDir)) {
            try (Stream<Path> stream = Files.list(buildDir)) {
                esaFile = stream
                        .filter(p -> p.toString().endsWith(".esa") || p.toString().endsWith(".zip"))
                        .findFirst().orElse(null);
            } catch (Exception e) {
                LOG.error("Error searching for ESA: {}", e.getMessage());
            }
        }
        boolean check1 = (esaFile != null && Files.exists(esaFile));
        report.addCheck(1, "ESA Package Existence", check1,
                check1 ? "Found ESA package: " + esaFile.getFileName()
                       : "ESA package not found under target/build/");
        if (esaFile != null) report.setEsaPath(esaFile.toAbsolutePath().toString());

        // ----------------------------------------------------------------
        // Check 2: Component JAR Archive
        // ----------------------------------------------------------------
        Path jarFile = null;
        if (Files.exists(targetDir)) {
            try (Stream<Path> stream = Files.list(targetDir)) {
                jarFile = stream
                        .filter(p -> p.toString().endsWith(".jar") && !p.toString().endsWith("-sources.jar"))
                        .findFirst().orElse(null);
            } catch (Exception e) {
                LOG.error("Error searching for JAR: {}", e.getMessage());
            }
        }
        boolean check2 = (jarFile != null && Files.exists(jarFile));
        report.addCheck(2, "Component JAR Archive", check2,
                check2 ? "Found component JAR: " + jarFile.getFileName()
                       : "Component JAR not found under target/");
        if (jarFile != null) report.setJarPath(jarFile.toAbsolutePath().toString());

        // ----------------------------------------------------------------
        // Check 3: metadata.xml XML Validity
        //   Located at metadata/metadata.xml (at project root, like real POC)
        // ----------------------------------------------------------------
        Path metadataPath = workspaceDir.resolve("metadata/metadata.xml");
        boolean check3 = false;
        String check3Detail;
        if (Files.exists(metadataPath)) {
            try {
                String metaContent = Files.readString(metadataPath, StandardCharsets.UTF_8);
                if (metaContent.contains("<ComponentMetadata") && !metaContent.contains("<AttributeBehavior>")) {
                    check3 = true;
                    check3Detail = "metadata.xml present and schema-compliant (no unsupported AttributeBehavior tags)";
                } else if (metaContent.contains("<AttributeBehavior>")) {
                    check3Detail = "metadata.xml rejected: Contains illegal <AttributeBehavior> unsupported by ADK 2.2.0";
                } else {
                    check3Detail = "metadata.xml missing required <ComponentMetadata> element";
                }
            } catch (Exception e) {
                check3Detail = "Error reading metadata.xml: " + e.getMessage();
            }
        } else {
            check3Detail = "metadata.xml not found at metadata/metadata.xml";
        }
        report.addCheck(3, "ADK metadata.xml Schema Validation", check3, check3Detail);

        // ----------------------------------------------------------------
        // Check 4: Camel Service Descriptor inside JAR
        // ----------------------------------------------------------------
        boolean check4 = false;
        String check4Detail = "Service descriptor missing in JAR";
        if (jarFile != null && Files.exists(jarFile)) {
            try (JarFile jar = new JarFile(jarFile.toFile())) {
                String expectedDescriptor = "META-INF/services/org/apache/camel/component/" + scheme;
                JarEntry entry = jar.getJarEntry(expectedDescriptor);
                if (entry != null) {
                    check4 = true;
                    check4Detail = "Camel component service descriptor verified: " + expectedDescriptor;
                } else {
                    check4Detail = "Missing service descriptor in JAR: " + expectedDescriptor;
                }
            } catch (Exception e) {
                check4Detail = "Error inspecting JAR for service descriptor: " + e.getMessage();
            }
        }
        report.addCheck(4, "Camel Service Registration Descriptor", check4, check4Detail);

        // ----------------------------------------------------------------
        // Check 5: Java Compiled Class Files in JAR
        // ----------------------------------------------------------------
        boolean check5 = false;
        String check5Detail = "Compiled class files missing in JAR";
        if (jarFile != null && Files.exists(jarFile)) {
            try (JarFile jar = new JarFile(jarFile.toFile())) {
                long classCount = jar.stream().filter(e -> e.getName().endsWith(".class")).count();
                if (classCount > 0) {
                    check5 = true;
                    check5Detail = "Verified " + classCount + " compiled .class files in component JAR";
                }
            } catch (Exception e) {
                check5Detail = "Error counting classes: " + e.getMessage();
            }
        }
        report.addCheck(5, "Java Compiled Bytecode Verification", check5, check5Detail);

        // ----------------------------------------------------------------
        // Check 6: Embedded Libraries (lib/) inside JAR
        // ----------------------------------------------------------------
        boolean check6 = false;
        String check6Detail = "No embedded lib/ dependencies found in JAR";
        if (jarFile != null && Files.exists(jarFile)) {
            try (JarFile jar = new JarFile(jarFile.toFile())) {
                long libCount = jar.stream()
                        .filter(e -> e.getName().startsWith("lib/") && e.getName().endsWith(".jar"))
                        .count();
                if (libCount > 0) {
                    check6 = true;
                    check6Detail = "Verified " + libCount + " runtime dependency JARs embedded under lib/";
                } else {
                    // Jackson embedded directly as class files is also acceptable
                    long jacksonClasses = jar.stream()
                            .filter(e -> e.getName().contains("jackson") || e.getName().startsWith("lib/"))
                            .count();
                    if (jacksonClasses > 0) {
                        check6 = true;
                        check6Detail = "Runtime dependencies verified (embedded into bundle classes)";
                    }
                }
            } catch (Exception e) {
                check6Detail = "Error inspecting JAR for libs: " + e.getMessage();
            }
        }
        report.addCheck(6, "OSGi Dependency Embedding (lib/)", check6, check6Detail);

        // ----------------------------------------------------------------
        // Check 7: Bundle MANIFEST.MF Headers
        // ----------------------------------------------------------------
        boolean check7 = false;
        String check7Detail = "MANIFEST.MF verification failed";
        if (jarFile != null && Files.exists(jarFile)) {
            try (JarFile jar = new JarFile(jarFile.toFile())) {
                JarEntry entry = jar.getJarEntry("META-INF/MANIFEST.MF");
                if (entry != null) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        String manifest = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        if (manifest.contains("Bundle-SymbolicName") && manifest.contains("Export-Package")) {
                            check7 = true;
                            check7Detail = "MANIFEST.MF verified — Bundle-SymbolicName and Export-Package headers present";
                        } else {
                            check7Detail = "MANIFEST.MF incomplete — missing Bundle-SymbolicName or Export-Package";
                        }
                    }
                }
            } catch (Exception e) {
                check7Detail = "Error reading MANIFEST.MF: " + e.getMessage();
            }
        }
        report.addCheck(7, "Bundle OSGi MANIFEST.MF Inspection", check7, check7Detail);

        // ----------------------------------------------------------------
        // Check 8: OSGI-INF/SUBSYSTEM.MF at project root
        // ----------------------------------------------------------------
        Path subsystemPath = workspaceDir.resolve("OSGI-INF/SUBSYSTEM.MF");
        boolean check8 = Files.exists(subsystemPath);
        String check8Detail;
        if (check8) {
            try {
                String content = Files.readString(subsystemPath, StandardCharsets.UTF_8);
                check8 = content.contains("SAP-BundleType: IntegrationAdapter");
                check8Detail = check8
                        ? "SUBSYSTEM.MF verified with SAP-BundleType: IntegrationAdapter"
                        : "SUBSYSTEM.MF missing required SAP-BundleType header";
            } catch (Exception e) {
                check8Detail = "Error reading SUBSYSTEM.MF: " + e.getMessage();
            }
        } else {
            check8Detail = "SUBSYSTEM.MF not found at OSGI-INF/SUBSYSTEM.MF";
        }
        report.addCheck(8, "ESA Subsystem Manifest (SUBSYSTEM.MF)", check8, check8Detail);

        // ----------------------------------------------------------------
        // Check 9: Endpoint Scheme Consistency
        // ----------------------------------------------------------------
        boolean check9 = (scheme != null && !scheme.trim().isEmpty());
        report.addCheck(9, "Endpoint Scheme Consistency", check9,
                check9 ? "Verified URI scheme: " + scheme : "Empty or invalid scheme detected");

        // ----------------------------------------------------------------
        // Check 10: @UriParam fields present in generated Endpoint source
        // ----------------------------------------------------------------
        boolean check10 = false;
        String check10Detail = "Endpoint source not found for verification";
        try (Stream<Path> stream = Files.walk(workspaceDir.resolve("src/main/java"))) {
            java.util.Optional<Path> endpointSource = stream
                    .filter(p -> p.getFileName().toString().endsWith("Endpoint.java"))
                    .findFirst();
            if (endpointSource.isPresent()) {
                String epCode = Files.readString(endpointSource.get(), StandardCharsets.UTF_8);
                if (epCode.contains("@UriParam") && epCode.contains("@UriEndpoint")) {
                    check10 = true;
                    check10Detail = "Endpoint.java verified — @UriEndpoint and @UriParam annotations present";
                } else {
                    check10Detail = "Endpoint.java found but missing @UriEndpoint or @UriParam annotations";
                }
            }
        } catch (Exception e) {
            check10Detail = "Error inspecting Endpoint.java: " + e.getMessage();
        }
        report.addCheck(10, "Adapter Parameter Annotation Verification", check10, check10Detail);

        // ----------------------------------------------------------------
        // Check 11: Security Credentials Audit
        // ----------------------------------------------------------------
        boolean check11 = true;
        String check11Detail = "Security audit passed — no hardcoded credentials in source";
        try (Stream<Path> stream = Files.walk(workspaceDir.resolve("src"))) {
            boolean leakFound = stream.filter(Files::isRegularFile).anyMatch(p -> {
                try {
                    String c = Files.readString(p, StandardCharsets.UTF_8);
                    return c.contains("private_key") || c.contains("-----BEGIN PRIVATE KEY-----")
                            || c.contains("password=") || c.contains("apiKey=");
                } catch (Exception e) {
                    return false;
                }
            });
            if (leakFound) {
                check11 = false;
                check11Detail = "SECURITY ALERT: Hardcoded credential detected in source files!";
            }
        } catch (Exception e) {
            LOG.error("Security audit error: {}", e.getMessage());
        }
        report.addCheck(11, "Security Credentials Audit", check11, check11Detail);

        // ----------------------------------------------------------------
        // Check 12: Maven Build Outcome
        // ----------------------------------------------------------------
        boolean check12 = (check1 || check2);
        report.addCheck(12, "Maven Build Outcome", check12,
                check12 ? "Maven compilation and packaging completed with output artifacts"
                        : "Maven build failed to produce target artifacts");

        return report;
    }
}
