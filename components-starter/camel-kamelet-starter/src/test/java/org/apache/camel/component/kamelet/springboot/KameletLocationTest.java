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
package org.apache.camel.component.kamelet.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.junit.jupiter.api.Test;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        KameletLocationTest.class,
    }
)

public class KameletLocationTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;

    @Autowired
    private CamelContext context;

    protected CamelContext createCamelContext() throws Exception {
        context.getRegistry().bind("routes-builder-loader-xml", new MyRoutesLoader());
        return context;
    }

    @Test
    public void testOne() throws Exception {
        mock.expectedBodiesReceived("HELLO");

        template.sendBody("direct:start", "Hello");

        mock.assertIsSatisfied();
    }

    @Test
    public void testTwo() throws Exception {
        mock.reset();
        mock.expectedBodiesReceived("HELLO", "WORLD");

        template.sendBody("direct:start", "Hello");
        template.sendBody("direct:start", "World");

        mock.assertIsSatisfied();
    }

    // we cannot use camel-xml-io-dsl to load the XML route so we fool Camel
    // and use this class that has the route template hardcoded from java
    public class MyRoutesLoader implements RoutesBuilderLoader {


        @Override
        public CamelContext getCamelContext() {
            return context;
        }

        @Override
        public void setCamelContext(CamelContext camelContext) {
            context = camelContext;
        }

        @Override
        public String getSupportedExtension() {
            return "xml";
        }

        @Override
        public RoutesBuilder loadRoutesBuilder(Resource resource) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    routeTemplate("upper")
                            .from("kamelet:source")
                            .transform().simple("${body.toUpperCase()}");

                    from("direct:start")
                        .kamelet("upper?location=file:resource:classpath:upper-kamelet.xml")
                        .to("mock:result");        
                }
            };
        }

        @Override
        public void start() {
            // noop
        }

        @Override
        public void stop() {
            // noop
        }
    }

    // **********************************************
    //
    // test set-up
    //
    // **********************************************

    @Bean
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .kamelet("upper?location=file:src/test/resources/upper-kamelet.xml")
                        .to("mock:result");
            }
        };
    }
}