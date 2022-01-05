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
package org.apache.camel.spring.boot.actuate.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootApplication
@SpringBootTest(
    classes = {CamelAutoConfiguration.class, CamelHealthCheckAutoConfiguration.class, DownRoute.class, MyCamelRoute.class},
    properties = {"camel.health.config[consumer\\:down-route].parent=consumers",
                  "camel.health.config[consumer\\:down-route].enabled=false"})
class CamelHealthCheckConfigurationTest {

    @Autowired
    CamelHealthCheckIndicator indicator;

    @Autowired
    CamelContext camelContext;

    @Test
    void shouldBeHealth() throws Exception {
        // 'down-route' is DOWN, but health check for this consumer should be disabled by configuration
        final Health health = indicator.health();
        assertThat(health)
            .as("Has health")
            .isNotNull()
            .as("Should be UP")
            .matches(h -> h.getStatus() == Status.UP);
    }

    @Test
    void shouldNotDisableAllConsumersHealthChecks() {
        @SuppressWarnings("resource")
        final HealthCheckRegistry registry = camelContext.getExtension(HealthCheckRegistry.class);

        assertThat(registry.getCheck("consumer:down-route"))
            .as("'down-route' health check is disabled")
            .isPresent()
            .get()
            .matches(hc -> !hc.getConfiguration().isEnabled());

        assertThat(registry.getCheck("consumer:route1"))
            .as("other route health check is enabled")
            .isPresent()
            .get()
            .matches(hc -> hc.getConfiguration().isEnabled());
    }
}
