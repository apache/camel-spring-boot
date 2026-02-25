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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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
            Path targetDir = Path.of(requireSystemProperty("project.build.directory"));
            Files.createDirectories(targetDir);
            Path outputDir = Files.createTempDirectory(targetDir, "archetype-test-");

            DefaultContainerConfiguration config = new DefaultContainerConfiguration();
            config.setClassPathScanning(PlexusConstants.SCANNING_ON);
            DefaultPlexusContainer container = new DefaultPlexusContainer(config);
            ArchetypeGenerator generator = container.lookup(ArchetypeGenerator.class);

            String archetypeVersion = requireSystemProperty("project-version");
            String camelVersion = requireSystemProperty("camel-version");
            String springBootVersion = requireSystemProperty("spring-boot-version");
            String mavenCompilerPluginVersion = requireSystemProperty("maven-compiler-plugin-version");

            File archetypeFile = resolveArchetypeJar(archetypeVersion);
            if (!archetypeFile.exists()) {
                throw new IllegalStateException("Archetype JAR not found at: " + archetypeFile
                        + ". Run 'mvn install -pl archetypes/camel-archetype-spring-boot -am -DskipTests' first.");
            }

            Properties additionalProperties = new Properties();
            additionalProperties.setProperty("camel-version", camelVersion);
            additionalProperties.setProperty("spring-boot-version", springBootVersion);
            additionalProperties.setProperty("maven-compiler-plugin-version", mavenCompilerPluginVersion);

            ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
                    .setArchetypeGroupId(ARCHETYPE_GROUP_ID)
                    .setArchetypeArtifactId(ARCHETYPE_ARTIFACT_ID)
                    .setArchetypeVersion(archetypeVersion)
                    .setGroupId(archetypeConfig.groupId)
                    .setArtifactId(archetypeConfig.artifactId)
                    .setVersion(archetypeConfig.version)
                    .setPackage(archetypeConfig.packageName)
                    .setOutputDirectory(outputDir.toString())
                    .setProperties(additionalProperties);

            ArchetypeGenerationResult result = new ArchetypeGenerationResult();
            generator.generateArchetype(request, archetypeFile, result);

            Path projectDir = outputDir.resolve(archetypeConfig.artifactId);
            customizeArchetype(projectDir, archetypeConfig);
            return new GeneratedProject(outputDir, projectDir, result);
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

        FileUtils.deleteQuietly(projectDir.resolve("src/test").toFile());

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
        private final ArchetypeGenerationResult result;

        GeneratedProject(Path outputDir, Path projectDir, ArchetypeGenerationResult result) {
            this.outputDir = outputDir;
            this.projectDir = projectDir;
            this.result = result;
        }

        public Path getProjectDir() {
            return projectDir;
        }

        public ArchetypeGenerationResult getResult() {
            return result;
        }

        @Override
        public void close() throws Exception {
            if (Boolean.parseBoolean(System.getProperty("delete-after-test", "true"))
                    && outputDir != null && Files.exists(outputDir)) {
                FileUtils.deleteQuietly(outputDir.toFile());
            }
        }
    }
}
