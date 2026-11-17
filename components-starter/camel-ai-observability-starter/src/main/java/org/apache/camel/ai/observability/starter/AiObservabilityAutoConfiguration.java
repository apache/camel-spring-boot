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

import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.observability.GenAiObservabilityProperties;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = CamelAutoConfiguration.class)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(AiObservabilityConfigurationProperties.class)
public class AiObservabilityAutoConfiguration {

    @Bean
    CamelContextConfiguration aiObservabilityCamelContextConfiguration(Environment environment) {
        boolean enabled = resolveEnabled(environment);
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                applyEnabledProperty(camelContext, enabled);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
            }
        };
    }

    static boolean resolveEnabled(Environment environment) {
        Boolean canonical = environment.getProperty(GenAiObservabilityProperties.ENABLED, Boolean.class);
        if (canonical != null) {
            return canonical;
        }
        return Binder.get(environment)
                .bind("camel.aiobservability", Bindable.of(AiObservabilityConfigurationProperties.class))
                .map(AiObservabilityConfigurationProperties::isEnabled)
                .orElse(true);
    }

    /**
     * Hands the option to the Camel PropertiesComponent as an override property, matching Camel Main and JBang. The AI
     * producers read it on the threads that route the exchanges; the local properties of the PropertiesComponent belong
     * to the thread that sets them, so they would only be seen at startup.
     */
    static void applyEnabledProperty(CamelContext camelContext, boolean enabled) {
        camelContext.getPropertiesComponent().addOverrideProperty(GenAiObservabilityProperties.ENABLED,
                Boolean.toString(enabled));
    }
}
