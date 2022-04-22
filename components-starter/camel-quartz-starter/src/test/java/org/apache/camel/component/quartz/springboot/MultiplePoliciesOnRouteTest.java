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



import java.util.Date;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.routepolicy.quartz.SimpleScheduledRoutePolicy;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.throttling.ThrottlingInflightRoutePolicy;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MultiplePoliciesOnRouteTest.class
    }
)
public class MultiplePoliciesOnRouteTest extends FromFileBase {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
       
    private String url = "seda:foo?concurrentConsumers=20";
    private int size = 100;
    
    @Bean("startPolicy")
    private RoutePolicy createRouteStartPolicy() {
        SimpleScheduledRoutePolicy policy = new SimpleScheduledRoutePolicy();
        long startTime = System.currentTimeMillis() + 3000L;
        policy.setRouteStartDate(new Date(startTime));
        policy.setRouteStartRepeatCount(1);
        policy.setRouteStartRepeatInterval(3000);

        return policy;
    }

    @Bean("throttlePolicy")
    private RoutePolicy createThrottlePolicy() {
        ThrottlingInflightRoutePolicy policy = new ThrottlingInflightRoutePolicy();
        policy.setMaxInflightExchanges(10);
        return policy;
    }
    
    @Test
    public void testMultiplePoliciesOnRoute() throws Exception {
        MockEndpoint success = context.getEndpoint("mock:success", MockEndpoint.class);
        success.expectedMinimumMessageCount(size - 10);

        context.getComponent("quartz", QuartzComponent.class)
                .setPropertiesFile("org/apache/camel/routepolicy/quartz/myquartz.properties");
        context.addRoutes(new RouteBuilder() {
            public void configure() {
                from(url)
                        .routeId("test")
                        .routePolicyRef("startPolicy, throttlePolicy")
                        .to("log:foo?groupSize=10")
                        .to("mock:success");
            }
        });
        context.start();

        assertSame(ServiceStatus.Started, context.getRouteController().getRouteStatus("test"));
        for (int i = 0; i < size; i++) {
            template.sendBody(url, "Message " + i);
            Thread.sleep(3);
        }

        context.getComponent("quartz", QuartzComponent.class).stop();
        success.assertIsSatisfied();
    }

    
   

}
