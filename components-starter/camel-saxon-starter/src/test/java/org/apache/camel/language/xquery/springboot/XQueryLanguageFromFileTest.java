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
package org.apache.camel.language.xquery.springboot;



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        XQueryLanguageFromFileTest.class,
        XQueryLanguageFromFileTest.TestConfiguration.class
    }
)
public class XQueryLanguageFromFileTest extends FromFileBase {

    @Autowired
    private CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:davsclaus")
    MockEndpoint mock;

    @EndpointInject("mock:other")
    MockEndpoint other;
    
    @Test
    public void testXQueryFromFile() throws Exception {
        
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).contains("Hello World");

        
        other.expectedMessageCount(1);
        other.message(0).body(String.class).contains("Bye World");

        template.sendBodyAndHeader(fileUri(),
                "<mail from=\"davsclaus@apache.org\"><subject>Hey</subject><body>Hello World!</body></mail>",
                Exchange.FILE_NAME, "claus.xml");

        template.sendBodyAndHeader(fileUri(),
                "<mail from=\"janstey@apache.org\"><subject>Hey</subject><body>Bye World!</body></mail>",
                Exchange.FILE_NAME, "janstey.xml");

        MockEndpoint.assertIsSatisfied(context);
    }

    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(fileUri())
                            .choice()
                            .when().xquery("/mail/@from = 'davsclaus@apache.org'")
                            .convertBodyTo(String.class)
                            .to("mock:davsclaus")
                            .otherwise()
                            .convertBodyTo(String.class)
                            .to("mock:other");
                }
            };
        }
    }
}
