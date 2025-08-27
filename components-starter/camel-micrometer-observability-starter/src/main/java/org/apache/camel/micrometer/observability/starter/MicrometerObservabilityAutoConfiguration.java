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
package org.apache.camel.micrometer.observability.starter;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import org.apache.camel.CamelContext;
import org.apache.camel.micrometer.observability.MicrometerObservabilityTracer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(value = {
        org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration.class,
        MicrometerTracingAutoConfiguration.class })
@EnableConfigurationProperties(MicrometerObservabilityConfigurationProperties.class)
@ConditionalOnProperty(value = "camel.micrometer.observability.enabled", matchIfMissing = true)
public class MicrometerObservabilityAutoConfiguration {

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(MicrometerObservabilityTracer.class)
    MicrometerObservabilityTracer micrometerObservationTracer(
            CamelContext camelContext,
            MicrometerObservabilityConfigurationProperties config,
            ObjectProvider<Tracer> tracer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<Propagator> propagator
            ) {
        MicrometerObservabilityTracer micrometerObservationTracer = new MicrometerObservabilityTracer();
        tracer.ifAvailable(micrometerObservationTracer::setTracer);
        observationRegistry.ifAvailable(micrometerObservationTracer::setObservationRegistry);
        propagator.ifAvailable(micrometerObservationTracer::setPropagator);

        if (config.getExcludePatterns() != null) {
            micrometerObservationTracer.setExcludePatterns(config.getExcludePatterns());
        }
        if (config.isTraceProcessors()) {
            micrometerObservationTracer.setTraceProcessors(config.isTraceProcessors());;
        }
        micrometerObservationTracer.init(camelContext);

        return micrometerObservationTracer;
    }
}
