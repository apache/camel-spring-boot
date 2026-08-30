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

import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.observability.GenAiObservability;
import org.apache.camel.component.ai.observability.GenAiObservabilityProperties;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                AiObservabilityAutoConfigurationRelaxedBindingTest.TestConfiguration.class,
                AiObservabilityAutoConfiguration.class,
                CamelAutoConfiguration.class
        },
        properties = "camel.aiObservability.enabled=false")
class AiObservabilityAutoConfigurationRelaxedBindingTest {

    @Autowired
    CamelContext camelContext;

    @Test
    void shouldDisableGenAiObservabilityWithCamelCaseProperty() {
        assertThat(GenAiObservability.isEnabled(camelContext)).isFalse();

        PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
        assertThat(pc.getLocalProperties())
                .containsEntry(GenAiObservabilityProperties.ENABLED, "false");
    }

    @Configuration
    static class TestConfiguration {
    }
}
