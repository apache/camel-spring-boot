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
package org.apache.camel.itest.springboot.catalog;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.model.ArtifactModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Verifies that every component, data format, and language in the Camel catalog
 * that has a corresponding Spring Boot starter also has an integration test class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CamelCatalogIT {

    private static final String TEST_PACKAGE = "org.apache.camel.itest.springboot";

    private CamelCatalog catalog;
    private Set<String> existingStarters;

    @BeforeAll
    void setup() throws IOException {
        catalog = new DefaultCamelCatalog();

        Path buildDir = Path.of(System.getProperty("project.build.directory", "target"));
        Path projectRoot = buildDir.getParent().getParent().getParent();
        Path startersDir = projectRoot.resolve("components-starter");

        existingStarters = new TreeSet<>();
        if (Files.isDirectory(startersDir)) {
            try (Stream<Path> stream = Files.list(startersDir)) {
                stream.filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.startsWith("camel-") && n.endsWith("-starter"))
                        .forEach(existingStarters::add);
            }
        }
    }

    @Test
    void allComponentsCovered() {
        assertCoverage("component", catalog.findComponentNames(),
                name -> catalog.componentModel(name));
    }

    @Test
    void allDataFormatsCovered() {
        assertCoverage("dataformat", catalog.findDataFormatNames(),
                name -> catalog.dataFormatModel(name));
    }

    @Test
    void allLanguagesCovered() {
        assertCoverage("language", catalog.findLanguageNames(),
                name -> catalog.languageModel(name));
    }

    private void assertCoverage(String kind, List<String> names,
            Function<String, ? extends ArtifactModel<?>> modelResolver) {

        Map<String, Set<String>> missingByArtifact = new TreeMap<>();

        for (String name : names) {
            ArtifactModel<?> model = modelResolver.apply(name);
            if (model == null) {
                continue;
            }

            String artifactId = model.getArtifactId();
            String starterName = artifactId + "-starter";
            if (!existingStarters.contains(starterName)) {
                continue;
            }

            String expectedClass = TEST_PACKAGE + "." + artifactIdToClassName(artifactId);
            try {
                Class.forName(expectedClass);
            } catch (ClassNotFoundException e) {
                missingByArtifact
                        .computeIfAbsent(artifactId, k -> new TreeSet<>())
                        .add(name);
            }
        }

        if (!missingByArtifact.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing ").append(kind).append(" integration tests:\n");
            for (Map.Entry<String, Set<String>> entry : missingByArtifact.entrySet()) {
                sb.append("  ").append(entry.getKey()).append("-starter")
                        .append(" (").append(String.join(", ", entry.getValue())).append(")\n");
            }
            fail(sb.toString());
        }
    }

    /**
     * Converts a Maven artifactId to the expected test class name.
     * E.g. {@code camel-aws2-s3} to {@code CamelAws2S3IT}
     */
    static String artifactIdToClassName(String artifactId) {
        StringBuilder sb = new StringBuilder();
        for (String part : artifactId.split("-")) {
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        sb.append("IT");
        return sb.toString();
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
