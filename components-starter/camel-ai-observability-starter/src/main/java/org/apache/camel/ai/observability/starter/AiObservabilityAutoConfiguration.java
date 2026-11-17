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
package org.apache.camel.ai.observability.starter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.apache.camel.component.ai.observability.GenAiObservabilityProperties.CONTENT_MAX_LENGTH;
import static org.apache.camel.component.ai.observability.GenAiObservabilityProperties.DEFER_TO_FRAMEWORK;
import static org.apache.camel.component.ai.observability.GenAiObservabilityProperties.ENABLED;
import static org.apache.camel.component.ai.observability.GenAiObservabilityProperties.LEVEL;

@AutoConfiguration(after = CamelAutoConfiguration.class)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(AiObservabilityConfigurationProperties.class)
public class AiObservabilityAutoConfiguration {

    @Bean
    CamelContextConfiguration aiObservabilityCamelContextConfiguration(Environment environment) {
        Map<String, String> properties = resolveProperties(environment);
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                applyProperties(camelContext, properties);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
            }
        };
    }

    /**
     * Resolves every option under the key the AI producers read. A property set under that key
     * ({@code camel.aiObservability.level}) wins, as in Camel Main; otherwise the relaxed binding of
     * {@code camel.aiobservability.*} gives the value, or its default.
     */
    static Map<String, String> resolveProperties(Environment environment) {
        AiObservabilityConfigurationProperties bound = Binder.get(environment)
                .bind("camel.aiobservability", Bindable.of(AiObservabilityConfigurationProperties.class))
                .orElseGet(AiObservabilityConfigurationProperties::new);
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(ENABLED, String.valueOf(environment.getProperty(ENABLED, Boolean.class, bound.isEnabled())));
        properties.put(LEVEL, environment.getProperty(LEVEL, bound.getLevel()));
        properties.put(CONTENT_MAX_LENGTH, String.valueOf(
                environment.getProperty(CONTENT_MAX_LENGTH, Integer.class, bound.getContentMaxLength())));
        properties.put(DEFER_TO_FRAMEWORK,
                String.valueOf(environment.getProperty(DEFER_TO_FRAMEWORK, Boolean.class, bound.isDeferToFramework())));
        return properties;
    }

    static boolean resolveEnabled(Environment environment) {
        return Boolean.parseBoolean(resolveProperties(environment).get(ENABLED));
    }

    /**
     * Hands the options to the Camel PropertiesComponent as override properties, matching Camel Main and JBang. The AI
     * producers read them on the threads that route the exchanges; the local properties of the PropertiesComponent
     * belong to the thread that sets them, so they would only be seen at startup.
     */
    static void applyProperties(CamelContext camelContext, Map<String, String> properties) {
        PropertiesComponent pc = camelContext.getPropertiesComponent();
        properties.forEach(pc::addOverrideProperty);
    }
}
