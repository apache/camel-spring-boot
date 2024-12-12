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
package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.concurrent.Executor;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpMultipleExecutorsVirtualThreadsTest.class,
        SpringBootPlatformHttpMultipleExecutorsVirtualThreadsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class },
    properties = "spring.threads.virtual.enabled=true")
@EnableScheduling
public class SpringBootPlatformHttpMultipleExecutorsVirtualThreadsTest extends PlatformHttpBase {

    private static final String THREAD_PREFIX = "myThread-";

    private static final String postRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_mypost";

    private static final String getRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_myget";

    // *************************************
    // Config
    // *************************************
    @Configuration
    public static class TestConfiguration {

        @Bean
        public SimpleAsyncTaskExecutor simpleAsyncTaskExecutor(SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder) {
            return simpleAsyncTaskExecutorBuilder
                .threadNamePrefix(THREAD_PREFIX)
                .build();
        }

        @Bean
        public RouteBuilder servletPlatformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/myget").id(postRouteId).setBody().constant("get");
                    from("platform-http:/mypost").id(getRouteId).transform().body(String.class, b -> b.toUpperCase());

                    from("platform-http:/executor").process(exchange -> exchange.getIn().setBody(Thread.currentThread().getName()));
                }
            };
        }
    }

    @Override
    protected String getPostRouteId() {
        return postRouteId;
    }

    @Override
    protected String getGetRouteId() {
        return getRouteId;
    }

    @Autowired
    List<Executor> executors;

    @Test
    public void checkCustomExecutorIsPickedWhenMultipleExecutorsAreDefined() {
        Assertions.assertThat(executors).hasSizeGreaterThan(1);

        Assertions.assertThat(restTemplate.postForEntity("/executor", "test", String.class).getBody())
                        .contains(THREAD_PREFIX);
    }
}
