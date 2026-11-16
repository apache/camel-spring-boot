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
package org.apache.camel.undertow.spring.boot;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The provider options were named <tt>camel.component.undertow.spring.security.*</tt> before Camel 3.10, and are
 * <tt>camel.security.undertow.*</tt> since. An option under the old prefix is not bound, and is logged at WARN with the
 * option to use instead.
 */
public class UndertowSpringSecurityRenamedPropertiesTest {

    private static final String OLD_PREFIX = "camel.component.undertow.spring.security.";

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(UndertowSpringSecurityCustomizer.class)
            .withBean(DelegatingFilterProxyRegistrationBean.class,
                    () -> new DelegatingFilterProxyRegistrationBean("springSecurityFilterChain"));

    @BeforeEach
    public void startLogs() {
        logs.start();
        rootLogger().addAppender(logs);
    }

    @AfterEach
    public void stopLogs() {
        rootLogger().detachAppender(logs);
        logs.stop();
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN))
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    public void oldPrefixIsReportedWithTheNewOption() {
        runner.withPropertyValues(OLD_PREFIX + "keycloak.url=http://localhost:8080",
                OLD_PREFIX + "keycloak.realm-id=my-realm",
                OLD_PREFIX + "keycloak.client-id=camel-client").run(context -> {
                    assertThat(context).as("no provider is bound from the old options").hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .hasMessageContaining("camel.security.undertow.keycloak");
                    assertThat(warnings()).filteredOn(m -> m.contains(OLD_PREFIX + "keycloak.url "))
                            .singleElement().asString()
                            .contains("has no effect")
                            .contains("Use camel.security.undertow.keycloak.url instead");
                    assertThat(warnings()).filteredOn(m -> m.contains(OLD_PREFIX + "keycloak.realm-id "))
                            .singleElement().asString()
                            .contains("Use camel.security.undertow.keycloak.realm-id instead");
                });
    }

    @Test
    public void newPrefixIsBoundAndNotReported() {
        runner.withPropertyValues("camel.security.undertow.keycloak.url=http://localhost:8080",
                "camel.security.undertow.keycloak.realm-id=my-realm",
                "camel.security.undertow.keycloak.client-id=camel-client").run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class);
                    assertThat(warnings()).noneMatch(m -> m.contains("has no effect"));
                });
    }
}
