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
package org.apache.camel.spring.boot.stub;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.StubEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@CamelSpringBootTest
@StubEndpoints("kafka,amqp")
@SpringBootApplication
@SpringBootTest(classes = StubEndpointsTest.class)
public class StubEndpointsTest {

    @Autowired
    FluentProducerTemplate producerTemplate;

    @Autowired
    CamelContext camelContext;

    @Configuration
    public static class Config extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("direct:start").to("amqp:cheese");
            from("amqp:cheese").to("mock:middle").transform(simple("Bye ${body}")).to("kafka:beer");
            from("kafka:beer").to("mock:result");
        }
    }

    @Test
    public void shouldStubEndpoints() throws Exception {
        camelContext.getEndpoint("mock:middle", MockEndpoint.class).expectedBodiesReceived("World");
        camelContext.getEndpoint("mock:result", MockEndpoint.class).expectedBodiesReceived("Bye World");

        producerTemplate.withBody("World").to("direct:start").send();

        MockEndpoint.assertIsSatisfied(camelContext);
    }

}
