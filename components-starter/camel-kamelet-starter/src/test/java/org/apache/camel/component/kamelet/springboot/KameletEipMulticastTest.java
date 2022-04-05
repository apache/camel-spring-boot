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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.RouteDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;

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
        KameletEipMulticastTest.class,
    }
)

public class KameletEipMulticastTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;

    @EndpointInject("mock:resultTwo")
    MockEndpoint mockTwo;

    @Autowired
    CamelContext context;

    @Bean
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testOne() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                routeTemplate("echo")
                        .from("kamelet:source")
                        .setBody(body().append(body()));

                routeTemplate("reverse")
                        .from("kamelet:source")
                        .setBody(this::reverse);

                from("direct:start").routeId("start")
                    .multicast()
                        .kamelet("echo")
                        .kamelet("reverse") // this becomes output on previous kamelet
                    .end()
                    .to("mock:result");
            }

            private Object reverse(Exchange exchange) {
                StringBuilder sb = new StringBuilder(exchange.getMessage().getBody(String.class));
                sb.reverse();
                return sb.toString();
            }
        });
        context.start();

        mock.expectedBodiesReceived("CBACBA");

        template.sendBody("direct:start", "ABC");

        mock.assertIsSatisfied();

        RouteDefinition rd = ((ModelCamelContext) context).getRouteDefinition("start");
        MulticastDefinition md = ProcessorDefinitionHelper.findFirstTypeInOutputs(rd.getOutputs(), MulticastDefinition.class);
        Assertions.assertEquals(1, md.getOutputs().size());
    }

    @Test
    public void testTwo() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                routeTemplate("echo")
                        .from("kamelet:source")
                        .setBody(body().append(body()));

                routeTemplate("reverse")
                        .from("kamelet:source")
                        .setBody(this::reverse);

                from("direct:start").routeId("start")
                    .multicast()
                        .kamelet("echo").end()
                        .kamelet("reverse").end()
                        .end()
                        .to("mock:resultTwo");
            }

            private Object reverse(Exchange exchange) {
                StringBuilder sb = new StringBuilder(exchange.getMessage().getBody(String.class));
                sb.reverse();
                return sb.toString();
            }
        });
        context.start();

        mockTwo.expectedBodiesReceived("CBA", "FED");

        template.sendBody("direct:start", "ABC");
        template.sendBody("direct:start", "DEF");

        mockTwo.assertIsSatisfied();

        RouteDefinition rd = ((ModelCamelContext) context).getRouteDefinition("start");
        MulticastDefinition md = ProcessorDefinitionHelper.findFirstTypeInOutputs(rd.getOutputs(), MulticastDefinition.class);
        Assertions.assertEquals(2, md.getOutputs().size());
    }

}