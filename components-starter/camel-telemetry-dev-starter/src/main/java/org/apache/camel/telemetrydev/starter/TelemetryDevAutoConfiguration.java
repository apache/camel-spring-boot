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
package org.apache.camel.telemetrydev.starter;

import org.apache.camel.CamelContext;
import org.apache.camel.telemetrydev.TelemetryDevTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TelemetryDevConfigurationProperties.class)
@ConditionalOnProperty(value = "camel.telemetrydev.enabled", matchIfMissing = true)
public class TelemetryDevAutoConfiguration {

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(TelemetryDevTracer.class)
    TelemetryDevTracer openTelemetryEventNotifier(CamelContext camelContext,
            TelemetryDevConfigurationProperties config) {
        TelemetryDevTracer devTracer = new TelemetryDevTracer();

        if (config.getExcludePatterns() != null) {
            devTracer.setExcludePatterns(config.getExcludePatterns());
        }
        if (config.getTraceProcessors() != null && config.getTraceProcessors()) {
            devTracer.setTraceProcessors(config.getTraceProcessors());
        }
        if (config.getTraceFormat() != null) {
            devTracer.setTraceFormat(config.getTraceFormat());
        }

        devTracer.init(camelContext);

        return devTracer;
    }
}
