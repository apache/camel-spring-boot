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
package org.apache.camel.observability.services.springboot;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ObservabilityServicesTestApplication.class)
public class ObservabilityServicesEnvironmentPostProcessorTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void defaultsAreApplied() {
        assertTrue(environment.getPropertySources()
                .contains(ObservabilityServicesEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));

        assertEquals("health,prometheus", environment.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("metrics", environment.getProperty("management.endpoints.web.path-mapping.prometheus"));
        assertEquals("true", environment.getProperty("camel.metrics.log-metrics-on-shutdown"));
        assertEquals("app.info,camel.exchanges.*,process.cpu.usage,jvm.memory.max,jvm.memory.used",
                environment.getProperty("camel.metrics.log-metrics-on-shutdown-filters"));
        assertEquals("prometheus",
                environment.getProperty("camel.metrics.log-metrics-on-shutdown-format"));
        assertEquals("true", environment.getProperty("management.endpoint.health.probes.enabled"));
        assertEquals("true", environment.getProperty("management.health.readinessState.enabled"));
        assertEquals("true", environment.getProperty("management.health.livenessState.enabled"));
        assertEquals("livenessState,camelLivenessState",
                environment.getProperty("management.endpoint.health.group.live.include"));
        assertEquals("readinessState,camelReadinessState",
                environment.getProperty("management.endpoint.health.group.ready.include"));
    }

    @Test
    public void managementListenerBindsToLoopback() {
        // the port alone would put a second listener on every interface
        assertEquals("127.0.0.1", environment.getProperty("management.server.address"));
    }

    @Test
    public void aggregateHealthDetailsRequireAuthorization() {
        assertEquals("when-authorized", environment.getProperty("management.endpoint.health.show-details"));
    }

    @Test
    public void probeGroupsKeepTheirDetails() {
        // the kubelet reads these unauthenticated and surfaces the body in a probe-failure event; the groups
        // hold availability-state indicators only, which report a status and no data
        assertEquals("always", environment.getProperty("management.endpoint.health.group.live.show-details"));
        assertEquals("always", environment.getProperty("management.endpoint.health.group.ready.show-details"));
    }

    @Test
    public void camelHealthExposureLevelIsNotForced() {
        // 'full' is an opt-in; left alone, Camel's own 'default' level applies and filters health check metadata
        assertNull(environment.getProperty("camel.health.exposure-level"));
    }

    @Test
    public void injectedPropertiesAreTheDocumentedSet() {
        EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) environment.getPropertySources()
                .get(ObservabilityServicesEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);

        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                "management.server.port",
                "management.server.address",
                "management.endpoints.web.exposure.include",
                "management.endpoints.web.base-path",
                "management.endpoints.web.path-mapping.prometheus",
                "camel.metrics.log-metrics-on-shutdown",
                "camel.metrics.log-metrics-on-shutdown-filters",
                "camel.metrics.log-metrics-on-shutdown-format",
                "camel.opentelemetry2.enabled",
                "management.endpoint.health.probes.enabled",
                "management.health.readinessState.enabled",
                "management.health.livenessState.enabled",
                "management.endpoint.health.show-details",
                "management.endpoint.health.group.live.include",
                "management.endpoint.health.group.live.show-details",
                "management.endpoint.health.group.ready.include",
                "management.endpoint.health.group.ready.show-details"));

        // keep this in sync with the table in src/main/doc/usage.adoc
        assertEquals(expected, new LinkedHashSet<>(Arrays.asList(source.getPropertyNames())));
    }

    @Test
    public void userApplicationPropertiesOverrideDefaults() {
        // these values are set in src/test/resources/application.properties and must win
        // over the starter defaults, which was not possible when the defaults were
        // packaged as config/application.properties inside the starter jar
        assertEquals("9999", environment.getProperty("management.server.port"));
        assertEquals("/custom-observe", environment.getProperty("management.endpoints.web.base-path"));
        assertEquals("false", environment.getProperty("camel.opentelemetry2.enabled"));
    }
}
