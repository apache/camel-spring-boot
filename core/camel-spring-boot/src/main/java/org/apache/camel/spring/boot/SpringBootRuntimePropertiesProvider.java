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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
 * to avoid noise — only application-level configuration is included. However, camel/spring/server/management keys from
 * env/sys sources are still included since they represent application configuration.
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
    public Collection<Property> getProperties() {
        Collection<Property> answer = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        environment.getPropertySources().forEach(ps -> {
            boolean skipped = SKIP_SOURCES.contains(ps.getName());
            String source = toSourceLabel(ps.getName());
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    if (!seen.contains(name)) {
                        if (skipped && !isApplicationKey(name)) {
                            continue;
                        }
                        seen.add(name);
                        try {
                            Object value = environment.getProperty(name);
                            answer.add(new Property(name, value, source));
                        } catch (Exception e) {
                            // ignore properties that cannot be resolved
                        }
                    }
                }
            }
        });
        return answer;
    }

    private static boolean isApplicationKey(String name) {
        String lower = name.toLowerCase();
        return lower.startsWith("camel.") || lower.startsWith("camel_")
                || lower.startsWith("spring.") || lower.startsWith("spring_")
                || lower.startsWith("server.") || lower.startsWith("server_")
                || lower.startsWith("management.") || lower.startsWith("management_");
    }

    private static String toSourceLabel(String sourceName) {
        if ("systemProperties".equals(sourceName)) {
            return "JVM";
        } else if ("systemEnvironment".equals(sourceName)) {
            return "ENV";
        } else if ("server.ports".equals(sourceName)) {
            return "Spring Boot";
        } else {
            return "Spring Boot";
        }
    }
}
