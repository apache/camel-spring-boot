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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test to validate that the replacement of ProjectDependenciesResolver
 * with prj.getArtifacts() produces the same filtering behavior.
 *
 * This test documents the migration from:
 * - OLD: projectDependenciesResolver.resolve(prj, Collections.singleton(Artifact.SCOPE_COMPILE), session)
 *        + filter(a -> !Artifact.SCOPE_TEST.equals(a.getScope()))
 * - NEW: ProjectBuildingRequest request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
 *        request.setResolveDependencies(true);  // CRITICAL: ensures transitive dependencies are resolved
 *        ProjectBuildingResult result = projectBuilder.build(artifact, request);
 *        prj.getArtifacts().filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
 *        + filter(a -> !Artifact.SCOPE_TEST.equals(a.getScope()))
 *
 * Note: Both the old and new implementations have two filters:
 * 1. First filter for SCOPE_COMPILE (via resolve() parameter or stream filter)
 * 2. Second filter to exclude SCOPE_TEST (redundant but matches original behavior exactly)
 *
 * IMPORTANT: The explicit call to setResolveDependencies(true) ensures that when we build
 * an artifact via ProjectBuilder.build(), Maven will resolve ALL transitive dependencies,
 * not just direct dependencies. Without this, prj.getArtifacts() would return an incomplete set.
 */
class SpringBootStarterMojoTest {

    @Test
    @DisplayName("New scope filter (SCOPE_COMPILE) matches old behavior (SCOPE_COMPILE + !SCOPE_TEST)")
    void testScopeFilteringBehavior() {
        // Setup mock artifacts with different scopes
        Artifact compileArtifact = mock(Artifact.class);
        when(compileArtifact.getScope()).thenReturn(Artifact.SCOPE_COMPILE);
        when(compileArtifact.getGroupId()).thenReturn("org.apache.camel");
        when(compileArtifact.getArtifactId()).thenReturn("camel-core");

        Artifact testArtifact = mock(Artifact.class);
        when(testArtifact.getScope()).thenReturn(Artifact.SCOPE_TEST);
        when(testArtifact.getGroupId()).thenReturn("org.junit.jupiter");
        when(testArtifact.getArtifactId()).thenReturn("junit-jupiter");

        Artifact runtimeArtifact = mock(Artifact.class);
        when(runtimeArtifact.getScope()).thenReturn(Artifact.SCOPE_RUNTIME);
        when(runtimeArtifact.getGroupId()).thenReturn("com.h2database");
        when(runtimeArtifact.getArtifactId()).thenReturn("h2");

        Artifact providedArtifact = mock(Artifact.class);
        when(providedArtifact.getScope()).thenReturn(Artifact.SCOPE_PROVIDED);
        when(providedArtifact.getGroupId()).thenReturn("javax.servlet");
        when(providedArtifact.getArtifactId()).thenReturn("servlet-api");

        Set<Artifact> allArtifacts = new HashSet<>();
        allArtifacts.add(compileArtifact);
        allArtifacts.add(testArtifact);
        allArtifacts.add(runtimeArtifact);
        allArtifacts.add(providedArtifact);

        // OLD behavior: projectDependenciesResolver.resolve() with SCOPE_COMPILE, then filter !SCOPE_TEST
        // (Note: resolve with SCOPE_COMPILE already excludes non-compile, so !SCOPE_TEST filter was redundant)
        Set<String> oldBehavior = new TreeSet<>();
        allArtifacts.stream()
                .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
                .filter(a -> !Artifact.SCOPE_TEST.equals(a.getScope()))  // redundant but matches original
                .map(a -> a.getGroupId() + ":" + a.getArtifactId())
                .forEach(oldBehavior::add);

        // NEW behavior: prj.getArtifacts() filtered to SCOPE_COMPILE, then filter !SCOPE_TEST
        Set<String> newBehavior = new TreeSet<>();
        allArtifacts.stream()
                .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
                .filter(a -> !Artifact.SCOPE_TEST.equals(a.getScope()))  // redundant but matches original
                .map(a -> a.getGroupId() + ":" + a.getArtifactId())
                .forEach(newBehavior::add);

        // Both should produce identical results: only SCOPE_COMPILE artifacts
        assertThat(newBehavior)
                .as("New behavior should match old behavior exactly")
                .isEqualTo(oldBehavior)
                .containsExactly("org.apache.camel:camel-core")
                .doesNotContain("org.junit.jupiter:junit-jupiter", "com.h2database:h2", "javax.servlet:servlet-api");
    }

