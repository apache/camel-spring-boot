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
package org.apache.camel.service.lra.springboot;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.saga.CamelSagaService;
import org.apache.camel.service.lra.LRASagaService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The LRA options were named <tt>camel.service.lra.*</tt> before Camel 3.4, and are <tt>camel.lra.*</tt> since. An
 * option under the old prefix is not bound, and is logged at WARN with the option to use instead.
 */
public class LraServiceRenamedPropertiesTest {

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CamelAutoConfiguration.class, LraServiceAutoConfiguration.class));

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
        runner.withPropertyValues("camel.service.lra.enabled=true",
                "camel.service.lra.coordinator-url=http://lra-coordinator:8080").run(context -> {
                    assertThat(context).as("the old options are not bound").doesNotHaveBean(CamelSagaService.class);
                    assertThat(warnings()).filteredOn(m -> m.contains("camel.service.lra.enabled "))
                            .singleElement().asString()
                            .contains("has no effect")
                            .contains("Use camel.lra.enabled instead");
                    assertThat(warnings()).filteredOn(m -> m.contains("camel.service.lra.coordinator-url "))
                            .singleElement().asString()
                            .contains("Use camel.lra.coordinator-url instead");
                });
    }

    @Test
    public void newPrefixIsBoundAndNotReported() {
        runner.withPropertyValues("camel.lra.enabled=true", "camel.lra.coordinator-url=http://lra-coordinator:8080",
                "camel.lra.local-participant-url=http://localhost:8080").run(context -> {
                    assertThat(context.getBean(LRASagaService.class).getCoordinatorUrl())
                            .isEqualTo("http://lra-coordinator:8080");
                    assertThat(warnings()).noneMatch(m -> m.contains("has no effect"));
                });
    }
}
