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
package org.apache.camel.spring.boot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.spi.RuntimePropertiesProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * {@link RuntimePropertiesProvider} that enumerates application properties from the Spring Boot {@link
 * ConfigurableEnvironment}. These properties are used for display purposes only (e.g. the Properties dev console) and
 * do not affect Camel's placeholder resolution.
 * <p>
 * Property sources that represent environment variables, JVM system properties, and Spring Boot internals are skipped
 * to avoid noise — only application-level configuration is included.
 */
public class SpringBootRuntimePropertiesProvider implements RuntimePropertiesProvider {

    private static final Set<String> SKIP_SOURCES = Set.of(
            "systemProperties",
            "systemEnvironment",
            "configurationProperties",
            "random");

    private final ConfigurableEnvironment environment;

    public SpringBootRuntimePropertiesProvider(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public String getSource() {
        return "Spring Boot";
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> answer = new LinkedHashMap<>();
        environment.getPropertySources().forEach(ps -> {
            if (SKIP_SOURCES.contains(ps.getName())) {
                return;
            }
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    if (!answer.containsKey(name)) {
                        try {
                            answer.put(name, environment.getProperty(name));
                        } catch (Exception e) {
                            // ignore properties that cannot be resolved
                        }
                    }
                }
            }
        });
        return answer;
    }
}
