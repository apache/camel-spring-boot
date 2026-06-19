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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarFile;

import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates per-starter documentation pages for the Spring Boot documentation site.
 */
@Mojo(name = "update-starter-doc-page", threadSafe = true,
      requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
      defaultPhase = LifecyclePhase.PACKAGE)
public class UpdateStarterDocPageMojo extends AbstractSpringBootGenerator {

    private static final String[] IGNORE_MODULES = { "camel-spring-boot-xml", "camel-spring-boot-engine" };

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}")
    protected File multiModuleDir;

    @Override
    protected boolean isIgnore(String artifactId) {
        return Arrays.asList(IGNORE_MODULES).contains(artifactId);
    }

    @Override
    protected void executeAll() throws MojoExecutionException, MojoFailureException, IOException {
        String starterArtifactId = project.getArtifactId();
        String baseName = starterBaseName(starterArtifactId);

        List<CatalogEntry> entries;
        if (getMainDep() != null) {
            entries = discoverCatalogEntries();
        } else {
            entries = List.of();
        }
        List<SBProperty> properties = readSpringBootProperties(baseName);

        // load optional hand-written sections from src/main/doc/
        String intro = loadSection("intro.adoc");
        String usage = loadSection("usage.adoc");
        String configuration = loadSection("configuration.adoc");
        String limitations = loadSection("limitations.adoc");

        // skip starters with no catalog entries, no properties, and no hand-written sections
        if (entries.isEmpty() && properties.isEmpty() && intro == null && usage == null) {
            getLog().debug("Skipping starter with no content: " + starterArtifactId);
            return;
        }

        String title = determineTitle(baseName, entries);
        String description = determineDescription(entries);

        String page = generatePage(starterArtifactId, baseName, title, description, entries, properties,
                intro, usage, configuration, limitations);

        Path targetPath = multiModuleDir.toPath()
                .resolve("docs/spring-boot/modules/ROOT/pages/starters/" + baseName + ".adoc");
        writeIfChanged(page, targetPath.toFile());
        getLog().info("Generated starter doc page: starters/" + baseName + ".adoc");
    }

    private String loadSection(String fileName) {
        Path docDir = project.getBasedir().toPath().resolve("src/main/doc");
        Path sectionFile = docDir.resolve(fileName);
        if (Files.isRegularFile(sectionFile)) {
            try {
                String content = Files.readString(sectionFile).trim();
                if (!content.isEmpty()) {
                    getLog().debug("Loaded section: " + fileName);
                    return content;
                }
            } catch (IOException e) {
                getLog().warn("Failed to read section file: " + sectionFile, e);
            }
        }
        return null;
    }

    private String starterBaseName(String artifactId) {
        String name = artifactId;
        if (name.startsWith("camel-")) {
            name = name.substring(6);
        }
        if (name.endsWith("-starter")) {
            name = name.substring(0, name.length() - 8);
        }
        return name;
    }

    private List<CatalogEntry> discoverCatalogEntries() throws IOException {
        List<CatalogEntry> entries = new ArrayList<>();

        if ("camel-core".equals(getMainDepArtifactId())) {
            discoverFromJar(getMainDepGroupId(), "camel-core", entries);
            discoverFromJar(getMainDepGroupId(), "camel-base", entries);
            discoverFromJar(getMainDepGroupId(), "camel-core-engine", entries);
            discoverFromJar("org.apache.camel", "camel-core-languages", entries);
        } else {
            discoverFromJar(getMainDepGroupId(), getMainDepArtifactId(), entries);
        }

        entries.sort(Comparator.comparing(e -> e.title));
        return entries;
    }

    private void discoverFromJar(String groupId, String artifactId, List<CatalogEntry> entries) throws IOException {
        try (JarFile jar = getJarFile(groupId, artifactId)) {
            if (jar == null) {
                return;
            }
            Map<String, Supplier<String>> files = getJSonFiles(jar);

            for (String name : findComponentNames(jar)) {
                String json = loadComponentJson(files, name);
                if (json != null) {
                    ComponentModel model = JsonMapper.generateComponentModel(json);
                    String syntax = model.getSyntax() != null ? model.getSyntax() : model.getScheme() + ":destination";
                    entries.add(new CatalogEntry(name, model.getTitle(), model.getDescription(), "component", syntax));
                }
            }
            for (String name : findDataFormatNames(jar)) {
                String json = loadDataFormatJson(files, name);
                if (json != null) {
                    DataFormatModel model = JsonMapper.generateDataFormatModel(json);
                    entries.add(new CatalogEntry(name, model.getTitle(), model.getDescription(), "dataformat", null));
                }
            }
            for (String name : findLanguageNames(jar)) {
                String json = loadLanguageJson(files, name);
                if (json != null) {
                    LanguageModel model = JsonMapper.generateLanguageModel(json);
                    entries.add(new CatalogEntry(name, model.getTitle(), model.getDescription(), "language", null));
                }
            }
        }
    }

    private List<SBProperty> readSpringBootProperties(String baseName) throws IOException {
        List<SBProperty> result = new ArrayList<>();
        Path jsonPath = project.getBasedir().toPath().resolve("src/main/docs/" + baseName + ".json");
        if (!Files.exists(jsonPath)) {
            return result;
        }

        try {
            String content = Files.readString(jsonPath);
            JsonObject root = (JsonObject) Jsoner.deserialize(content);
            JsonArray props = root.getCollectionOrDefault("properties", new JsonArray());
            for (Object item : props) {
                JsonObject prop = (JsonObject) item;
                String name = prop.getStringOrDefault("name", "");
                // skip customizer.enabled entries
                if (name.endsWith(".customizer.enabled")) {
                    continue;
                }
                String desc = prop.getStringOrDefault("description", "");
                String type = prop.getStringOrDefault("type", "");
                Object defaultValue = prop.get("defaultValue");
                result.add(new SBProperty(name, desc, type, defaultValue));
            }
        } catch (Exception e) {
            getLog().warn("Failed to parse Spring Boot properties JSON: " + jsonPath, e);
        }
        return result;
    }

    private String determineTitle(String baseName, List<CatalogEntry> entries) {
        if (!entries.isEmpty()) {
            return entries.get(0).title;
        }
        // fallback: capitalize the base name
        StringBuilder sb = new StringBuilder();
        for (String part : baseName.split("-")) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private String determineDescription(List<CatalogEntry> entries) {
        if (!entries.isEmpty()) {
            return entries.get(0).description;
        }
        return "";
    }

    private String generatePage(
            String starterArtifactId, String baseName, String title,
            String description, List<CatalogEntry> entries, List<SBProperty> properties,
            String intro, String usage, String configuration, String limitations) {

        StringBuilder sb = new StringBuilder();
        sb.append("// Do not edit directly!\n");
        sb.append("// This file was generated by camel-spring-boot-generator-maven-plugin\n");
        sb.append("= ").append(title).append("\n");
        sb.append(":artifactid: ").append(starterArtifactId).append("\n");
        sb.append("\n");

        // intro.adoc overrides the auto-discovered description
        if (intro != null) {
            sb.append(intro).append("\n");
            sb.append("\n");
        } else if (description != null && !description.isEmpty()) {
            sb.append(description).append("\n");
            sb.append("\n");
        }

        // What's inside
        if (!entries.isEmpty()) {
            sb.append("== What's inside\n");
            sb.append("\n");
            for (CatalogEntry entry : entries) {
                String xref = buildXref(entry);
                sb.append("* ").append(xref);
                if ("component".equals(entry.kind) && entry.syntax != null) {
                    sb.append(", URI syntax: `").append(entry.syntax).append("`");
                }
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("Please refer to the above links for usage and configuration details.\n");
            sb.append("\n");
        }

        // Maven coordinates
        sb.append("== Maven coordinates\n");
        sb.append("\n");
        sb.append("[source,xml]\n");
        sb.append("----\n");
        sb.append("<dependency>\n");
        sb.append("    <groupId>org.apache.camel.springboot</groupId>\n");
        sb.append("    <artifactId>").append(starterArtifactId).append("</artifactId>\n");
        sb.append("</dependency>\n");
        sb.append("----\n");
        sb.append("\n");

        // Usage section (from src/main/doc/usage.adoc)
        if (usage != null) {
            sb.append("== Usage\n");
            sb.append("\n");
            sb.append(usage).append("\n");
            sb.append("\n");
        }

        // Limitations section (from src/main/doc/limitations.adoc)
        if (limitations != null) {
            sb.append("== Limitations\n");
            sb.append("\n");
            sb.append(limitations).append("\n");
            sb.append("\n");
        }

        // Spring Boot Auto-Configuration
        if (!properties.isEmpty()) {
            sb.append("== Spring Boot Auto-Configuration\n");
            sb.append("\n");

            // Additional configuration notes (from src/main/doc/configuration.adoc)
            if (configuration != null) {
                sb.append(configuration).append("\n");
                sb.append("\n");
            }

            sb.append("The starter supports ").append(properties.size()).append(" options, which are listed below.\n");
            sb.append("\n");
            sb.append("[width=\"100%\",cols=\"2,5,^1,2\",options=\"header\"]\n");
            sb.append("|===\n");
            sb.append("| Name | Description | Default | Type\n");
            for (SBProperty prop : properties) {
                sb.append("| ").append(prop.name);
                sb.append(" | ").append(escapeCell(prop.description));
                sb.append(" | ").append(prop.defaultValue != null ? prop.defaultValue : "");
                sb.append(" | ").append(javaSimpleName(prop.type));
                sb.append("\n");
            }
            sb.append("|===\n");
        }

        return sb.toString();
    }

    private String buildXref(CatalogEntry entry) {
        return switch (entry.kind) {
            case "component" ->
                    "xref:components::" + entry.name + "-component.adoc[" + entry.title + " component]";
            case "dataformat" ->
                    "xref:components:dataformats:" + entry.name + "-dataformat.adoc[" + entry.title + " data format]";
            case "language" ->
                    "xref:components:languages:" + entry.name + "-language.adoc[" + entry.title + " language]";
            default ->
                    "xref:components:others:" + entry.name + ".adoc[" + entry.title + "]";
        };
    }

    private static String escapeCell(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // escape pipe characters in AsciiDoc table cells
        return text.replace("|", "\\|");
    }

    private static String javaSimpleName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) {
            return "";
        }
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    private record CatalogEntry(String name, String title, String description, String kind, String syntax) {
    }

    private record SBProperty(String name, String description, String type, Object defaultValue) {
    }
}
