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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.SynchronizationAdapter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.CamelContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, KameletConsumerUoWIssueTest.class, })

public class KameletConsumerUoWIssueTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:foo")
    MockEndpoint mock;

    @Autowired
    private CamelContext context;

    @Test
    public void testUoW() throws Exception {
        mock.expectedBodiesReceived("1", "Done");

        context.getRouteController().startAllRoutes();

        mock.assertIsSatisfied();
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
                routeTemplate("tick").from("timer:tick?repeatCount=1&delay=-1&includeMetadata=true").setBody()
                        .exchangeProperty(Exchange.TIMER_COUNTER).process(new Processor() {
                            @Override
                            public void process(Exchange exchange) {
                                exchange.getExchangeExtension().addOnCompletion(new SynchronizationAdapter() {
                                    @Override
                                    public void onDone(Exchange exchange) {
                                        super.onDone(exchange);
                                        template.sendBody("mock:foo", "Done");
                                    }
                                });
                            }
                        }).to("kamelet:sink");

                from("kamelet:tick").autoStartup(false).routeId("tick").to("mock:foo");
            }
        };
    }
}
