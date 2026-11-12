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
package org.apache.camel.component.ai.tool.springboot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ai.tool.AiToolComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

/**
 * CAMEL-26782: verifies that when an MCP hint is explicitly configured via application properties
 * its value (even {@code false}) is applied to the component, while hints that are not configured
 * stay {@code null}.
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, AiToolComponentConverter.class,
        AiToolComponentAutoConfiguration.class,
        AiToolMcpHintsConfiguredTest.TestConfiguration.class }, properties = {
                // readOnlyHint explicitly set to true — must be applied
                "camel.component.ai-tool.read-only-hint = true",
                // destructiveHint explicitly set to false — must be applied (equals the config-class
                // default of false, but it was explicitly configured so the component must see false
                // rather than null)
                "camel.component.ai-tool.destructive-hint = false" })
public class AiToolMcpHintsConfiguredTest {

    @Autowired
    CamelContext context;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from("ai-tool:greet?description=Greet a person by name")
                            .setBody().simple("Hello ${header.name}");
                }
            };
        }
    }

    @Test
    void explicitlyConfiguredReadOnlyHintIsApplied() {
        AiToolComponent component = assertInstanceOf(AiToolComponent.class, context.getComponent("ai-tool"));
        assertTrue(component.getConfiguration().getReadOnlyHint(),
                "readOnlyHint configured to true via properties must be applied to the component");
    }

    @Test
    void explicitlyConfiguredDestructiveHintFalseIsApplied() {
        AiToolComponent component = assertInstanceOf(AiToolComponent.class, context.getComponent("ai-tool"));
        // destructiveHint is false in the config-class initializer, but was explicitly set via
        // application properties. BoundConfigurationProperties records the binding, so the value
        // must reach the component even though it equals the default.
        assertFalse(component.getConfiguration().getDestructiveHint(),
                "destructiveHint explicitly configured to false via properties must be applied (not left null)");
    }
}
