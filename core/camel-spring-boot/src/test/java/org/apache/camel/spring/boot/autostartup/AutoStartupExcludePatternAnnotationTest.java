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
package org.apache.camel.spring.boot.autostartup;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.test.spring.junit5.AutoStartupExclude;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@CamelSpringBootTest
@SpringBootApplication
@AutoStartupExclude("myRoute,timer*")
@SpringBootTest(classes = AutoStartupExcludePatternAnnotationTest.class)
public class AutoStartupExcludePatternAnnotationTest {

    @Autowired
    FluentProducerTemplate producerTemplate;

    @Autowired
    CamelContext camelContext;

    @Configuration
    public static class Config extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("timer:tick?delay=1").to("mock:result");
            from("direct:start").to("seda:foo");
            from("seda:foo").id("myRoute").to("mock:result");
        }
    }

    @Test
    public void testRouteNotAutoStartedThenStarted() throws Exception {
        MockEndpoint mock = camelContext.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(0);
        mock.setAssertPeriod(500);

        producerTemplate.withBody("Hello World").to("direct:start").send();

        MockEndpoint.assertIsSatisfied(camelContext);

        mock.reset();
        mock.expectedMessageCount(1);

        camelContext.getRouteController().startRoute("myRoute");

        MockEndpoint.assertIsSatisfied(camelContext);
    }

}
