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
package org.apache.camel.component.jsonpath.springboot.test;


import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        EasyJsonPathWithSimpleCBRTest.class,
        EasyJsonPathWithSimpleCBRTest.TestConfiguration.class
    }
)
public class EasyJsonPathWithSimpleCBRTest {

    @Autowired
    CamelContext  context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:cheap")
    MockEndpoint mockCheap;
    
    @EndpointInject("mock:average")
    MockEndpoint mockAverage;
    
    @EndpointInject("mock:expensive")
    MockEndpoint mockExpensive;
    
    FluentProducerTemplate fluentTemplate;

    @Test
    public void testCheap() throws Exception {
        fluentTemplate = context.createFluentProducerTemplate();
        fluentTemplate.start();
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(1);
        mockAverage.expectedMessageCount(0);
        mockExpensive.expectedMessageCount(0);
        fluentTemplate.withHeader("cheap", 10).withHeader("average", 30).withBody(new File("src/test/resources/cheap.json"))
                .to("direct:start").send();
        

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testAverage() throws Exception {
        fluentTemplate = context.createFluentProducerTemplate();
        fluentTemplate.start();
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(0);
        mockAverage.expectedMessageCount(1);
        mockExpensive.expectedMessageCount(0);

        fluentTemplate.withHeader("cheap", 10).withHeader("average", 30).withBody(new File("src/test/resources/average.json"))
                .to("direct:start").send();

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testExpensive() throws Exception {
        fluentTemplate = context.createFluentProducerTemplate();
        fluentTemplate.start();
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(0);
        mockAverage.expectedMessageCount(0);
        mockExpensive.expectedMessageCount(1);

        fluentTemplate.withHeader("cheap", 10).withHeader("average", 30).withBody(new File("src/test/resources/expensive.json"))
                .to("direct:start").send();

        MockEndpoint.assertIsSatisfied(context);
    }

    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .choice()
                            .when().jsonpath("store.book.price < ${header.cheap}")
                            .to("mock:cheap")
                            .when().jsonpath("store.book.price < ${header.average}")
                            .to("mock:average")
                            .otherwise()
                            .to("mock:expensive");
                }
            };
        }
    }
}
