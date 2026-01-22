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
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Edge case and negative scenario tests for SSL Bundle auto-configuration.
 * <p>
 * Uses ApplicationContextRunner for lightweight, fast tests that verify:
 * <ul>
 *   <li>Auto-configuration activation conditions</li>
 *   <li>Exception handling for invalid bundles</li>
 *   <li>Property binding and defaults</li>
 * </ul>
 */
class HttpComponentSslBundleEdgeCasesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CamelAutoConfiguration.class,
                    HttpComponentAutoConfiguration.class,
                    HttpComponentSslBundleAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWithoutSslBundleProperty() {
        // Given: No ssl-bundle property set
        contextRunner
                .withBean(SslBundles.class, this::createMockSslBundles)
                .run(context -> {
                    // Then: Auto-configuration should not be loaded due to @ConditionalOnProperty
                    assertThat(context).doesNotHaveBean(HttpComponentSslBundleAutoConfiguration.class);
                });
    }

    @Test
    void autoConfigurationActivatedWithSslBundleProperty() {
        // Given: ssl-bundle property is set
        contextRunner
                .withPropertyValues("camel.component.http.ssl-bundle=test-bundle")
                .withBean(SslBundles.class, this::createMockSslBundles)
                .run(context -> {
                    // Then: Auto-configuration should be loaded
                    assertThat(context).hasSingleBean(HttpComponentSslBundleAutoConfiguration.class);
                    assertThat(context).hasSingleBean(HttpComponentSslBundleConfigurationProperties.class);
                });
    }

    @Test
    void configurationPropertiesBindCorrectly() {
        // Given: Both properties set
        contextRunner
                .withPropertyValues(
                        "camel.component.http.ssl-bundle=my-bundle",
                        "camel.component.http.ssl-bundle-hot-reload=true")
                .withBean(SslBundles.class, () -> createMockSslBundlesForName("my-bundle"))
                .run(context -> {
                    // Then: Properties should be bound correctly
                    HttpComponentSslBundleConfigurationProperties props =
                            context.getBean(HttpComponentSslBundleConfigurationProperties.class);
                    assertThat(props.getSslBundle()).isEqualTo("my-bundle");
                    assertThat(props.isSslBundleHotReload()).isTrue();
                });
    }

    @Test
    void hotReloadDefaultsToFalse() {
        // Given: Only ssl-bundle property set (no hot-reload)
        contextRunner
                .withPropertyValues("camel.component.http.ssl-bundle=test-bundle")
                .withBean(SslBundles.class, this::createMockSslBundles)
                .run(context -> {
                    // Then: Hot-reload should default to false
                    HttpComponentSslBundleConfigurationProperties props =
                            context.getBean(HttpComponentSslBundleConfigurationProperties.class);
                    assertThat(props.isSslBundleHotReload()).isFalse();
                });
    }

    @Test
    void invalidBundleNameThrowsException() {
        // Given: ssl-bundle property set to non-existent bundle
        contextRunner
                .withPropertyValues("camel.component.http.ssl-bundle=non-existent-bundle")
                .withBean(SslBundles.class, () -> {
                    SslBundles bundles = mock(SslBundles.class);
                    when(bundles.getBundle("non-existent-bundle"))
                            .thenThrow(new NoSuchSslBundleException("non-existent-bundle", "SSL bundle 'non-existent-bundle' not found"));
                    return bundles;
                })
                .run(context -> {
                    // Then: Context should fail to start with NoSuchSslBundleException
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(NoSuchSslBundleException.class)
                            .hasMessageContaining("non-existent-bundle");
                });
    }

    @Test
    void sslContextCreationWithNullManagers() throws GeneralSecurityException {
        // Given: SslBundle with null key/trust managers (common for trust-only scenarios)
        SslBundle bundle = mock(SslBundle.class);
        SslManagerBundle managers = mock(SslManagerBundle.class);
        when(managers.getKeyManagers()).thenReturn(null);
        when(managers.getTrustManagers()).thenReturn(null);
        when(bundle.getManagers()).thenReturn(managers);
        when(bundle.getProtocol()).thenReturn("TLSv1.3");

        // When: Creating SSLContext with null managers
        SSLContext sslContext = SSLContext.getInstance(bundle.getProtocol());
        sslContext.init(
                bundle.getManagers().getKeyManagers(),
                bundle.getManagers().getTrustManagers(),
                new java.security.SecureRandom());

        // Then: SSLContext should be created successfully
        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("TLSv1.3");
    }

    @Test
    void sslContextCreationWithDifferentProtocols() throws GeneralSecurityException {
        // Test various SSL/TLS protocol versions
        for (String protocol : new String[]{"TLS", "TLSv1.2", "TLSv1.3"}) {
            // Given
            SslBundle bundle = mock(SslBundle.class);
            SslManagerBundle managers = mock(SslManagerBundle.class);
            when(managers.getKeyManagers()).thenReturn(null);
            when(managers.getTrustManagers()).thenReturn(null);
            when(bundle.getManagers()).thenReturn(managers);
            when(bundle.getProtocol()).thenReturn(protocol);

            // When
            SSLContext sslContext = SSLContext.getInstance(bundle.getProtocol());
            sslContext.init(null, null, new java.security.SecureRandom());

            // Then
            assertThat(sslContext.getProtocol()).isEqualTo(protocol);
        }
    }

    private SslBundles createMockSslBundles() {
        return createMockSslBundlesForName("test-bundle");
    }

    private SslBundles createMockSslBundlesForName(String bundleName) {
        SslBundles bundles = mock(SslBundles.class);
        SslBundle bundle = mock(SslBundle.class);
        SslManagerBundle managers = mock(SslManagerBundle.class);

        when(managers.getKeyManagers()).thenReturn(null);
        when(managers.getTrustManagers()).thenReturn(null);
        when(bundle.getManagers()).thenReturn(managers);
        when(bundle.getProtocol()).thenReturn("TLS");
        when(bundles.getBundle(bundleName)).thenReturn(bundle);

        return bundles;
    }
}
