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



import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.builder.Namespaces;
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
        XQueryHeaderNameResultTypeAndNamespaceTest.class,
        XQueryHeaderNameResultTypeAndNamespaceTest.TestConfiguration.class
    }
)
public class XQueryHeaderNameResultTypeAndNamespaceTest {
    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:55")
    protected MockEndpoint mock;   
    
    @Test
    public void testXPathWithNamespace() throws Exception {
        
        mock.expectedBodiesReceived("body");
        mock.expectedHeaderReceived("cheeseDetails", "<number xmlns=\"http://acme.com/cheese\">55</number>");

        template.sendBodyAndHeader("direct:in", "body", "cheeseDetails",
                "<number xmlns=\"http://acme.com/cheese\">55</number>");

        mock.assertIsSatisfied();
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
                    Namespaces ns = new Namespaces("c", "http://acme.com/cheese");

                    from("direct:in").choice()
                            .when().xquery("/c:number = 55", Integer.class, ns, "cheeseDetails")
                            .to("mock:55")
                            .otherwise()
                            .to("mock:other")
                            .end();
                }
            };
        }
    }
}
