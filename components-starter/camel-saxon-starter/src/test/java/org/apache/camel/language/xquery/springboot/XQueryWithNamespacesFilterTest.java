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
        XQueryWithNamespacesFilterTest.class,
        XQueryWithNamespacesFilterTest.TestConfiguration.class
    }
)
public class XQueryWithNamespacesFilterTest {
    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    protected MockEndpoint mock;   
    
    @Test
    public void testSendMatchingMessage() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        template.sendBody("direct:start", "<person xmlns='http://acme.com/cheese' name='James' city='London'/>");

        mock.assertIsSatisfied();
    }

    @Test
    public void testSendNotMatchingMessage() throws Exception {
        mock.reset();
        mock.expectedMessageCount(0);

        template.sendBody("direct:start", "<person xmlns='http://acme.com/cheese'  name='Hiram' city='Tampa'/>");

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
                public void configure() {
                    // START SNIPPET: example
                    Namespaces ns = new Namespaces("c", "http://acme.com/cheese");

                    from("direct:start").filter().xquery("/c:person[@name='James']", ns).to("mock:result");
                    // END SNIPPET: example
                }
            };
        }
    }
}
