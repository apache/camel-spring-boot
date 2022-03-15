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
        XQueryHeaderNameTest.class,
        XQueryHeaderNameTest.TestConfiguration.class
    }
)
public class XQueryHeaderNameTest {
    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:premium")
    protected MockEndpoint premium;   
    
    @EndpointInject("mock:unknown")
    protected MockEndpoint unknown; 
    
    @EndpointInject("mock:standard")
    protected MockEndpoint standard; 
    
    @Test
    public void testChoiceWithHeaderNamePremium() throws Exception {
        
        premium.expectedBodiesReceived("<response>OK</response>");
        premium.expectedHeaderReceived("invoiceDetails",
                "<invoice orderType='premium'><person><name>Alan</name></person></invoice>");

        template.sendBodyAndHeader("direct:in", "<response>OK</response>",
                "invoiceDetails", "<invoice orderType='premium'><person><name>Alan</name></person></invoice>");

        premium.assertIsSatisfied();
    }

    @Test
    public void testChoiceWithHeaderNameStandard() throws Exception {
        
        standard.expectedBodiesReceived("<response>OK</response>");
        standard.expectedHeaderReceived("invoiceDetails",
                "<invoice orderType='standard'><person><name>Alan</name></person></invoice>");

        template.sendBodyAndHeader("direct:in", "<response>OK</response>",
                "invoiceDetails", "<invoice orderType='standard'><person><name>Alan</name></person></invoice>");

        standard.assertIsSatisfied();
    }

    @Test
    public void testChoiceWithHeaderNameUnknown() throws Exception {
        
        unknown.expectedBodiesReceived("<response>OK</response>");
        unknown.expectedHeaderReceived("invoiceDetails", "<invoice />");

        template.sendBodyAndHeader("direct:in", "<response>OK</response>",
                "invoiceDetails", "<invoice />");

        unknown.assertIsSatisfied();
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
                    from("direct:in")
                            .choice()
                            .when().xquery("/invoice/@orderType = 'premium'", "invoiceDetails")
                            .to("mock:premium")
                            .when().xquery("/invoice/@orderType = 'standard'", "invoiceDetails")
                            .to("mock:standard")
                            .otherwise()
                            .to("mock:unknown")
                            .end();
                }
            };
        }
    }
}
