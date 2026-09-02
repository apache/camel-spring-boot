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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Contributes the opinionated observability defaults as the lowest precedence property source so that any
 * user-provided configuration (application.properties, environment variables, system properties, ...) overrides them
 * following the standard Spring Boot precedence rules.
 * <p>
 * The defaults stay within the Spring Boot baseline: the management listener binds to loopback, and the aggregate
 * health endpoint only shows its details to an authorized caller. The {@code live} and {@code ready} probe groups
 * keep {@code show-details=always} because they are consumed unauthenticated by the kubelet, and the indicators
 * they contain report an availability state and nothing else.
 */
public class ObservabilityServicesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "camel-observability-services";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("management.server.port", "9876");
        // bind the management listener to loopback; exposing it beyond the host is a conscious step
        defaults.put("management.server.address", "127.0.0.1");
        defaults.put("management.endpoints.web.exposure.include", "health,prometheus");
        defaults.put("management.endpoints.web.base-path", "/observe");
        defaults.put("management.endpoints.web.path-mapping.prometheus", "metrics");
        // Metrics
        defaults.put("camel.metrics.log-metrics-on-shutdown", "true");
        defaults.put("camel.metrics.log-metrics-on-shutdown-filters", "app.info,camel.exchanges.*,process.cpu.usage,jvm.memory.max,jvm.memory.used");
        defaults.put("camel.metrics.log-metrics-on-shutdown-format", "prometheus");
        // Opentelemetry
        defaults.put("camel.opentelemetry2.enabled", "true");
        // Health
        defaults.put("management.endpoint.health.probes.enabled", "true");
        defaults.put("management.health.readinessState.enabled", "true");
        defaults.put("management.health.livenessState.enabled", "true");
        defaults.put("management.endpoint.health.show-details", "when-authorized");
        // /observe/health/live remap; the group carries availability states only, and the kubelet reads it
        // unauthenticated, so its details stay visible
        defaults.put("management.endpoint.health.group.live.include", "livenessState,camelLivenessState");
        defaults.put("management.endpoint.health.group.live.show-details", "always");
        // /observe/health/ready remap; see the note on the live group above
        defaults.put("management.endpoint.health.group.ready.include", "readinessState,camelReadinessState");
        defaults.put("management.endpoint.health.group.ready.show-details", "always");

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        // run after ConfigDataEnvironmentPostProcessor so user configuration is already present
        return Ordered.LOWEST_PRECEDENCE;
    }
}
