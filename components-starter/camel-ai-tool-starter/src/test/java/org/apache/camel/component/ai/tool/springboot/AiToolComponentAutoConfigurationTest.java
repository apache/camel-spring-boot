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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ai.tool.AiToolComponent;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, AiToolComponentConverter.class,
        AiToolComponentAutoConfiguration.class,
        AiToolComponentAutoConfigurationTest.TestConfiguration.class }, properties = {
                "camel.component.ai-tool.tags = demo" })
public class AiToolComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from("ai-tool:greet"
                         + "?description=Greet a person by name"
                         + "&parameter.name=string"
                         + "&parameter.name.description=Who to greet"
                         + "&parameter.name.required=true")
                            .setBody().simple("Hello ${header.name}");
                }
            };
        }
    }

    @Test
    public void componentIsAutoConfigured() {
        AiToolComponent component = assertInstanceOf(AiToolComponent.class, context.getComponent("ai-tool"));
        // set through camel.component.ai-tool.tags, so this asserts the Spring Boot
        // properties are copied onto the component by the starter
        assertEquals("demo", component.getConfiguration().getTags());
    }

    @Test
    public void routeIsRegisteredAsToolUnderTheConfiguredTag() {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(context);
        assertTrue(registry.getDefaultTools().isEmpty(), "a tagged tool must not land in the default pool");

        Set<AiToolSpec> tools = registry.getToolsByTag("demo");
        assertEquals(1, tools.size());

        AiToolSpec spec = tools.iterator().next();
        assertEquals("greet", spec.getName());
        assertEquals("Greet a person by name", spec.getDescription());
        assertTrue(spec.getParametersJsonSchema().contains("\"name\""),
                "expected the declared parameter in the JSON schema, but was: " + spec.getParametersJsonSchema());
    }
}
