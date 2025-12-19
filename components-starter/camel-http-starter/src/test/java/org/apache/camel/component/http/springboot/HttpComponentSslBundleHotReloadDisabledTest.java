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
package org.apache.camel.component.http.springboot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests SSL Bundle auto-configuration with hot-reload disabled (default behavior).
 * <p>
 * Verifies:
 * <ul>
 *   <li>SSL bundles are applied even when hot-reload is disabled</li>
 *   <li>No update handler is registered (opt-in behavior)</li>
 * </ul>
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                HttpComponentSslBundleHotReloadDisabledTest.class,
                HttpComponentSslBundleHotReloadDisabledTest.TestConfiguration.class
        },
        properties = {
                "camel.component.http.ssl-bundle=test-bundle",
                "camel.component.http.ssl-bundle-hot-reload=false"
        }
)
class HttpComponentSslBundleHotReloadDisabledTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    TestSslBundles sslBundles;

    @Test
    void sslBundleIsAppliedWhenHotReloadDisabled() {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);

        // Then
        assertNotNull(http);
        assertNotNull(http.getSslContextParameters(), "SSL context parameters should be set");
    }

    @Test
    void updateHandlerNotRegisteredWhenHotReloadDisabled() {
        // Then
        assertFalse(sslBundles.hasUpdateHandler("test-bundle"),
                "Update handler should NOT be registered when hot-reload is disabled");
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        TestSslBundles sslBundles() {
            return new TestSslBundles();
        }
    }

    /**
     * Test implementation of SslBundles for verifying handler registration.
     */
    static class TestSslBundles implements SslBundles {
        private final List<Consumer<SslBundle>> updateHandlers = new ArrayList<>();
        private final SslBundle bundle;

        TestSslBundles() {
            SslManagerBundle managers = mock(SslManagerBundle.class);
            when(managers.getKeyManagers()).thenReturn(null);
            when(managers.getTrustManagers()).thenReturn(null);
            this.bundle = mock(SslBundle.class);
            when(bundle.getManagers()).thenReturn(managers);
            when(bundle.getProtocol()).thenReturn("TLS");
        }

        boolean hasUpdateHandler(String name) {
            return !updateHandlers.isEmpty();
        }

        @Override
        public SslBundle getBundle(String name) {
            if (!"test-bundle".equals(name)) {
                throw new IllegalArgumentException("Unknown bundle: " + name);
            }
            return bundle;
        }

        @Override
        public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) {
            if ("test-bundle".equals(name)) {
                updateHandlers.add(updateHandler);
            }
        }

        @Override
        public void addBundleRegisterHandler(java.util.function.BiConsumer<String, SslBundle> registerHandler) {
        }

        @Override
        public java.util.List<String> getBundleNames() {
            return java.util.List.of("test-bundle");
        }
    }
}
