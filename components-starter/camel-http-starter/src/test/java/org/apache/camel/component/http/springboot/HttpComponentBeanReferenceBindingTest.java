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

import com.example.httpstarter.ThirdPartyHttpProperties;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A complex (object) typed option such as {@code sslContextParameters} is configured with a reference to a bean in the
 * Spring application context. A value that cannot be resolved must be reported, not silently turned into null.
 */
class HttpComponentBeanReferenceBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration.class))
            .withUserConfiguration(HttpComponentConverter.class);

    @Test
    void testBeanSyntaxIsResolved() {
        runner.withPropertyValues("camel.component.http.ssl-context-parameters=#bean:mySslContextParameters")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HttpComponentConfiguration.class).getSslContextParameters())
                            .isSameAs(context.getBean("mySslContextParameters"));
                });
    }

    @Test
    void testHashSyntaxIsResolved() {
        runner.withPropertyValues("camel.component.http.ssl-context-parameters=#mySslContextParameters")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HttpComponentConfiguration.class).getSslContextParameters())
                            .isSameAs(context.getBean("mySslContextParameters"));
                });
    }

    @Test
    void testPlainBeanIdIsResolved() {
        // a plain bean id used to be silently converted to null
        runner.withPropertyValues("camel.component.http.ssl-context-parameters=mySslContextParameters")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HttpComponentConfiguration.class).getSslContextParameters())
                            .isSameAs(context.getBean("mySslContextParameters"));
                });
    }

    @Test
    void testUnresolvableValueIsReported() {
        // a typo used to leave the option unset without any error
        runner.withPropertyValues("camel.component.http.ssl-context-parameters=#bean:mySslContextParametrs")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("mySslContextParametrs")
                            .hasStackTraceContaining("camel.component.http");
                });
    }

    @Test
    void testThirdPartyPropertiesAreNotIntercepted() {
        // HttpComponentConverter is registered with @ConfigurationPropertiesBinding and so takes part in every
        // @ConfigurationProperties binding, not only in Camel's own. Adding the starter to the classpath must not
        // make an unrelated property of the same type fail to bind.
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ThirdPartyConfiguration.class))
                .withUserConfiguration(HttpComponentConverter.class)
                .withPropertyValues("thirdparty.http.verifier=someValue")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertNull(context.getBean(ThirdPartyHttpProperties.class).getVerifier());
                });
    }

    @Test
    void testThirdPartyPropertiesStillResolveHashReferences() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ThirdPartyConfiguration.class))
                .withUserConfiguration(HttpComponentConverter.class)
                .withPropertyValues("thirdparty.http.verifier=#bean:noSuchVerifier")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("noSuchVerifier");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ThirdPartyHttpProperties.class)
    static class ThirdPartyConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(HttpComponentConfiguration.class)
    static class TestConfiguration {

        @Bean(name = "mySslContextParameters")
        SSLContextParameters mySslContextParameters() {
            return new SSLContextParameters();
        }
    }

}
