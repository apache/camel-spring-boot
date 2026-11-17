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
package org.apache.camel.ai.observability.starter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.camel.component.ai.observability.GenAiObservability;
import org.apache.camel.component.ai.observability.GenAiObservabilityProperties;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiObservabilityAutoConfigurationUnitTest {

    @Test
    void shouldPreferCanonicalPropertyFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(GenAiObservabilityProperties.ENABLED, "false");

        assertThat(AiObservabilityAutoConfiguration.resolveEnabled(environment)).isFalse();
    }

    @Test
    void shouldBindKebabCasePropertyFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("camel.ai-observability.enabled", "false");

        assertThat(AiObservabilityAutoConfiguration.resolveEnabled(environment)).isFalse();
    }

    @Test
    void shouldDefaultToEnabledWhenPropertyIsAbsent() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(AiObservabilityAutoConfiguration.resolveEnabled(environment)).isTrue();
    }

    @Test
    void shouldApplyDisabledPropertyAsOverrideProperty() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            AiObservabilityAutoConfiguration.applyProperties(camelContext,
                    Map.of(GenAiObservabilityProperties.ENABLED, "false"));

            PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
            assertThat(pc.getOverrideProperties())
                    .containsEntry(GenAiObservabilityProperties.ENABLED, "false");
            assertThat(GenAiObservability.isEnabled(camelContext)).isFalse();
        }
    }

    @Test
    void shouldApplyEnabledPropertyAsOverrideProperty() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            AiObservabilityAutoConfiguration.applyProperties(camelContext,
                    Map.of(GenAiObservabilityProperties.ENABLED, "true"));

            PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
            assertThat(pc.getOverrideProperties())
                    .containsEntry(GenAiObservabilityProperties.ENABLED, "true");
            assertThat(GenAiObservability.isEnabled(camelContext)).isTrue();
        }
    }

    @Test
    void shouldResolveDefaultsOfAllOptions() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(AiObservabilityAutoConfiguration.resolveProperties(environment))
                .containsEntry(GenAiObservabilityProperties.ENABLED, "true")
                .containsEntry(GenAiObservabilityProperties.LEVEL, "basic")
                .containsEntry(GenAiObservabilityProperties.CONTENT_MAX_LENGTH, "8192")
                .containsEntry(GenAiObservabilityProperties.DEFER_TO_FRAMEWORK, "true");
    }

    @Test
    void shouldResolveOptionsFromRelaxedAndCanonicalKeys() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("camel.aiobservability.level", "detailed");
        environment.setProperty("camel.ai-observability.content-max-length", "100");
        environment.setProperty(GenAiObservabilityProperties.DEFER_TO_FRAMEWORK, "false");

        assertThat(AiObservabilityAutoConfiguration.resolveProperties(environment))
                .containsEntry(GenAiObservabilityProperties.LEVEL, "detailed")
                .containsEntry(GenAiObservabilityProperties.CONTENT_MAX_LENGTH, "100")
                .containsEntry(GenAiObservabilityProperties.DEFER_TO_FRAMEWORK, "false");
    }

    @Test
    void shouldPreferCanonicalKeyOverRelaxedBinding() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("camel.ai-observability.level", "detailed");
        environment.setProperty(GenAiObservabilityProperties.LEVEL, "full");

        assertThat(AiObservabilityAutoConfiguration.resolveProperties(environment))
                .containsEntry(GenAiObservabilityProperties.LEVEL, "full");
    }

    @Test
    void shouldShipTheImplementationTheProducersLoad() {
        // GenAiObservability loads it reflectively; without it every AI producer records nothing
        assertThat(ClassUtils.isPresent("org.apache.camel.component.ai.observability.GenAiObservabilityImpl",
                getClass().getClassLoader())).isTrue();
    }

    @Test
    void shouldMakeOptionsVisibleToOtherThreads() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            AiObservabilityAutoConfiguration.applyProperties(camelContext,
                    Map.of(GenAiObservabilityProperties.LEVEL, "detailed"));

            // the AI producers run on the threads that route the exchanges, not on the startup thread
            CompletableFuture<Optional<String>> level = CompletableFuture.supplyAsync(
                    () -> camelContext.getPropertiesComponent().resolveProperty(GenAiObservabilityProperties.LEVEL));
            assertThat(level.get(10, TimeUnit.SECONDS)).contains("detailed");
        }
    }
}
