/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.observation.starter.observation;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Taken from https://github.com/spring-projects/spring-boot/tree/v3.0.2/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/observation.
 */
@AutoConfiguration(after = { CompositeMeterRegistryAutoConfiguration.class, MicrometerTracingAutoConfiguration.class }, afterName = "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration")
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnMissingBean(ObservationRegistry.class)
@EnableConfigurationProperties(ObservationProperties.class)
public class ObservationAutoConfiguration {

	@Bean
	static ObservationRegistryPostProcessor camelObservationRegistryPostProcessor(
			ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
			ObjectProvider<ObservationFilter> observationFilters) {
		return new ObservationRegistryPostProcessor(observationRegistryCustomizers, observationPredicates,
				observationConventions, observationHandlers, observationHandlerGrouping, observationFilters);
	}

	@Bean
	@ConditionalOnMissingBean
	ObservationRegistry camelObservationRegistry() {
		return ObservationRegistry.create();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
	static class CamelOnlyMetricsConfiguration {

		@Bean
		ObservationHandlerGrouping camelMetricsObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(MeterObservationHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Tracer.class)
	@ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
	static class CamelOnlyTracingConfiguration {

		@Bean
		ObservationHandlerGrouping camelTracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(TracingObservationHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ MeterRegistry.class, Tracer.class })
	static class CamelMetricsWithTracingConfiguration {

		@Bean
		ObservationHandlerGrouping camelMetricsAndTracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(
					List.of(TracingObservationHandler.class, MeterObservationHandler.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(MeterRegistry.class)
	@ConditionalOnMissingBean(MeterObservationHandler.class)
	static class CamelMeterObservationHandlerConfiguration {

		@ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
		@Configuration(proxyBeanMethods = false)
		static class CamelOnlyMetricsMeterObservationHandlerConfiguration {

			@Bean
			DefaultMeterObservationHandler camelDefaultMeterObservationHandler(MeterRegistry meterRegistry) {
				return new DefaultMeterObservationHandler(meterRegistry);
			}

		}

		@ConditionalOnBean(Tracer.class)
		@Configuration(proxyBeanMethods = false)
		static class CamelTracingAndMetricsObservationHandlerConfiguration {

			@Bean
			TracingAwareMeterObservationHandler<Observation.Context> cameltracingAwareMeterObservationHandler(
					MeterRegistry meterRegistry, Tracer tracer) {
				DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
				return new TracingAwareMeterObservationHandler<>(delegate, tracer);
			}

		}

	}

}
