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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.main.SecurityPolicyResult;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class CamelSecurityPolicyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(CamelAutoConfiguration.class, CamelSecurityPolicyAutoConfiguration.class));

    @Test
    public void noSecurityPropertiesShouldStartNormally() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
            assertThat(result).isNotNull();
            assertThat(result.hasViolations()).isFalse();
        });
    }

    @Test
    public void policyAllowShouldIgnoreInsecureConfig() {
        runner.withPropertyValues("camel.security.policy=allow", "camel.component.http.trustAllCertificates=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isFalse();
                });
    }

    @Test
    public void policyWarnShouldStartWithViolations() {
        runner.withPropertyValues("camel.security.policy=warn", "camel.component.http.trustAllCertificates=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isTrue();
                    assertThat(result.getViolations()).anyMatch(
                            v -> v.propertyKey().contains("trustAllCertificates") && "warn".equals(v.policy()));
                });
    }

    @Test
    public void policyFailShouldPreventStartup() {
        runner.withPropertyValues("camel.security.policy=fail", "camel.component.http.trustAllCertificates=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().isInstanceOf(RuntimeCamelException.class)
                            .hasMessageContaining("Security policy violations detected");
                });
    }

    @Test
    public void categoryOverrideShouldTakePrecedence() {
        runner.withPropertyValues("camel.security.policy=fail", "camel.security.insecure-ssl-policy=allow",
                "camel.component.http.trustAllCertificates=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isFalse();
                });
    }

    @Test
    public void categoryOverrideWarnWhileGlobalFail() {
        runner.withPropertyValues("camel.security.policy=fail", "camel.security.insecure-ssl-policy=warn",
                "camel.component.http.trustAllCertificates=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isTrue();
                    assertThat(result.getViolations()).anyMatch(v -> "warn".equals(v.policy()));
                });
    }

    @Test
    public void allowedPropertiesShouldExcludeFromChecks() {
        runner.withPropertyValues("camel.security.policy=fail",
                "camel.security.allowed-properties=camel.component.http.trustAllCertificates",
                "camel.component.http.trustAllCertificates=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isFalse();
                });
    }

    @Test
    public void multipleViolationsDetected() {
        runner.withPropertyValues("camel.security.policy=warn", "camel.component.http.trustAllCertificates=true",
                "camel.component.netty.allowJavaSerializedObject=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.getViolationCount()).isGreaterThanOrEqualTo(2);
                });
    }

    @Test
    public void insecureDevPolicyOverride() {
        runner.withPropertyValues("camel.security.policy=fail", "camel.security.insecure-dev-policy=allow",
                "camel.main.devConsoleEnabled=true").run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    public void insecureSerializationPolicyOverride() {
        runner.withPropertyValues("camel.security.policy=fail", "camel.security.insecure-serialization-policy=warn",
                "camel.component.netty.allowJavaSerializedObject=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityPolicyResult result = context.getBean(SecurityPolicyResult.class);
                    assertThat(result.hasViolations()).isTrue();
                });
    }

}
