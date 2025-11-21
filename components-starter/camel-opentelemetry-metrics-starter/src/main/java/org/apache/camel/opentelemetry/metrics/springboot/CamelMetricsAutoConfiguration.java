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
package org.apache.camel.opentelemetry.metrics.springboot;

import io.opentelemetry.api.metrics.Meter;
import org.apache.camel.CamelContext;
import org.apache.camel.opentelemetry.metrics.eventnotifier.OpenTelemetryExchangeEventNotifier;
import org.apache.camel.opentelemetry.metrics.eventnotifier.OpenTelemetryRouteEventNotifier;
import org.apache.camel.opentelemetry.metrics.messagehistory.OpenTelemetryMessageHistoryFactory;
import org.apache.camel.opentelemetry.metrics.routepolicy.OpenTelemetryRoutePolicyFactory;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Conditional;


@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@EnableConfigurationProperties({ CamelMetricsConfiguration.class })
@AutoConfigureAfter({ CamelAutoConfiguration.class })
public class CamelMetricsAutoConfiguration {

    public CamelMetricsAutoConfiguration(CamelContext camelContext, CamelMetricsConfiguration configuration, Meter meter) {
        if (meter != null) {
            configureOpenTelemetry(camelContext, configuration, meter);
        }
    }

    private void configureOpenTelemetry(CamelContext camelContext, CamelMetricsConfiguration configuration, Meter meter) {
        if (configuration.isEnableRoutePolicy()) {
            OpenTelemetryRoutePolicyFactory factory = new OpenTelemetryRoutePolicyFactory();
            factory.setCamelContext(camelContext);
            factory.setMeter(meter);
            if ("all".equalsIgnoreCase(configuration.getRoutePolicyLevel())) {
                factory.getPolicyConfiguration().setContextEnabled(true);
                factory.getPolicyConfiguration().setRouteEnabled(true);
            } else if ("context".equalsIgnoreCase(configuration.getRoutePolicyLevel())) {
                factory.getPolicyConfiguration().setContextEnabled(true);
                factory.getPolicyConfiguration().setRouteEnabled(false);
            } else {
                factory.getPolicyConfiguration().setContextEnabled(false);
                factory.getPolicyConfiguration().setRouteEnabled(true);
            }
            factory.getPolicyConfiguration().setExcludePattern(configuration.getRoutePolicyExcludePattern());
            camelContext.addRoutePolicyFactory(factory);
        }

        ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
        if (configuration.isEnableExchangeEventNotifier()) {
            OpenTelemetryExchangeEventNotifier notifier = new OpenTelemetryExchangeEventNotifier();
            notifier.setCamelContext(camelContext);
            notifier.setMeter(meter);
            notifier.setBaseEndpointURI(configuration.isBaseEndpointUriExchangeEventNotifier());
            managementStrategy.addEventNotifier(notifier);
        }

        if (configuration.isEnableRouteEventNotifier()) {
            OpenTelemetryRouteEventNotifier notifier = new OpenTelemetryRouteEventNotifier();
            notifier.setCamelContext(camelContext);
            notifier.setMeter(meter);
            managementStrategy.addEventNotifier(notifier);
        }

        if (configuration.isEnableMessageHistory()) {
            if (!camelContext.isMessageHistory()) {
                camelContext.setMessageHistory(true);
            }
            OpenTelemetryMessageHistoryFactory factory = new OpenTelemetryMessageHistoryFactory();
            factory.setCamelContext(camelContext);
            factory.setMeter(meter);
            camelContext.setMessageHistoryFactory(factory);
        }
    }
}