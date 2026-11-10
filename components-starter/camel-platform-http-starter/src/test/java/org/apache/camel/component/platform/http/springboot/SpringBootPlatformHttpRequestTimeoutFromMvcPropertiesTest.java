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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * CAMEL-26782 regression test: verifies that when the request timeout is set
 * programmatically via {@code spring.mvc.async.request-timeout} (which
 * {@link SpringBootPlatformWebMvcConfiguration#configureAsyncSupport} picks up
 * and writes onto the configuration bean), the value survives
 * {@code CamelPropertiesHelper.copyConfigurationProperties()} and appears on
 * the live {@link PlatformHttpComponent}.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {
                CamelAutoConfiguration.class,
                PlatformHttpComponentAutoConfiguration.class,
                SpringBootPlatformHttpAutoConfiguration.class,
                SpringBootPlatformWebMvcConfiguration.class,
                SpringBootPlatformHttpRequestTimeoutFromMvcPropertiesTest.TestConfiguration.class
        },
        properties = {
                "spring.mvc.async.request-timeout=5000ms"
        })
public class SpringBootPlatformHttpRequestTimeoutFromMvcPropertiesTest {

    @Autowired
    CamelContext camelContext;

    @Test
    public void requestTimeoutFromMvcPropertiesIsCopiedToComponent() {
        PlatformHttpComponent component = camelContext.getComponent("platform-http", PlatformHttpComponent.class);
        assertEquals(5000L, component.getRequestTimeout(),
                "request-timeout set via spring.mvc.async.request-timeout must be propagated to PlatformHttpComponent");
    }

    @Configuration
    public static class TestConfiguration {
        // No routes needed — we only verify the configuration propagation
    }
}