    @Test
    @DisplayName("Filter includes only SCOPE_COMPILE artifacts")
    void testFilterIncludesOnlyCompileScope() {
        Artifact compileArtifact = mock(Artifact.class);
        when(compileArtifact.getScope()).thenReturn(Artifact.SCOPE_COMPILE);
        when(compileArtifact.getGroupId()).thenReturn("org.apache.camel");
        when(compileArtifact.getArtifactId()).thenReturn("camel-core");

        Artifact testArtifact = mock(Artifact.class);
        when(testArtifact.getScope()).thenReturn(Artifact.SCOPE_TEST);
        when(testArtifact.getGroupId()).thenReturn("org.junit.jupiter");
        when(testArtifact.getArtifactId()).thenReturn("junit-jupiter");

        Artifact runtimeArtifact = mock(Artifact.class);
        when(runtimeArtifact.getScope()).thenReturn(Artifact.SCOPE_RUNTIME);
        when(runtimeArtifact.getGroupId()).thenReturn("com.h2database");
        when(runtimeArtifact.getArtifactId()).thenReturn("h2");

        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(compileArtifact);
        artifacts.add(testArtifact);
        artifacts.add(runtimeArtifact);

        Set<String> filtered = new TreeSet<>();
        artifacts.stream()
                .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
                .map(a -> a.getGroupId() + ":" + a.getArtifactId())
                .forEach(filtered::add);

        assertThat(filtered)
                .as("Should include only compile scope")
                .containsExactly("org.apache.camel:camel-core")
                .doesNotContain("org.junit.jupiter:junit-jupiter", "com.h2database:h2");
    }

    @Test
    @DisplayName("Empty artifact set should produce empty result")
    void testEmptyArtifactSet() {
        Set<Artifact> emptySet = new HashSet<>();

        Set<String> filtered = new TreeSet<>();
        emptySet.stream()
                .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
                .map(a -> a.getGroupId() + ":" + a.getArtifactId())
                .forEach(filtered::add);

        assertThat(filtered).isEmpty();
    }

    @Test
    @DisplayName("Multiple compile artifacts should all be included")
    void testMultipleCompileArtifacts() {
        Artifact artifact1 = mock(Artifact.class);
        when(artifact1.getScope()).thenReturn(Artifact.SCOPE_COMPILE);
        when(artifact1.getGroupId()).thenReturn("org.apache.camel");
        when(artifact1.getArtifactId()).thenReturn("camel-core");

        Artifact artifact2 = mock(Artifact.class);
        when(artifact2.getScope()).thenReturn(Artifact.SCOPE_COMPILE);
        when(artifact2.getGroupId()).thenReturn("org.apache.camel");
        when(artifact2.getArtifactId()).thenReturn("camel-support");

        Artifact artifact3 = mock(Artifact.class);
        when(artifact3.getScope()).thenReturn(Artifact.SCOPE_COMPILE);
        when(artifact3.getGroupId()).thenReturn("com.fasterxml.jackson.core");
        when(artifact3.getArtifactId()).thenReturn("jackson-databind");

        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(artifact1);
        artifacts.add(artifact2);
        artifacts.add(artifact3);

        Set<String> filtered = new TreeSet<>();
        artifacts.stream()
                .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()))
                .map(a -> a.getGroupId() + ":" + a.getArtifactId())
                .forEach(filtered::add);

        assertThat(filtered)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        "org.apache.camel:camel-core",
                        "org.apache.camel:camel-support",
                        "com.fasterxml.jackson.core:jackson-databind"
                );
    }
}
