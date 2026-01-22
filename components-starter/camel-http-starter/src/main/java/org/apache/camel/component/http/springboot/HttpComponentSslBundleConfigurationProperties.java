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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Boot SSL Bundle integration with Camel HTTP component.
 * <p>
 * This allows applications to reference SSL bundles configured in Spring Boot's
 * {@code spring.ssl.bundle.*} configuration to secure HTTP(S) connections managed
 * by the Camel HTTP component.
 * <p>
 * Configuration properties are prefixed with {@code camel.component.http}.
 *
 * @see HttpComponentSslBundleAutoConfiguration
 * @see org.springframework.boot.ssl.SslBundles
 */
@ConfigurationProperties(prefix = "camel.component.http")
public class HttpComponentSslBundleConfigurationProperties {

    /**
     * The name of the Spring Boot SSL bundle to use for configuring SSL on the HTTP component.
     * When set, the SSL bundle's trust material and key material will be used to create the
     * SSLContext for HTTPS connections.
     */
    private String sslBundle;

    /**
     * Whether to enable hot-reload of SSL certificates when the SSL bundle is updated.
     * When enabled, certificate changes trigger recreation of the connection manager so new
     * connections use the updated certificates. Requires the SSL bundle to have
     * reload-on-update enabled in Spring Boot configuration.
     */
    private boolean sslBundleHotReload = false;

    public String getSslBundle() {
        return sslBundle;
    }

    public void setSslBundle(String sslBundle) {
        this.sslBundle = sslBundle;
    }

    public boolean isSslBundleHotReload() {
        return sslBundleHotReload;
    }

    public void setSslBundleHotReload(boolean sslBundleHotReload) {
        this.sslBundleHotReload = sslBundleHotReload;
    }
}
