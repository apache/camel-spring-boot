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
package org.apache.camel.spring.boot.debug;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.debugger.DebuggerJmxConnectorService;
import org.apache.camel.main.DebuggerConfigurationProperties;
import org.apache.camel.spi.BacklogDebugger;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The debugger itself is installed when the starter is on the classpath, but the JMX RMI connector it can expose is
 * opt-in, so no socket is opened by default.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = {
        CamelDebugAutoConfigurationDefaultTest.TestConfiguration.class,
        CamelDebugAutoConfiguration.class,
        CamelAutoConfiguration.class
})
class CamelDebugAutoConfigurationDefaultTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    CamelDebugConfigurationProperties configurationProperties;

    @Autowired
    DebuggerConfigurationProperties debuggerConfigurationProperties;

    @Test
    void shouldEnableTheDebuggerByDefault() {
        assertThat(configurationProperties.isEnabled()).isTrue();
        assertThat(debuggerConfigurationProperties.isEnabled()).isTrue();
        assertThat(camelContext.hasService(BacklogDebugger.class)).isNotNull();
    }

    @Test
    void shouldNotEnableTheJmxConnectorByDefault() {
        assertThat(configurationProperties.isJmxConnectorEnabled()).isFalse();
        assertThat(debuggerConfigurationProperties.isJmxConnectorEnabled()).isFalse();
    }

    @Test
    void shouldNotStartAnyJmxConnectorServiceByDefault() {
        assertThat(camelContext.hasService(DebuggerJmxConnectorService.class)).isNull();
    }

    @Configuration
    static class TestConfiguration {
    }
}
