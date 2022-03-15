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
import org.apache.camel.language.xpath.XPathBuilder;
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
        XPathHeaderEnableSaxonJavaDslTest.class,
        XPathHeaderEnableSaxonJavaDslTest.TestConfiguration.class
    }
)
public class XPathHeaderEnableSaxonJavaDslTest {
    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:camel")
    protected MockEndpoint camel;   
    
    @EndpointInject("mock:donkey")
    protected MockEndpoint donkey;   
    
    @EndpointInject("mock:other")
    protected MockEndpoint other;   
    
    @Test
    public void testChoiceWithHeaderSelectCamel() throws Exception {
        
        camel.expectedBodiesReceived("<name>King</name>");
        camel.expectedHeaderReceived("type", "Camel");

        template.sendBodyAndHeader("direct:in", "<name>King</name>", "type", "Camel");

        camel.assertIsSatisfied();
    }

    @Test
    public void testChoiceWithNoHeaderSelectDonkey() throws Exception {
        
        donkey.expectedBodiesReceived("<name>Kong</name>");

        template.sendBody("direct:in", "<name>Kong</name>");

        donkey.assertIsSatisfied();
    }

    @Test
    public void testChoiceWithNoHeaderSelectOther() throws Exception {
        
        other.expectedBodiesReceived("<name>Other</name>");

        template.sendBody("direct:in", "<name>Other</name>");

        other.assertIsSatisfied();
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
                            .when(XPathBuilder.xpath("$type = 'Camel'").saxon())
                            .to("mock:camel")
                            .when(XPathBuilder.xpath("//name = 'Kong'").saxon())
                            .to("mock:donkey")
                            .otherwise()
                            .to("mock:other");
                }
            };
        }
    }
}
