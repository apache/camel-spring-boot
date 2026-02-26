/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.springboot.common;

import ch.qos.logback.classic.LoggerContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSpringBootBaseTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSpringBootBaseTestSupport.class);
    private static final String CLASSPATH_FILE = "target/classpath.txt";

    @RegisterExtension
    ArchetypeGenerationExtension archetype = new ArchetypeGenerationExtension(getArchetypeConfig());

    private List<String> classpathEntries;

    protected ArchetypeConfig.Builder baseArchetypeConfig() {
        String name = inferModuleName(getClass());
        return ArchetypeConfig.builder()
                .artifactId(name)
                .dependency(inferStarterDependency(getClass()))
                .property("spring.main.banner-mode=off")
                .property("camel.main.name=" + name)
                .property("spring.groovy.template.check-template-location=false");
    }

    protected ArchetypeConfig getArchetypeConfig() {
        return baseArchetypeConfig().build();
    }

    private ConfigurableApplicationContext applicationContext;
    private URLClassLoader appClassLoader;
    private ClassLoader originalClassLoader;

    @BeforeAll
    void compileAndStartApp() throws Exception {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).setName(inferModuleName(getClass()));

        Path projectDir = archetype.getGeneratedProject().getProjectDir();

        List<String> command = new ArrayList<>(List.of(
                System.getProperty("mvn-command"), "compile", "dependency:build-classpath",
                "-DincludeScope=runtime",
                "-Dmdep.outputFile=" + CLASSPATH_FILE,
                "-B"));
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo != null) {
            command.add("-Dmaven.repo.local=" + localRepo);
        }

        LOG.debug("running: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(String.join(" ", command) + " failed (exit code "
                    + exitCode + ") for " + projectDir + ":\n" + output);
        }

        Path classpathFile = projectDir.resolve(CLASSPATH_FILE);
        String cpContent = Files.readString(classpathFile).trim();
        classpathEntries = cpContent.isEmpty()
                ? List.of()
                : List.of(cpContent.split(File.pathSeparator));

        // classloader: target/classes + all dependency JARs
        Path classesDir = projectDir.resolve("target/classes");
        List<URL> urls = new ArrayList<>();
        urls.add(classesDir.toUri().toURL());
        for (String entry : classpathEntries) {
            urls.add(new File(entry).toURI().toURL());
        }

        appClassLoader = new URLClassLoader(
                urls.toArray(URL[]::new),
                Thread.currentThread().getContextClassLoader());

        originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(appClassLoader);

        Class<?> springAppClass = appClassLoader.loadClass("org.springframework.boot.SpringApplication");
        Class<?> appClass = appClassLoader.loadClass(getArchetypeConfig().getMainClassFqn());

        Object springApp = springAppClass.getConstructor(Class[].class)
                .newInstance((Object) new Class<?>[]{appClass});

        // to load application.properties from the generated project
        Class<?> resourceLoaderClass = appClassLoader.loadClass("org.springframework.core.io.DefaultResourceLoader");
        Object resourceLoader = resourceLoaderClass.getConstructor(ClassLoader.class)
                .newInstance(appClassLoader);
        Class<?> resourceLoaderInterface = appClassLoader.loadClass("org.springframework.core.io.ResourceLoader");
        springAppClass.getMethod("setResourceLoader", resourceLoaderInterface)
                .invoke(springApp, resourceLoader);

        applicationContext = (ConfigurableApplicationContext) springAppClass
                .getMethod("run", String[].class)
                .invoke(springApp, (Object) new String[0]);

        // Bridge CamelClusterService beans from Spring to the CamelContext service registry.
        // The child classloader prevents DefaultConfigurationConfigurer.afterConfigure()
        // from discovering these beans via the Camel registry.
        if (getArchetypeConfig().isRegisterClusterServices()) {
            Map<String, CamelClusterService> clusterServices
                    = applicationContext.getBeansOfType(CamelClusterService.class);
            for (CamelClusterService css : clusterServices.values()) {
                getCamelContext().addService(css);
            }
        }
    }

    @Test
    void camelContextIsRunning() {
        Assertions.assertNotNull(getCamelContext());
        Assertions.assertTrue(getCamelContext().isStarted());
    }

    @Test
    void dependencyVersionMismatchTest() {
        assertNoVersionMismatch();
    }

    protected void assertComponent(String name) {
        Component component = getCamelContext().getComponent(name, true, false);
        Assertions.assertNotNull(component, "Component not found: " + name);
    }

    @AfterAll
    void stopApp() throws Exception {
        try {
            if (applicationContext != null) {
                applicationContext.close();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            if (appClassLoader != null) {
                appClassLoader.close();
            }
        }
    }

    protected ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected CamelContext getCamelContext() {
        return applicationContext.getBean(CamelContext.class);
    }

    protected void assertDataFormat(String name) {
        DataFormat df = getCamelContext().resolveDataFormat(name);
        Assertions.assertNotNull(df, "DataFormat not found: " + name);
    }

    protected void assertLanguage(String name) {
        Language lang = getCamelContext().resolveLanguage(name);
        Assertions.assertNotNull(lang, "Language not found: " + name);
    }

    /**
     * Infers the Camel Spring Boot starter dependency GAV from the test class name.
     * E.g. {@code CamelActivemq6IT} -> {@code org.apache.camel.springboot:camel-activemq6-starter}
     */
    protected static String inferStarterDependency(Class<?> testClass) {
        return "org.apache.camel.springboot:" + inferModuleName(testClass) + "-starter";
    }

    /**
     * Infers the Camel component name from the test class name.
     * E.g. {@code CamelActivemq6IT} -> {@code activemq6}
     */
    protected static String inferComponentName(Class<?> testClass) {
        String moduleName = inferModuleName(testClass);
        return moduleName.startsWith("camel-") ? moduleName.substring("camel-".length()) : moduleName;
    }

    protected static String inferModuleName(Class<?> testClass) {
        String name = testClass.getSimpleName();
        int end = name.length();
        if (name.endsWith("IT")) {
            end = name.length() - 2;
        } else if (name.endsWith("Test")) {
            end = name.length() - 4;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !sb.isEmpty()) {
                sb.append("-");
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * Parses the runtime classpath entries (Maven local repository JAR paths) and detects
     * version mismatches among artifacts sharing the same groupId and artifact prefix.
     * <p>
     * Maven repository paths follow the layout:
     * {@code <repo>/<groupId-dirs>/<artifactId>/<version>/<artifactId>-<version>.jar}
     */
    protected void assertNoVersionMismatch() {
        Assertions.assertNotNull(classpathEntries, "Classpath not resolved yet");

        record Artifact(String groupId, String artifactId, String version) {}

        List<Artifact> artifacts = new ArrayList<>();
        for (String entry : classpathEntries) {
            Path path = Path.of(entry);
            String fileName = path.getFileName().toString();
            if (!fileName.endsWith(".jar")) {
                continue;
            }

            Path versionDir = path.getParent();
            if (versionDir == null) continue;
            Path artifactDir = versionDir.getParent();
            if (artifactDir == null) continue;

            String version = versionDir.getFileName().toString();
            String artifactId = artifactDir.getFileName().toString();

            String fullPath = artifactDir.getParent() == null ? "" : artifactDir.getParent().toString();
            int repoIdx = fullPath.indexOf("repository" + File.separator);
            if (repoIdx < 0) continue;
            String groupPath = fullPath.substring(repoIdx + "repository".length() + 1);
            String groupId = groupPath.replace(File.separatorChar, '.');

            artifacts.add(new Artifact(groupId, artifactId, version));
            LOG.debug("Dependency: {}:{}:{}", groupId, artifactId, version);
        }

        Map<String, Map<String, Set<String>>> status = new TreeMap<>();
        for (Artifact a : artifacts) {
            String artifactPrefix = a.artifactId();
            if (artifactPrefix.contains("-")) {
                artifactPrefix = artifactPrefix.substring(0, artifactPrefix.indexOf("-"));
            }
            String prefixId = a.groupId() + ":" + artifactPrefix;
            String identifier = a.groupId() + ":" + a.artifactId();

            status.computeIfAbsent(prefixId, k -> new TreeMap<>())
                    .computeIfAbsent(identifier, k -> new LinkedHashSet<>())
                    .add(a.version());
        }

        Set<String> mismatches = new TreeSet<>();
        Set<String> potentialMismatches = new TreeSet<>();
        for (Map.Entry<String, Map<String, Set<String>>> group : status.entrySet()) {
            String prefixId = group.getKey();
            Map<String, Set<String>> artifactVersions = group.getValue();

            Set<String> allVersions = new TreeSet<>();
            for (Set<String> versions : artifactVersions.values()) {
                allVersions.addAll(versions);
            }

            if (allVersions.size() <= 1) {
                continue;
            }

            boolean exactMismatch = artifactVersions.values().stream()
                    .anyMatch(v -> v.size() > 1 && !differOnlyInPatch(v));
            if (exactMismatch) {
                mismatches.add(prefixId);
            } else {
                potentialMismatches.add(prefixId);
            }
        }

        StringBuilder message = new StringBuilder();
        for (String mismatch : mismatches) {
            message.append("Version mismatch for ").append(mismatch).append(":\n");
            for (Map.Entry<String, Set<String>> entry : status.get(mismatch).entrySet()) {
                message.append("  - ").append(entry.getKey()).append(" --> ").append(entry.getValue()).append("\n");
            }
        }

        StringBuilder warnings = new StringBuilder();
        for (String mismatch : potentialMismatches) {
            warnings.append("Potential version mismatch for ").append(mismatch).append(":\n");
            for (Map.Entry<String, Set<String>> entry : status.get(mismatch).entrySet()) {
                warnings.append("  - ").append(entry.getKey()).append(" --> ").append(entry.getValue()).append("\n");
            }
        }

        if (!warnings.isEmpty()) {
            String moduleName = inferModuleName(getClass());
            String warningText = "=== Potential version mismatches for " + moduleName + " ===\n" + warnings;
            LOG.warn(warningText);
            try {
                String reportsDir = System.getProperty("project.build.directory", "target") + "/failsafe-reports";
                Path reportFile = Path.of(reportsDir, moduleName + "-version-mismatches.txt");
                Files.createDirectories(reportFile.getParent());
                Files.writeString(reportFile, warningText);
            } catch (IOException e) {
                LOG.warn("Failed to write version mismatch report: " + e.getMessage());
            }
        }

        Assertions.assertTrue(mismatches.isEmpty(),
                "Library version mismatches found in runtime dependencies:\n" + message);
    }

    /**
     * Returns the major.minor prefix of a semantic version string.
     * E.g. {@code "1.2.3"} -> {@code "1.2"}, {@code "4.19.0-SNAPSHOT"} -> {@code "4.19"}.
     * Returns the full version if it has fewer than two dot-separated segments.
     */
    private static String majorMinor(String version) {
        int first = version.indexOf('.');
        if (first < 0) {
            return version;
        }
        int second = version.indexOf('.', first + 1);
        return second < 0 ? version : version.substring(0, second);
    }

    /**
     * Returns {@code true} if all versions in the set share the same major.minor
     * and only differ in patch (or qualifier).
     */
    private static boolean differOnlyInPatch(Set<String> versions) {
        return versions.stream().map(AbstractSpringBootBaseTestSupport::majorMinor).distinct().count() <= 1;
    }
}
