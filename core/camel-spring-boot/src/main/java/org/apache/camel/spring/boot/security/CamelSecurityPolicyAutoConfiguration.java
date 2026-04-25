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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.main.SecurityConfigurationProperties;
import org.apache.camel.main.SecurityPolicyResult;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.SecurityUtils;
import org.apache.camel.util.SecurityViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = CamelAutoConfiguration.class)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelSecurityPolicyConfigurationProperties.class)
public class CamelSecurityPolicyAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelSecurityPolicyAutoConfiguration.class);

    @Bean
    SecurityPolicyResult camelSecurityPolicyResult(
            CamelContext camelContext,
            CamelSecurityPolicyConfigurationProperties config,
            Environment environment) {

        SecurityConfigurationProperties securityConfig = applySecurityProperties(camelContext, config);

        Map<String, Object> camelProperties = extractCamelProperties(environment);

        List<SecurityViolation> violations = SecurityUtils.detectViolations(
                camelProperties,
                (k, v) -> containsSensitive(camelContext, k, v),
                securityConfig::resolvePolicy,
                securityConfig.getAllowedPropertySet());

        SecurityPolicyResult result = new SecurityPolicyResult(violations);
        camelContext.getCamelContextExtension().addContextPlugin(SecurityPolicyResult.class, result);

        enforceViolations(violations);

        return result;
    }

    private SecurityConfigurationProperties applySecurityProperties(
            CamelContext camelContext,
            CamelSecurityPolicyConfigurationProperties config) {

        // get the security config from camel-main's MainConfigurationProperties
        // which is already bound by CamelAutoConfiguration via CamelConfigurationProperties
        SecurityConfigurationProperties securityConfig
                = new SecurityConfigurationProperties(null);

        securityConfig.setPolicy(config.getPolicy());
        if (config.getSecretPolicy() != null) {
            securityConfig.setSecretPolicy(config.getSecretPolicy());
        }
        if (config.getInsecureSslPolicy() != null) {
            securityConfig.setInsecureSslPolicy(config.getInsecureSslPolicy());
        }
        if (config.getInsecureSerializationPolicy() != null) {
            securityConfig.setInsecureSerializationPolicy(config.getInsecureSerializationPolicy());
        }
        if (config.getInsecureDevPolicy() != null) {
            securityConfig.setInsecureDevPolicy(config.getInsecureDevPolicy());
        }
        if (config.getAllowedProperties() != null) {
            securityConfig.setAllowedProperties(config.getAllowedProperties());
        }

        return securityConfig;
    }

    private static Map<String, Object> extractCamelProperties(Environment environment) {
        Map<String, Object> properties = new LinkedHashMap<>();

        if (environment instanceof ConfigurableEnvironment ce) {
            ce.getPropertySources().forEach(ps -> {
                if (ps instanceof EnumerablePropertySource<?> eps) {
                    for (String name : eps.getPropertyNames()) {
                        if (name.startsWith("camel.") && !name.startsWith("camel.security.")) {
                            Object value = environment.getProperty(name);
                            if (value != null) {
                                properties.putIfAbsent(name, value);
                            }
                        }
                    }
                }
            });
        }

        return properties;
    }

    private static boolean containsSensitive(CamelContext camelContext, String key, Object value) {
        boolean answer = CamelContextHelper.containsSensitive(camelContext, key);
        if (!answer && value != null) {
            String v = value.toString();
            answer = v.startsWith("RAW(");
        }
        return answer;
    }

    private static void enforceViolations(List<SecurityViolation> violations) {
        List<String> failures = new ArrayList<>();
        for (SecurityViolation v : violations) {
            if ("fail".equals(v.policy())) {
                failures.add(v.toString());
            } else {
                LOG.warn("SECURITY WARNING: {}", v);
            }
        }

        if (!failures.isEmpty()) {
            throw new RuntimeCamelException(
                    "Security policy violations detected (policy=fail):\n - " + String.join("\n - ", failures)
                                            + "\nTo allow specific properties, add them to camel.security.allowed-properties"
                                            + " or change the policy to 'warn' or 'allow'.");
        }
    }

}
