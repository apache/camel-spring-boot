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
package org.apache.camel.opentelemetry2.starter;

import org.apache.camel.CamelContext;
import org.apache.camel.opentelemetry2.OpenTelemetryTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenTelemetry2ConfigurationProperties.class)
@ConditionalOnProperty(value = "camel.opentelemetry2.enabled", matchIfMissing = true)
public class OpenTelemetry2AutoConfiguration {

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(OpenTelemetryTracer.class)
    OpenTelemetryTracer openTelemetryEventNotifier(CamelContext camelContext,
            OpenTelemetry2ConfigurationProperties config) {
        OpenTelemetryTracer ottracer = new OpenTelemetryTracer();
        if (config.getExcludePatterns() != null) {
            ottracer.setExcludePatterns(config.getExcludePatterns());
        }
        if (config.getTraceProcessors() != null ) {
            ottracer.setTraceProcessors(config.getTraceProcessors());
        }
        ottracer.init(camelContext);

        return ottracer;
    }
}
