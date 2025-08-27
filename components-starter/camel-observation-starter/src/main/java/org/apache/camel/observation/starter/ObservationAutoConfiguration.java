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
package org.apache.camel.observation.starter;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;

import org.apache.camel.CamelContext;
import org.apache.camel.observation.MicrometerObservationTracer;

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
@EnableConfigurationProperties(ObservationConfigurationProperties.class)
@ConditionalOnProperty(value = "camel.observation.enabled", matchIfMissing = true)
public class ObservationAutoConfiguration {

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(MicrometerObservationTracer.class)
    MicrometerObservationTracer micrometerObservationTracer(CamelContext camelContext,
            ObservationConfigurationProperties config, ObjectProvider<Tracer> tracer,
            ObjectProvider<ObservationRegistry> observationRegistry) {
        MicrometerObservationTracer micrometerObservationTracer = new MicrometerObservationTracer();
        tracer.ifAvailable(micrometerObservationTracer::setTracer);
        observationRegistry.ifAvailable(micrometerObservationTracer::setObservationRegistry);

        if (config.getExcludePatterns() != null) {
            micrometerObservationTracer.setExcludePatterns(config.getExcludePatterns());
        }
        if (config.getEncoding() != null) {
            micrometerObservationTracer.setEncoding(config.getEncoding().booleanValue());
        }
        micrometerObservationTracer.init(camelContext);

        return micrometerObservationTracer;
    }

    @Bean
    // No-op version to suppress metric creation which may explode the length
    // of actuator as seen in CAMEL-22349
    public TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(Tracer tracer) {
        return new TracingAwareMeterObservationHandler<>(
            new MeterObservationHandler<Observation.Context>() {},
            tracer
        ) {
            @Override
            public void onEvent(Observation.Event event, Observation.Context context) {}
        };
    }

}
