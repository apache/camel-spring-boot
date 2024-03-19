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
package org.apache.camel.spring.boot.xml;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rest.RestEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        properties = {
                "camel.springboot.routes-include-pattern=classpath:camel-xml-io-dsl.xml",
                "spring.main.allow-circular-references=true"
        })
public class MixedRestDslTest {

    @Autowired
    CamelContext camelContext;

    @Test
    public void testMultipleRestDslDefinition() {
        Assertions.assertThat(camelContext.getRoutes())
                .filteredOn(
                        route -> route.getEndpoint() instanceof RestEndpoint
                )
                .map(route -> ((RestEndpoint) route.getEndpoint()).getPath())
                .containsExactlyInAnyOrder(
                        "java",
                        "xml-io",
                        "spring");
    }

    @Configuration
    @EnableAutoConfiguration
    @ImportResource(value = { "classpath:spring-camel-context.xml" })
    public static class TestConfiguration {

        @Bean
        protected RouteBuilder createRouteBuilder() throws Exception {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest("java").get().to("direct:test");
                }
            };
        }
    }
}
