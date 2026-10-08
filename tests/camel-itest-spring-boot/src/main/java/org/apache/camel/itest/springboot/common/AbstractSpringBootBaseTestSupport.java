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

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String STARTER_ARTIFACT_ID = "camel-spring-boot-starter";
    /**
     * Artifacts of these groups all come from a single release, so any version disagreement among them is
     * a real mismatch, not a potential one.
     */
    private static final Set<String> CAMEL_GROUP_IDS = Set.of("org.apache.camel", "org.apache.camel.springboot");
    /** artifacts versioned independently of the others sharing their groupId and prefix, by groupId:artifactId prefix */
    private static final List<String> IGNORED_ARTIFACTS = List.of("io.netty:netty-tcnative");

    // the supplier is resolved lazily by the extension, so a subclass may override getArchetypeConfig()
    // using its own fields even though this field is initialised before them
    @RegisterExtension
    ArchetypeGenerationExtension archetype = new ArchetypeGenerationExtension(this::getArchetypeConfig);

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
        Path projectDir = archetype.getGeneratedProject().getProjectDir();
        ArchetypeConfig config = archetype.getArchetypeConfig();

        ArchetypeGenerationExtension.runMaven(projectDir, "compile", "dependency:build-classpath",
                "-DincludeScope=runtime",
                "-Dmdep.outputFile=" + CLASSPATH_FILE);

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

        originalClassLoader = Thread.currentThread().getContextClassLoader();
        appClassLoader = new URLClassLoader(urls.toArray(URL[]::new), originalClassLoader);
        Thread.currentThread().setContextClassLoader(appClassLoader);

        Class<?> springAppClass = appClassLoader.loadClass("org.springframework.boot.SpringApplication");
        Class<?> appClass = appClassLoader.loadClass(config.getMainClassFqn());

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
        if (config.isRegisterClusterServices()) {
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
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
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
     * Parses the runtime classpath entries (Maven local repository JAR paths) and detects version
     * mismatches among artifacts sharing the same groupId and artifact prefix.
     * <p>
     * Maven repository paths follow the layout:
     * {@code <repo>/<groupId-dirs>/<artifactId>/<version>/<artifactId>-<version>.jar}
     * <p>
     * Artifacts of Camel's own groups ({@link #CAMEL_GROUP_IDS}) must all have the same version, any
     * disagreement fails the test. For other groups, versions differing in major or minor are reported
     * as potential mismatches: logged as a warning and written to
     * {@code target/failsafe-reports/<module>-version-mismatches.txt}.
     */
    protected void assertNoVersionMismatch() {
        Assertions.assertNotNull(classpathEntries, "Classpath not resolved yet");

        record Artifact(String groupId, String artifactId, String version) {}

        Path localRepository = localRepository();
        List<Artifact> artifacts = new ArrayList<>();
        for (String entry : classpathEntries) {
            Path path = Path.of(entry).toAbsolutePath().normalize();
            if (!path.getFileName().toString().endsWith(".jar") || !path.startsWith(localRepository)) {
                continue;
            }
            // <groupId-dirs>/<artifactId>/<version>/<jar>
            Path relative = localRepository.relativize(path);
            int count = relative.getNameCount();
            if (count < 4) {
                continue;
            }
            String version = relative.getName(count - 2).toString();
            String artifactId = relative.getName(count - 3).toString();
            StringBuilder groupId = new StringBuilder();
            for (int i = 0; i < count - 3; i++) {
                if (i > 0) {
                    groupId.append('.');
                }
                groupId.append(relative.getName(i));
            }

            artifacts.add(new Artifact(groupId.toString(), artifactId, version));
            LOG.debug("Dependency: {}:{}:{}", groupId, artifactId, version);
        }
        Assertions.assertFalse(artifacts.isEmpty(), "No artifacts from " + localRepository + " on the classpath");

        Map<String, Map<String, Set<String>>> status = new TreeMap<>();
        for (Artifact a : artifacts) {
            String artifactPrefix = a.artifactId();
            if (artifactPrefix.contains("-")) {
                artifactPrefix = artifactPrefix.substring(0, artifactPrefix.indexOf("-"));
            }
            String prefixId = a.groupId() + ":" + artifactPrefix;
            String identifier = a.groupId() + ":" + a.artifactId();
            if (IGNORED_ARTIFACTS.stream().anyMatch(identifier::startsWith)) {
                continue;
            }

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

            String groupId = prefixId.substring(0, prefixId.indexOf(':'));
            if (CAMEL_GROUP_IDS.contains(groupId)) {
                mismatches.add(prefixId);
            } else if (!differOnlyInPatch(allVersions)) {
                potentialMismatches.add(prefixId);
            }
        }

        StringBuilder message = new StringBuilder();
        for (String mismatch : mismatches) {
            message.append("Version mismatch for ").append(mismatch).append(":").append(System.lineSeparator());
            for (Map.Entry<String, Set<String>> entry : status.get(mismatch).entrySet()) {
                message.append("  - ").append(entry.getKey()).append(" --> ").append(entry.getValue()).append(System.lineSeparator());
            }
        }

        StringBuilder warnings = new StringBuilder();
        for (String mismatch : potentialMismatches) {
            warnings.append("Potential version mismatch for ").append(mismatch).append(":").append(System.lineSeparator());
            for (Map.Entry<String, Set<String>> entry : status.get(mismatch).entrySet()) {
                warnings.append("  - ").append(entry.getKey()).append(" --> ").append(entry.getValue()).append(System.lineSeparator());
            }
        }

        if (!warnings.isEmpty()) {
            String moduleName = inferModuleName(getClass());
            String warningText = "=== Potential version mismatches for " + moduleName + " ===" + System.lineSeparator() + warnings;
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
                "Library version mismatches found in runtime dependencies:" + System.lineSeparator() + message);
    }

    /**
     * Locates the Maven local repository from the classpath entry of {@code camel-spring-boot-starter},
     * which every generated project depends on. This does not rely on the repository's location or name,
     * which can be configured in settings.xml and is not visible from here.
     */
    private Path localRepository() {
        String starterJar = STARTER_ARTIFACT_ID + "-" + System.getProperty("project-version") + ".jar";
        for (String entry : classpathEntries) {
            Path path = Path.of(entry).toAbsolutePath().normalize();
            if (path.getFileName().toString().equals(starterJar)) {
                // <repo>/org/apache/camel/springboot/camel-spring-boot-starter/<version>/<jar>
                return path.getParent().getParent().getParent()
                        .getParent().getParent().getParent().getParent();
            }
        }
        throw new AssertionError(starterJar + " not found on the runtime classpath: " + classpathEntries);
    }

    /**
     * Returns the major.minor prefix of a semantic version string.
     * E.g. {@code "1.2.3"} -> {@code "1.2"}, {@code "4.23.0-SNAPSHOT"} -> {@code "4.23"}.
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
