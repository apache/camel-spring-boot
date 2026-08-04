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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CamelRequestHandlerMapping} registers itself as a platform-http listener and is only notified of endpoints
 * created after it exists. It must therefore be instantiated eagerly: creating it on first demand is unordered with
 * respect to CamelContext startup, and when Camel starts first the mapping never learns about the endpoints that
 * already exist, so every platform-http route is answered with 404 while reporting itself as started.
 * <p>
 * That the context starts at all also covers the circular dependency the ObjectProvider avoids.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpEagerMappingTest.class,
        SpringBootPlatformHttpEagerMappingTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpEagerMappingTest {

    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Autowired
    CamelRequestHandlerMapping mapping;

    @Test
    void mappingMustNotBeLazy() {
        Assertions.assertThat(applicationContext.getBeanFactory()
                .getBeanDefinition("platformHttpEngineRequestMapping").isLazyInit())
                .isFalse();
    }

    @Test
    void endpointIsMapped() {
        assertThat(mapping.getHandlerMethods().keySet().stream()
                .map(RequestMappingInfo::getPathPatternsCondition)
                .anyMatch(condition -> condition != null && condition.getPatternValues().contains("/eager")))
                .isTrue();
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/eager").routeId("eager-route")
                            .setBody().constant("alive");
                }
            };
        }
    }
}
