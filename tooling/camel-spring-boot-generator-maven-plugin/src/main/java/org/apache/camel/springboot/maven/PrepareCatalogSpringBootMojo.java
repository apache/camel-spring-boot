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
package org.apache.camel.springboot.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Prepares the Spring Boot provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-springboot", threadSafe = true,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PrepareCatalogSpringBootMojo extends AbstractSpringBootGenerator {

    protected static final String[] IGNORE_MODULES = {/* Non-standard -> */ "camel-grape"};

    /**
     * The catalog directory
     */
    @Parameter(defaultValue = "${basedir}/../../camel-catalog-provider-springboot")
    protected File catalogDir;

    protected void executeAll() throws MojoExecutionException, MojoFailureException, IOException {
        if ("camel-core".equals(getMainDepArtifactId())) {
            executeAll(getMainDepGroupId(), "camel-core");
            executeAll(getMainDepGroupId(), "camel-base");
            executeAll(getMainDepGroupId(), "camel-core-engine");
        } else {
            executeAll(getMainDepGroupId(), getMainDepArtifactId());
        }
    }

    private void executeAll(String groupId, String artifactId) throws MojoExecutionException, MojoFailureException, IOException {
        try (JarFile componentJar = getJarFile(groupId, artifactId)) {
            Map<String, Supplier<String>> files = getJSonFiles(componentJar);
            executeOthers(componentJar, files);
            executeComponents(componentJar, files);
            executeDataFormats(componentJar, files);
            executeLanguages(componentJar, files);
        }
    }

    @Override
    protected boolean isIgnore(String artifactId) {
        return Arrays.asList(IGNORE_MODULES).contains(artifactId);
    }

    protected void executeComponents(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoExecutionException, MojoFailureException, IOException {
        List<String> componentNames = findComponentNames(componentJar);
        if (!componentNames.isEmpty()) {
            getLog().info("Components found: " + String.join(", ", componentNames));
            List<String> actual = new ArrayList<>();
            for (String componentName : componentNames) {
                String json = loadComponentJson(jsonFiles, componentName);
                if (json != null) {
                    json = json.replace("\"groupId\": \"" + getMainDepGroupId() + "\"", "\"groupId\": \"" + project.getGroupId() + "\"")
                               .replace("\"artifactId\": \"" + getMainDepArtifactId() + "\"", "\"artifactId\": \"" + project.getArtifactId() + "\"")
                               .replace("\"version\": \"" + getMainDepVersion() + "\"", "\"version\": \"" + project.getVersion() + "\"");
                    writeIfChanged(json, new File(catalogDir,
                            "src/main/resources/org/apache/camel/springboot/catalog/components/" + componentName + ".json"));
                    actual.add(componentName);
                }
            }
            File components = new File(catalogDir, "src/main/resources/org/apache/camel/springboot/catalog/components.properties");
            Stream<String> existing = components.isFile() ? Files.lines(components.toPath()) : Stream.empty();
            String content = Stream.concat(existing, actual.stream())
                    .sorted().distinct()
                    .collect(Collectors.joining("\n"));
            writeIfChanged(content, components);
        }
    }

    protected void executeDataFormats(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoExecutionException, MojoFailureException, IOException {
        List<String> dataFormatNames = findDataFormatNames(componentJar);
        if (!dataFormatNames.isEmpty()) {
            getLog().info("Dataformats found: " + String.join(", ", dataFormatNames));
            List<String> actual = new ArrayList<>();
            for (String dataformatName : dataFormatNames) {
                String json = loadDataFormatJson(jsonFiles, dataformatName);
                if (json != null) {
                    json = json.replace("\"groupId\": \"" + getMainDepGroupId() + "\"", "\"groupId\": \"" + project.getGroupId() + "\"")
                               .replace("\"artifactId\": \"" + getMainDepArtifactId() + "\"", "\"artifactId\": \"" + project.getArtifactId() + "\"")
                               .replace("\"version\": \"" + getMainDepVersion() + "\"", "\"version\": \"" + project.getVersion() + "\"");
                    writeIfChanged(json, new File(catalogDir,
                            "src/main/resources/org/apache/camel/springboot/catalog/dataformats/" + dataformatName + ".json"));
                    actual.add(dataformatName);
                }
            }
            File dataformats = new File(catalogDir, "src/main/resources/org/apache/camel/springboot/catalog/dataformats.properties");
            Stream<String> existing = dataformats.isFile() ? Files.lines(dataformats.toPath()) : Stream.empty();
            String content = Stream.concat(existing, actual.stream())
                    .sorted().distinct()
                    .collect(Collectors.joining("\n"));
            writeIfChanged(content, dataformats);
        }
    }

    protected void executeLanguages(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoExecutionException, MojoFailureException, IOException {
        List<String> languageNames = findLanguageNames(componentJar);
        if (!languageNames.isEmpty()) {
            getLog().info("Languages found: " + String.join(", ", languageNames));
            List<String> actual = new ArrayList<>();
            for (String languageName : languageNames) {
                String json = loadLanguageJson(jsonFiles, languageName);
                if (json != null) {
                    json = json.replace("\"groupId\": \"" + getMainDepGroupId() + "\"", "\"groupId\": \"" + project.getGroupId() + "\"")
                               .replace("\"artifactId\": \"" + getMainDepArtifactId() + "\"", "\"artifactId\": \"" + project.getArtifactId() + "\"")
                               .replace("\"version\": \"" + getMainDepVersion() + "\"", "\"version\": \"" + project.getVersion() + "\"");
                    writeIfChanged(json, new File(catalogDir,
                            "src/main/resources/org/apache/camel/springboot/catalog/languages/" + languageName + ".json"));
                    actual.add(languageName);
                }
            }
            File languages = new File(catalogDir, "src/main/resources/org/apache/camel/springboot/catalog/languages.properties");
            Stream<String> existing = languages.isFile() ? Files.lines(languages.toPath()) : Stream.empty();
            String content = Stream.concat(existing, actual.stream())
                    .sorted().distinct()
                    .collect(Collectors.joining("\n"));
            writeIfChanged(content, languages);
        }
    }

    protected void executeOthers(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoExecutionException, MojoFailureException, IOException {
        // The json files for 'other' components are in the root of the jars
        List<String> otherNames = findNames(componentJar, "").stream()
                .map(s -> s.substring(0, s.length() - ".json".length()))
                .collect(Collectors.toList());
        if (!otherNames.isEmpty()) {
            getLog().info("Others found: " + String.join(", ", otherNames));
            List<String> actual = new ArrayList<>();
            for (String otherName : otherNames) {
                String json = loadOtherJson(jsonFiles, otherName);
                if (json != null) {
                    json = json.replace("\"groupId\": \"" + getMainDepGroupId() + "\"", "\"groupId\": \"" + project.getGroupId() + "\"")
                            .replace("\"artifactId\": \"" + getMainDepArtifactId() + "\"", "\"artifactId\": \"" + project.getArtifactId() + "\"")
                            .replace("\"version\": \"" + getMainDepVersion() + "\"", "\"version\": \"" + project.getVersion() + "\"");
                    writeIfChanged(json, new File(catalogDir,
                            "src/main/resources/org/apache/camel/springboot/catalog/others/" + otherName + ".json"));
                    actual.add(otherName);
                }
            }
            File others = new File(catalogDir, "src/main/resources/org/apache/camel/springboot/catalog/others.properties");
            Stream<String> existing = others.isFile() ? Files.lines(others.toPath()) : Stream.empty();
            String content = Stream.concat(existing, actual.stream())
                    .sorted().distinct()
                    .collect(Collectors.joining("\n"));
            writeIfChanged(content, others);
        }
    }

}
