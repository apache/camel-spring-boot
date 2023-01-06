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
package org.apache.camel.spring.boot;

import javax.annotation.PostConstruct;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = {
        CamelAutoConfiguration.class,
        CamelConfigurationPostConstructAddRouteToRegistryTest.Config.class
})
public class CamelConfigurationPostConstructAddRouteToRegistryTest {

    @Configuration
    static class Config {

        @Autowired
        CamelContext camelContext;

        @PostConstruct
        public void doPost() {
            camelContext.getRegistry().bind("myExtraRoute", new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:extra")
                            .to("mock:extra");
                }
            });
        }

    }

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    @Test
    public void shouldAddRoute() throws Exception {
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:extra", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody("direct:extra", "Hello World");

        mockEndpoint.assertIsSatisfied();
    }

}
