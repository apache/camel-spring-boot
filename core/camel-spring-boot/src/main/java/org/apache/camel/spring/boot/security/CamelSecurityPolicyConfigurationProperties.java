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
package org.apache.camel.spring.boot.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security policy configuration for Camel Spring Boot applications.
 * <p>
 * Controls how Camel reacts to insecure configuration at startup. Policies can be set globally or per security
 * category:
 * <ul>
 * <li>{@code allow} — no warnings, allow the configuration</li>
 * <li>{@code warn} — log a warning at startup (default)</li>
 * <li>{@code fail} — throw an exception and prevent startup</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "camel.security")
public class CamelSecurityPolicyConfigurationProperties {

    /**
     * Global security policy applied to all categories unless overridden. Controls how Camel reacts when insecure
     * configuration is detected at startup.
     */
    private String policy = "warn";

    /**
     * Security policy for plain-text secrets. When set, overrides the global policy for properties that contain
     * sensitive values configured as plain text.
     */
    private String secretPolicy;

    /**
     * Security policy for insecure SSL/TLS configuration. When set, overrides the global policy for options that
     * disable certificate validation or hostname verification.
     */
    private String insecureSslPolicy;

    /**
     * Security policy for insecure deserialization configuration. When set, overrides the global policy for options that
     * enable dangerous deserialization of untrusted data.
     */
    private String insecureSerializationPolicy;

    /**
     * Security policy for development-only features. When set, overrides the global policy for options intended only for
     * development environments.
     */
    private String insecureDevPolicy;

    /**
     * Comma-separated list of property keys to exclude from security policy checks. Use full property paths (e.g.,
     * camel.component.aws2-s3.trustAllCertificates) to allow specific properties regardless of the configured policy.
     */
    private String allowedProperties;

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getSecretPolicy() {
        return secretPolicy;
    }

    public void setSecretPolicy(String secretPolicy) {
        this.secretPolicy = secretPolicy;
    }

    public String getInsecureSslPolicy() {
        return insecureSslPolicy;
    }

    public void setInsecureSslPolicy(String insecureSslPolicy) {
        this.insecureSslPolicy = insecureSslPolicy;
    }

    public String getInsecureSerializationPolicy() {
        return insecureSerializationPolicy;
    }

    public void setInsecureSerializationPolicy(String insecureSerializationPolicy) {
        this.insecureSerializationPolicy = insecureSerializationPolicy;
    }

    public String getInsecureDevPolicy() {
        return insecureDevPolicy;
    }

    public void setInsecureDevPolicy(String insecureDevPolicy) {
        this.insecureDevPolicy = insecureDevPolicy;
    }

    public String getAllowedProperties() {
        return allowedProperties;
    }

    public void setAllowedProperties(String allowedProperties) {
        this.allowedProperties = allowedProperties;
    }

}
