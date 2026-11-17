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
                AiObservabilityAutoConfigurationOptionsTest.TestConfiguration.class,
                AiObservabilityAutoConfiguration.class,
                CamelAutoConfiguration.class
        },
        properties = {
                "camel.aiobservability.level=full",
                "camel.ai-observability.content-max-length=100",
                "camel.aiObservability.deferToFramework=false" })
class AiObservabilityAutoConfigurationOptionsTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    AiObservabilityConfigurationProperties configurationProperties;

    @Test
    void shouldBindOptionsAndHandThemToCamel() {
        assertThat(configurationProperties.getLevel()).isEqualTo("full");
        assertThat(configurationProperties.getContentMaxLength()).isEqualTo(100);

        PropertiesComponent pc = (PropertiesComponent) camelContext.getPropertiesComponent();
        assertThat(pc.getOverrideProperties())
                .containsEntry(GenAiObservabilityProperties.LEVEL, "full")
                .containsEntry(GenAiObservabilityProperties.CONTENT_MAX_LENGTH, "100")
                .containsEntry(GenAiObservabilityProperties.DEFER_TO_FRAMEWORK, "false");
        assertThat(camelContext.resolvePropertyPlaceholders("{{camel.aiObservability.level}}")).isEqualTo("full");
    }

    @Configuration
    static class TestConfiguration {
    }
}
