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
package org.apache.camel.component.quartz.springboot;



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        QuartzStopRouteTest.class,
        QuartzStopRouteTest.TestConfiguration.class
    }
)
public class QuartzStopRouteTest extends BaseQuartzTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testQuartzSuspend() throws Exception {
        
        mock.expectedMinimumMessageCount(1);

        mock.assertIsSatisfied();

        context.getRouteController().stopRoute("foo");

        int size = mock.getReceivedCounter();
        assertEquals(1, size, "Should not schedule when stopped");

        mock.reset();

        mock.expectedMessageCount(0);
        mock.assertIsSatisfied(3000);

        mock.reset();
        mock.expectedMinimumMessageCount(1);

        context.getRouteController().startRoute("foo");

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
                    // START SNIPPET: e1
                    // triggers every second at precise 00,01,02,03..59
                    // notice we must use + as space when configured using URI parameter
                    from("quartz://myGroup/myTimerName?cron=0/1+*+*+*+*+?")
                            .routeId("foo")
                            .to("log:result", "mock:result");
                    // END SNIPPET: e1
                }
            };
        }
    }
    
   

}
