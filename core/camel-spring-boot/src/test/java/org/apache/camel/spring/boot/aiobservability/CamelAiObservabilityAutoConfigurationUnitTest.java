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
package org.apache.camel.spring.boot.aiobservability;

import org.apache.camel.component.ai.observability.GenAiObservability;
import org.apache.camel.component.ai.observability.GenAiObservabilityProperties;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.mock.env.MockEnvironment;

class CamelAiObservabilityAutoConfigurationUnitTest {

    @Test
    void shouldPreferCanonicalPropertyFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(GenAiObservabilityProperties.ENABLED, "false");

        assertThat(CamelAiObservabilityAutoConfiguration.resolveEnabled(environment)).isFalse();
    }

    @Test
    void shouldBindKebabCasePropertyFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("camel.ai-observability.enabled", "false");

        assertThat(CamelAiObservabilityAutoConfiguration.resolveEnabled(environment)).isFalse();
    }

    @Test
    void shouldDefaultToEnabledWhenPropertyIsAbsent() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(CamelAiObservabilityAutoConfiguration.resolveEnabled(environment)).isTrue();
    }

    @Test
    void shouldApplyDisabledPropertyToLocalProperties() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            camelContext.getPropertiesComponent().setLocalProperties(null);

            CamelAiObservabilityAutoConfiguration.applyEnabledProperty(camelContext, false);

            PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
            assertThat(pc.getOverrideProperties())
                    .containsEntry(GenAiObservabilityProperties.ENABLED, "false");
            assertThat(GenAiObservability.isEnabled(camelContext)).isFalse();
        }
    }

    @Test
    void shouldApplyEnabledPropertyToExistingLocalProperties() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            CamelAiObservabilityAutoConfiguration.applyEnabledProperty(camelContext, true);

            PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
            assertThat(pc.getOverrideProperties())
                    .containsEntry(GenAiObservabilityProperties.ENABLED, "true");
            assertThat(GenAiObservability.isEnabled(camelContext)).isTrue();
        }
    }
}
