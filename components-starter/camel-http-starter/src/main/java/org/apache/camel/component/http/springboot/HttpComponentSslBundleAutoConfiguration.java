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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Auto-configuration for Spring Boot SSL Bundle support in Camel HTTP component.
 * <p>
 * This configuration bridges Spring Boot SSL Bundles to Camel's HTTP component,
 * enabling centralized SSL configuration and optional certificate hot-reload.
 * <p>
 * When hot-reload is enabled and certificates are rotated, the connection manager
 * is recreated so new connections use the updated certificates.
 * <p>
 * <b>Activation:</b> Enabled when property {@code camel.component.http.ssl-bundle} is configured.
 * <p>
 * <b>Hot-Reload:</b> Opt-in feature controlled by {@code camel.component.http.ssl-bundle-hot-reload}.
 *
 * @see HttpComponentSslBundleConfigurationProperties
 * @see org.springframework.boot.ssl.SslBundles
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SslBundles.class, HttpComponent.class})
@ConditionalOnBean({SslBundles.class, CamelContext.class})
@ConditionalOnProperty(prefix = "camel.component.http", name = "ssl-bundle")
@EnableConfigurationProperties(HttpComponentSslBundleConfigurationProperties.class)
@AutoConfigureAfter({CamelAutoConfiguration.class, HttpComponentAutoConfiguration.class})
public class HttpComponentSslBundleAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentSslBundleAutoConfiguration.class);

    private final SslBundles sslBundles;
    private final CamelContext camelContext;
    private final HttpComponentSslBundleConfigurationProperties configuration;

    public HttpComponentSslBundleAutoConfiguration(
            SslBundles sslBundles,
            CamelContext camelContext,
            HttpComponentSslBundleConfigurationProperties configuration) {
        this.sslBundles = sslBundles;
        this.camelContext = camelContext;
        this.configuration = configuration;
    }

    /**
     * Configures SSL bundle on HTTP components and optionally registers hot-reload handler.
     */
    @PostConstruct
    public void configureSslBundle() {
        String bundleName = configuration.getSslBundle();
        SslBundle bundle = sslBundles.getBundle(bundleName);

        configureComponent("http", bundleName, bundle);
        configureComponent("https", bundleName, bundle);

        if (configuration.isSslBundleHotReload()) {
            sslBundles.addBundleUpdateHandler(bundleName, updatedBundle -> onBundleUpdate(bundleName, updatedBundle));
            LOG.info("SSL bundle hot-reload enabled for bundle: {}", bundleName);
        }
    }

    /**
     * Applies SSL bundle configuration to a named HTTP component.
     *
     * @param name       the component name (http or https)
     * @param bundleName the SSL bundle name for logging
     * @param bundle     the SSL bundle to apply
     */
    private void configureComponent(String name, String bundleName, SslBundle bundle) {
        HttpComponent component = camelContext.getComponent(name, HttpComponent.class);
        if (component != null) {
            LOG.info("Configuring {} component with SSL bundle: {}", name, bundleName);
            applySslBundle(component, bundle);
        }
    }

    /**
     * Handles SSL bundle update notifications by recreating connection managers.
     * This ensures new connections use the updated certificates.
     *
     * @param bundleName    the name of the updated bundle
     * @param updatedBundle the newly updated SSL bundle
     */
    private void onBundleUpdate(String bundleName, SslBundle updatedBundle) {
        LOG.info("SSL bundle '{}' updated, recreating connection managers", bundleName);

        for (String name : new String[]{"http", "https"}) {
            HttpComponent component = camelContext.getComponent(name, HttpComponent.class);
            if (component == null) {
                continue;
            }

            HttpClientConnectionManager mgr = component.getClientConnectionManager();
            if (mgr != null) {
                try {
                    mgr.close();
                } catch (Exception e) {
                    LOG.debug("Error closing connection manager for {}: {}", name, e.getMessage());
                }
            }
            component.setClientConnectionManager(null);
            applySslBundle(component, updatedBundle);
        }
    }

    /**
     * Applies SSL bundle to an HTTP component by creating SSLContextParameters
     * from the bundle's key and trust managers.
     *
     * @param component the HTTP component to configure
     * @param bundle    the SSL bundle containing key and trust managers
     */
    private void applySslBundle(HttpComponent component, SslBundle bundle) {
        component.setSslContextParameters(new SSLContextParameters() {
            @Override
            public SSLContext createSSLContext(CamelContext ctx) throws GeneralSecurityException {
                SSLContext sslContext = SSLContext.getInstance(bundle.getProtocol());
                sslContext.init(
                    bundle.getManagers().getKeyManagers(),
                    bundle.getManagers().getTrustManagers(),
                    new SecureRandom());
                return sslContext;
            }
        });
    }
}
