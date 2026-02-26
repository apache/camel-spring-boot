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
import org.apache.camel.util.FileUtil;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ArchetypeGenerationExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(ArchetypeGenerationExtension.class);

    private static final String ARCHETYPE_GROUP_ID = "org.apache.camel.archetypes";
    private static final String ARCHETYPE_ARTIFACT_ID = "camel-archetype-spring-boot";

    private final ArchetypeConfig archetypeConfig;
    private GeneratedProject generatedProject;

    public ArchetypeGenerationExtension() {
        this(ArchetypeConfig.builder().build());
    }

    public ArchetypeGenerationExtension(ArchetypeConfig archetypeConfig) {
        this.archetypeConfig = archetypeConfig;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ensureProjectCreated(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ensureProjectCreated(context);
    }

    private void ensureProjectCreated(ExtensionContext context) {
        if (generatedProject == null) {
            generatedProject = createProject();
            // Register in store for cleanup
            context.getStore(NAMESPACE).put(GeneratedProject.class, generatedProject);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == GeneratedProject.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return generatedProject;
    }

    private GeneratedProject createProject() {
        try {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).setName(archetypeConfig.artifactId);
            Path targetDir = Path.of(requireSystemProperty("project.build.directory"));
            Files.createDirectories(targetDir);
            Path outputDir = Files.createTempDirectory(targetDir, "archetype-test-");

            String archetypeVersion = requireSystemProperty("project-version");
            String camelVersion = requireSystemProperty("camel-version");
            String springBootVersion = requireSystemProperty("spring-boot-version");
            String mavenCompilerPluginVersion = requireSystemProperty("maven-compiler-plugin-version");
            String mavenVersion = requireSystemProperty("maven-version");

            File archetypeFile = resolveArchetypeJar(archetypeVersion);
            if (!archetypeFile.exists()) {
                throw new IllegalStateException("Archetype JAR not found at: " + archetypeFile
                        + ". Run 'mvn install -pl archetypes/camel-archetype-spring-boot -am -DskipTests' first.");
            }

            // Resolve absolute path to Maven wrapper
            String mvnwPath = System.getProperty("mvn-command");
            if (mvnwPath.startsWith("./")) {
                // Convert relative path to absolute based on multiModuleProjectDirectory
                String projectRoot = System.getProperty("maven.multiModuleProjectDirectory");
                if (projectRoot != null) {
                    mvnwPath = Path.of(projectRoot, mvnwPath.substring(2)).toString();
                }
            }

            List<String> command = new ArrayList<>(List.of(
                    mvnwPath, "-q", "archetype:generate",
                    "-B",
                    "-DarchetypeGroupId=" + ARCHETYPE_GROUP_ID,
                    "-DarchetypeArtifactId=" + ARCHETYPE_ARTIFACT_ID,
                    "-DarchetypeVersion=" + archetypeVersion,
                    "-DgroupId=" + archetypeConfig.groupId,
                    "-DartifactId=" + archetypeConfig.artifactId,
                    "-Dversion=" + archetypeConfig.version,
                    "-Dpackage=" + archetypeConfig.packageName,
                    "-Dcamel-version=" + camelVersion,
                    "-Dspring-boot-version=" + springBootVersion,
                    "-Dmaven-compiler-plugin-version=" + mavenCompilerPluginVersion,
                    "-Dmaven-version=" + mavenVersion));

            String localRepo = System.getProperty("maven.repo.local");
            if (localRepo != null) {
                command.add("-Dmaven.repo.local=" + localRepo);
            }

            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(outputDir.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Archetype generation failed (exit code "
                        + exitCode + ")");
            }

            Path projectDir = outputDir.resolve(archetypeConfig.artifactId);
            customizeArchetype(projectDir, archetypeConfig);
            return new GeneratedProject(outputDir, projectDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate archetype project", e);
        }
    }

    private static String requireSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Required system property '" + key + "' is not set");
        }
        return value;
    }

    private File resolveArchetypeJar(String version) {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        return Path.of(localRepo, ARCHETYPE_GROUP_ID.replace('.', File.separatorChar)
                , ARCHETYPE_ARTIFACT_ID, version, ARCHETYPE_ARTIFACT_ID + "-" + version + ".jar").toFile();
    }

    public GeneratedProject getGeneratedProject() {
        return generatedProject;
    }

    /**
     * Strips the generated archetype project down to only the {@code @SpringBootApplication} main class.
     * Removes the entire {@code src/test} tree and all Java source files under {@code src/main/java}
     * except the main application class defined in the config. Adds any extra dependencies from the
     * config to the generated pom.xml.
     */
    public static void customizeArchetype(Path projectDir, ArchetypeConfig config) throws Exception {
        String mainClassFile = config.getMainClassName() + ".java";

        FileUtil.removeDir(projectDir.resolve("src/test").toFile());

        Path mainJavaDir = projectDir.resolve("src/main/java");
        if (Files.exists(mainJavaDir)) {
            Files.walk(mainJavaDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals(mainClassFile))
                    .forEach(p -> p.toFile().delete());
        }

        Path pomFile = projectDir.resolve("pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (Reader r = Files.newBufferedReader(pomFile)) {
            model = reader.read(r);
        }

        model.getDependencies().removeIf(d -> "test".equals(d.getScope()));
        model.getDependencies().removeIf(d -> "camel-stream-starter".equals(d.getArtifactId()));

        if (!config.isWebRequired()) {
            model.getDependencies().removeIf(d -> !"camel-spring-boot-starter".equals(d.getArtifactId()));
        }

        for (String gav : config.getDependencies()) {
            model.addDependency(parseDependency(gav));
        }

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (Writer w = Files.newBufferedWriter(pomFile)) {
            writer.write(w, model);
        }

        Path appProps = projectDir.resolve("src/main/resources/application.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(appProps)) {
            properties.load(in);
        }
        if (config.isWebRequired()) {
            properties.setProperty("server.port", "0");
        } else {
            properties.setProperty("spring.main.web-application-type", "none");
        }
        for (String prop : config.getProperties()) {
            int eq = prop.indexOf('=');
            if (eq >= 0) {
                properties.setProperty(prop.substring(0, eq), prop.substring(eq + 1));
            }
        }
        try (OutputStream out = Files.newOutputStream(appProps)) {
            properties.store(out, null);
        }

        for (Map.Entry<String, String> entry : config.getSourceFiles().entrySet()) {
            Path sourceFile = projectDir.resolve("src/main/java/"
                    + entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, entry.getValue());
        }
    }

    /**
     * Parses a GAV string in the format {@code groupId:artifactId[:version]} into a Maven {@link Dependency}.
     */
    private static Dependency parseDependency(String gav) {
        String[] parts = gav.split(":");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Invalid dependency GAV: '" + gav
                    + "'. Expected format: groupId:artifactId[:version]");
        }
        Dependency dep = new Dependency();
        dep.setGroupId(parts[0]);
        dep.setArtifactId(parts[1]);
        if (parts.length == 3) {
            dep.setVersion(parts[2]);
        }
        return dep;
    }

    public static class GeneratedProject implements AutoCloseable {

        private final Path outputDir;
        private final Path projectDir;

        GeneratedProject(Path outputDir, Path projectDir) {
            this.outputDir = outputDir;
            this.projectDir = projectDir;
        }

        public Path getProjectDir() {
            return projectDir;
        }

        @Override
        public void close() throws Exception {
            if (Boolean.parseBoolean(System.getProperty("delete-after-test", "true"))
                    && outputDir != null && Files.exists(outputDir)) {
                FileUtil.removeDir(outputDir.toFile());
            }
        }
    }
}
