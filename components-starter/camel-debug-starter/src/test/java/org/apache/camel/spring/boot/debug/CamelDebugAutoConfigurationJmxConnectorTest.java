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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.debugger.DebuggerJmxConnectorService;
import org.apache.camel.main.DebuggerConfigurationProperties;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The JMX RMI connector is opened only when it is explicitly requested.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = {
        CamelDebugAutoConfigurationJmxConnectorTest.TestConfiguration.class,
        CamelDebugAutoConfiguration.class,
        CamelAutoConfiguration.class
})
class CamelDebugAutoConfigurationJmxConnectorTest {

    private static final int PORT = AvailablePortFinder.getNextAvailable();

    @Autowired
    CamelContext camelContext;

    @Autowired
    DebuggerConfigurationProperties debuggerConfigurationProperties;

    @DynamicPropertySource
    static void debugProperties(DynamicPropertyRegistry registry) {
        registry.add("camel.debug.jmx-connector-enabled", () -> "true");
        registry.add("camel.debug.jmx-connector-port", () -> PORT);
    }

    @Test
    void shouldStartTheJmxConnectorWhenEnabled() {
        assertThat(debuggerConfigurationProperties.isJmxConnectorEnabled()).isTrue();
        assertThat(debuggerConfigurationProperties.getJmxConnectorPort()).isEqualTo(PORT);

        DebuggerJmxConnectorService service = camelContext.hasService(DebuggerJmxConnectorService.class);
        assertThat(service).isNotNull();
        assertThat(service.isStarted()).isTrue();
    }

    @Test
    void shouldListenOnTheConfiguredPortWhenEnabled() {
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", PORT), 1000);
                assertThat(socket.isConnected()).isTrue();
            }
        });
    }

    @Configuration
    static class TestConfiguration {
    }
}
