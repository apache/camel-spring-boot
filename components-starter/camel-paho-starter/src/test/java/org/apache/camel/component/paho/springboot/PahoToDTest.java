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
package org.apache.camel.component.paho.springboot;



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.paho.PahoComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.infra.activemq.services.ActiveMQEmbeddedService;
import org.apache.camel.test.infra.activemq.services.ActiveMQEmbeddedServiceBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        PahoToDTest.class,
        PahoToDTest.TestConfiguration.class
    }
)
public class PahoToDTest {
    
    static int mqttPort = AvailablePortFinder.getNextAvailable();

    @RegisterExtension
    public ActiveMQEmbeddedService service = ActiveMQEmbeddedServiceBuilder
            .bare()
            .withPersistent(false)
            .withMqttTransport(mqttPort)
            .build();


    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:bar")
    MockEndpoint mockBar;
    
    @EndpointInject("mock:beer")
    MockEndpoint mockBeer;
    
    @Test
    public void testToD() throws Exception {
        mockBar.expectedBodiesReceived("Hello bar");
        mockBeer.expectedBodiesReceived("Hello beer");

        template.sendBodyAndHeader("direct:start", "Hello bar", "where", "bar");
        template.sendBodyAndHeader("direct:start", "Hello beer", "where", "beer");

        mockBar.assertIsSatisfied();
        mockBeer.assertIsSatisfied();
    }

    @AfterAll
    public void cleanUp() {
        service.shutdown();
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
                    PahoComponent paho = context.getComponent("paho", PahoComponent.class);
                    paho.getConfiguration().setBrokerUrl("tcp://localhost:" + mqttPort);

                    // route message dynamic using toD
                    from("direct:start").toD("paho:${header.where}");

                    from("paho:bar").to("mock:bar");
                    from("paho:beer").to("mock:beer");
                }
            };
        }
    }
    
   

}
