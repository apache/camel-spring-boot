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
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpMultipleExecutorsTest.class, SpringBootPlatformHttpMultipleExecutorsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
@EnableScheduling
@AutoConfigureRestTestClient
public class SpringBootPlatformHttpMultipleExecutorsTest extends PlatformHttpBase {

    private static final String postRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_mypost";

    private static final String getRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_myget";

    private static final String THREAD_PREFIX = "myThread-";

    // *************************************
    // Config
    // *************************************
    @Configuration
    public static class TestConfiguration {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }


        @Bean(name = "customPoolTaskExecutor")
        public Executor customPoolTaskExecutor() {
            final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(2);
            executor.setQueueCapacity(500);
            executor.setThreadNamePrefix(THREAD_PREFIX);
            executor.initialize();
            return executor;
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

        EntityExchangeResult<String> result = restTestClient.post().uri("/executor")
                .body("test")
                .exchange()
                .expectBody(String.class)
                .returnResult();
        Assertions.assertThat(result.getResponseBody()).contains(THREAD_PREFIX);
    }
}
