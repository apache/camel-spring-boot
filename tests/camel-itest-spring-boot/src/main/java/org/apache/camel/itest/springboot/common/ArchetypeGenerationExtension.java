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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ArchetypeGenerationExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ArchetypeGenerationExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(ArchetypeGenerationExtension.class);

    private static final String ARCHETYPE_GROUP_ID = "org.apache.camel.archetypes";
    private static final String ARCHETYPE_ARTIFACT_ID = "camel-archetype-spring-boot";
    /** artifactId of the project generated once per build and copied for every test, see {@link #templateProject} */
    private static final String TEMPLATE_ARTIFACT_ID = "archetype-template";

    private final Supplier<ArchetypeConfig> configSupplier;
    private ArchetypeConfig archetypeConfig;
    private GeneratedProject generatedProject;

    public ArchetypeGenerationExtension() {
        this(ArchetypeConfig.builder().build());
    }

    public ArchetypeGenerationExtension(ArchetypeConfig archetypeConfig) {
        this(() -> archetypeConfig);
    }

    /**
     * The supplier is only called when the project is first needed, so a test class can register the
     * extension in a field initializer and still build its configuration from its own fields.
     */
    public ArchetypeGenerationExtension(Supplier<ArchetypeConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ensureProjectCreated(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // only reached without a preceding beforeAll (PER_METHOD lifecycle); register the project in the
        // class-level store, otherwise it would be deleted after the first test method and reused by the next
        ensureProjectCreated(context.getParent().orElse(context));
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

    public ArchetypeConfig getArchetypeConfig() {
        if (archetypeConfig == null) {
            archetypeConfig = configSupplier.get();
        }
        return archetypeConfig;
    }

    public GeneratedProject getGeneratedProject() {
        return generatedProject;
    }

    private GeneratedProject createProject() {
        try {
            ArchetypeConfig config = getArchetypeConfig();
            ((LoggerContext) LoggerFactory.getILoggerFactory()).setName(config.getArtifactId());
            Path targetDir = Path.of(requireSystemProperty("project.build.directory"));
            Files.createDirectories(targetDir);

            Path template = templateProject(targetDir, config);
            Path outputDir = Files.createTempDirectory(targetDir, "archetype-test-");
            Path projectDir = outputDir.resolve(config.getArtifactId());
            copyDirectory(template, projectDir);
            customizeArchetype(projectDir, config);
            return new GeneratedProject(outputDir, projectDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate archetype project", e);
        }
    }

    /**
     * Returns the project generated from the archetype, generating it on first use.
     * <p>
     * {@code archetype:generate} is a Maven JVM start per invocation and costs about as much as the rest of
     * a starter test together, while its output only depends on the archetype and the versions passed to
     * it. So the project is generated once per build under {@code target/archetype-templates/<key>} and
     * copied for every test. The key includes the archetype jar's timestamp and size, so a rebuilt archetype
     * is picked up without a clean. Concurrent failsafe forks are serialised with a file lock, and the
     * template directory only appears once it is complete (atomic move).
     */
    private static Path templateProject(Path targetDir, ArchetypeConfig config) throws Exception {
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

        String key = Integer.toHexString(Objects.hash(archetypeFile.lastModified(), archetypeFile.length(),
                config.getGroupId(), config.getVersion(), config.getPackageName(),
                camelVersion, springBootVersion, mavenCompilerPluginVersion, mavenVersion));
        Path templatesDir = targetDir.resolve("archetype-templates");
        Path template = templatesDir.resolve(key);
        if (Files.isDirectory(template)) {
            return template;
        }

        Files.createDirectories(templatesDir);
        Path lockFile = templatesDir.resolve(key + ".lock");
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            if (Files.isDirectory(template)) {
                // generated by another fork while waiting for the lock
                return template;
            }
            LOG.info("Generating archetype template project in {}", template);
            Path workDir = Files.createTempDirectory(templatesDir, key + "-");
            runMaven(workDir, "archetype:generate",
                    "-DarchetypeGroupId=" + ARCHETYPE_GROUP_ID,
                    "-DarchetypeArtifactId=" + ARCHETYPE_ARTIFACT_ID,
                    "-DarchetypeVersion=" + archetypeVersion,
                    "-DgroupId=" + config.getGroupId(),
                    "-DartifactId=" + TEMPLATE_ARTIFACT_ID,
                    "-Dversion=" + config.getVersion(),
                    "-Dpackage=" + config.getPackageName(),
                    "-Dcamel-version=" + camelVersion,
                    "-Dspring-boot-version=" + springBootVersion,
                    "-Dmaven-compiler-plugin-version=" + mavenCompilerPluginVersion,
                    "-Dmaven-version=" + mavenVersion);
            Files.move(workDir.resolve(TEMPLATE_ARTIFACT_ID), template, StandardCopyOption.ATOMIC_MOVE);
            FileUtil.removeDir(workDir.toFile());
        }
        return template;
    }

    /**
     * Runs Maven in {@code workDir} and returns its output, failing with the output on a non-zero exit.
     * <p>
     * The invocation is quiet, in batch mode and skips snapshot update checks ({@code -nsu}): every snapshot
     * the generated project needs was just installed by the reactor build, and the remote metadata check
     * costs up to a second per invocation. Missing artifacts are still downloaded.
     */
    static String runMaven(Path workDir, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(resolveMavenCommand());
        command.addAll(List.of("-q", "-B", "-nsu"));
        command.addAll(List.of(args));
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo != null) {
            command.add("-Dmaven.repo.local=" + localRepo);
        }
        LOG.debug("running: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        // The generated project compiles a single class: a small heap keeps the parallel forks from each
        // reserving the heap configured in .mvn/jvm.config, which the child inherits. A -Xmx in the
        // caller's MAVEN_OPTS comes last and therefore wins.
        pb.environment().merge("MAVEN_OPTS", "-Xmx1g", (user, ours) -> ours + " " + user);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(String.join(" ", command) + " failed (exit code "
                    + exitCode + ") in " + workDir + ":" + System.lineSeparator() + output);
        }
        return output;
    }

    private static String requireSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Required system property '" + key + "' is not set");
        }
        return value;
    }

    static String resolveMavenCommand() {
        String command = requireSystemProperty("mvn-command");
        if (command.startsWith("./")) {
            String projectRoot = requireSystemProperty("maven.multiModuleProjectDirectory");
            return Path.of(projectRoot, command.substring(2)).toString();
        }
        return command;
    }

    private static File resolveArchetypeJar(String version) {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        return Path.of(localRepo, ARCHETYPE_GROUP_ID.replace('.', File.separatorChar)
                , ARCHETYPE_ARTIFACT_ID, version, ARCHETYPE_ARTIFACT_ID + "-" + version + ".jar").toFile();
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
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
            try (Stream<Path> paths = Files.walk(mainJavaDir)) {
                for (Path path : (Iterable<Path>) paths::iterator) {
                    if (path.toString().endsWith(".java") && !path.getFileName().toString().equals(mainClassFile)) {
                        Files.delete(path);
                    }
                }
            }
        }

        Path pomFile = projectDir.resolve("pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (Reader r = Files.newBufferedReader(pomFile)) {
            model = reader.read(r);
        }

        // the project was generated once with a placeholder artifactId, see templateProject
        model.setArtifactId(config.getArtifactId());

        model.getDependencies().removeIf(d -> "test".equals(d.getScope()));
        // only used by the sample route deleted above
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
