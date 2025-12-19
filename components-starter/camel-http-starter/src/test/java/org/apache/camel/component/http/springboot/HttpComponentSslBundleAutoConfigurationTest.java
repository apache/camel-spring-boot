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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests SSL Bundle auto-configuration with hot-reload enabled.
 * <p>
 * Verifies:
 * <ul>
 *   <li>SSL bundles are applied to HTTP component</li>
 *   <li>Hot-reload recreates connection managers</li>
 *   <li>Update handlers are registered correctly</li>
 *   <li>Multiple sequential updates work without issues</li>
 * </ul>
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                HttpComponentSslBundleAutoConfigurationTest.class,
                HttpComponentSslBundleAutoConfigurationTest.TestConfiguration.class
        },
        properties = {
                "camel.component.http.ssl-bundle=test-bundle",
                "camel.component.http.ssl-bundle-hot-reload=true"
        }
)
class HttpComponentSslBundleAutoConfigurationTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    TestSslBundles sslBundles;

    @Test
    void sslBundleIsAppliedToHttpComponent() {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);

        // Then
        assertNotNull(http);
        assertNotNull(http.getSslContextParameters(), "SSL context parameters should be set");
    }

    @Test
    void hotReloadRecreatesConnectionManager() throws Exception {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);
        http.createEndpoint("https://localhost:8443/test");
        HttpClientConnectionManager initialManager = http.getClientConnectionManager();

        // When
        sslBundles.triggerUpdate();

        // Then
        HttpClientConnectionManager newManager = http.getClientConnectionManager();
        assertTrue(
                newManager == null || newManager != initialManager,
                "Connection manager should be cleared or recreated after SSL bundle update"
        );
    }

    @Test
    void updateHandlerIsRegistered() {
        // Then
        assertTrue(sslBundles.hasUpdateHandler("test-bundle"),
                "Update handler should be registered for hot-reload");
    }

    @Test
    void multipleUpdatesWork() throws Exception {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);

        // When/Then
        for (int i = 0; i < 3; i++) {
            sslBundles.triggerUpdate();
            assertNotNull(http.getSslContextParameters(),
                    "SSL context parameters should persist after update " + (i + 1));
        }
    }

    @Test
    void httpsComponentAlsoConfigured() {
        // Given
        HttpComponent https = camelContext.getComponent("https", HttpComponent.class);

        // Then
        assertNotNull(https);
        assertNotNull(https.getSslContextParameters(),
                "HTTPS component should also have SSL context parameters");
    }

    @Test
    void sslContextCanBeCreated() throws Exception {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);
        SSLContextParameters sslParams = http.getSslContextParameters();

        // When
        SSLContext sslContext = sslParams.createSSLContext(camelContext);

        // Then
        assertNotNull(sslContext, "SSLContext should be created successfully");
        assertNotNull(sslContext.getProtocol(), "SSLContext should have a protocol");
    }

    @Test
    void hotReloadHandlesConnectionManagerCloseException() throws Exception {
        // Given
        HttpComponent http = camelContext.getComponent("http", HttpComponent.class);
        HttpClientConnectionManager failingManager = mock(HttpClientConnectionManager.class);
        doThrow(new IOException("Simulated close failure")).when(failingManager).close();
        http.setClientConnectionManager(failingManager);

        // When - should not throw despite close() failing
        sslBundles.triggerUpdate();

        // Then - manager should be cleared and SSL params should be reapplied
        assertNull(http.getClientConnectionManager(),
                "Connection manager should be cleared even if close() throws exception");
        assertNotNull(http.getSslContextParameters(),
                "SSL context parameters should be reapplied after update");
    }

    @Test
    void hotReloadUpdatesHttpsComponent() throws Exception {
        // Given
        HttpComponent https = camelContext.getComponent("https", HttpComponent.class);
        https.createEndpoint("https://localhost:8443/test");
        HttpClientConnectionManager initialManager = https.getClientConnectionManager();

        // When
        sslBundles.triggerUpdate();

        // Then
        HttpClientConnectionManager newManager = https.getClientConnectionManager();
        assertTrue(
                newManager == null || newManager != initialManager,
                "HTTPS connection manager should be cleared or recreated after SSL bundle update"
        );
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        TestSslBundles sslBundles() {
            return new TestSslBundles();
        }
    }

    /**
     * Test implementation of SslBundles that allows triggering updates programmatically.
     */
    static class TestSslBundles implements SslBundles {
        private final List<Consumer<SslBundle>> updateHandlers = new ArrayList<>();
        private SslBundle currentBundle;

        TestSslBundles() {
            this.currentBundle = createMockBundle();
        }

        void triggerUpdate() {
            this.currentBundle = createMockBundle();
            for (Consumer<SslBundle> handler : updateHandlers) {
                handler.accept(currentBundle);
            }
        }

        boolean hasUpdateHandler(String name) {
            return !updateHandlers.isEmpty();
        }

        private SslBundle createMockBundle() {
            SslBundle bundle = mock(SslBundle.class);
            SslManagerBundle managers = mock(SslManagerBundle.class);
            when(managers.getKeyManagers()).thenReturn(null);
            when(managers.getTrustManagers()).thenReturn(null);
            when(bundle.getManagers()).thenReturn(managers);
            when(bundle.getProtocol()).thenReturn("TLS");
            return bundle;
        }

        @Override
        public SslBundle getBundle(String name) {
            if (!"test-bundle".equals(name)) {
                throw new IllegalArgumentException("Unknown bundle: " + name);
            }
            return currentBundle;
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
