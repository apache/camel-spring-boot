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

import java.security.SecureRandom;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        XQueryConcurrencyTest.class,
        XQueryConcurrencyTest.TestConfiguration.class
    }
)
public class XQueryConcurrencyTest {
    
    private String uri = "seda:in?concurrentConsumers=5";

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    protected MockEndpoint mock;   
    
    @Test
    public void testConcurrency() throws Exception {
        int total = 1000;

        mock.expectedMessageCount(total);

        // setup a task executor to be able send the messages in parallel
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.afterPropertiesSet();
        for (int i = 0; i < 5; i++) {
            final int threadCount = i;
            executor.execute(new Runnable() {
                public void run() {
                    int start = threadCount * 200;
                    for (int i = 0; i < 200; i++) {
                        try {
                            // do some random sleep to simulate spread in user activity
                            Thread.sleep(new SecureRandom().nextInt(10));
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        template.sendBody(uri, "<person><id>" + (start + i + 1) + "</id><name>James</name></person>");
                    }
                }
            });
        }

        mock.assertNoDuplicates(Builder.body());

        mock.assertIsSatisfied();
        executor.shutdown();
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
                    // no retry as we want every failure to submerge
                    errorHandler(noErrorHandler());

                    from(uri)
                            .transform().xquery("/person/id", String.class)
                            .to("mock:result");
                }
            };
        }
    }
}
