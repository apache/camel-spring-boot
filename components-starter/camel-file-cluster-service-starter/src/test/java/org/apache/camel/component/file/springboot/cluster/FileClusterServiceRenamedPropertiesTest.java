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
package org.apache.camel.component.file.springboot.cluster;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.component.file.cluster.FileLockClusterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The file cluster service options were named <tt>camel.component.file.cluster.service.*</tt> before Camel 3.10, and
 * are <tt>camel.cluster.file.*</tt> since. An option under the old prefix is not bound, and is logged at WARN with the
 * option to use instead.
 */
public class FileClusterServiceRenamedPropertiesTest {

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileLockClusterServiceAutoConfiguration.class));

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
        runner.withPropertyValues("camel.component.file.cluster.service.id=old-id").run(context -> {
            assertThat(context.getBean(FileLockClusterService.class).getId()).as("the old option is not bound")
                    .isNotEqualTo("old-id");
            assertThat(warnings()).filteredOn(m -> m.contains("camel.component.file.cluster.service.id "))
                    .singleElement().asString()
                    .contains("has no effect")
                    .contains("Use camel.cluster.file.id instead");
        });
    }

    @Test
    public void newPrefixIsBoundAndNotReported() {
        runner.withPropertyValues("camel.cluster.file.id=new-id").run(context -> {
            assertThat(context.getBean(FileLockClusterService.class).getId()).isEqualTo("new-id");
            assertThat(warnings()).noneMatch(m -> m.contains("has no effect"));
        });
    }
}
