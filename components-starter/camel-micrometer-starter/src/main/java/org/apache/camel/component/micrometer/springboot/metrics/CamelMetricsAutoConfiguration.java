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
package org.apache.camel.component.micrometer.springboot.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategy;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyDefault;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyLegacy;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifierNamingStrategy;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.micrometer.spi.InstrumentedThreadPoolFactory;
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

    public CamelMetricsAutoConfiguration(CamelContext camelContext, CamelMetricsConfiguration configuration,
            MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            configureMicrometer(camelContext, configuration, meterRegistry);
        }
    }

    private void configureMicrometer(CamelContext camelContext, CamelMetricsConfiguration configuration,
            MeterRegistry meterRegistry) {
        if (configuration.isEnableRoutePolicy()) {
            MicrometerRoutePolicyFactory factory = new MicrometerRoutePolicyFactory();
            factory.setCamelContext(camelContext);
            factory.setMeterRegistry(meterRegistry);
            if ("legacy".equalsIgnoreCase(configuration.getNamingStrategy())) {
                factory.setNamingStrategy(MicrometerRoutePolicyNamingStrategy.LEGACY);
            }
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
            MicrometerExchangeEventNotifier notifier = new MicrometerExchangeEventNotifier();
            notifier.setCamelContext(camelContext);
            notifier.setMeterRegistry(meterRegistry);
            if ("legacy".equalsIgnoreCase(configuration.getNamingStrategy())) {
                notifier.setNamingStrategy(new MicrometerExchangeEventNotifierNamingStrategyLegacy(
                    configuration.isBaseEndpointUriExchangeEventNotifier()
                ));
            } else {
                notifier.setNamingStrategy(new MicrometerExchangeEventNotifierNamingStrategyDefault(
                    configuration.isBaseEndpointUriExchangeEventNotifier()
                ));
            }
            managementStrategy.addEventNotifier(notifier);
        }

        if (configuration.isEnableRouteEventNotifier()) {
            MicrometerRouteEventNotifier notifier = new MicrometerRouteEventNotifier();
            notifier.setCamelContext(camelContext);
            notifier.setMeterRegistry(meterRegistry);
            if ("legacy".equalsIgnoreCase(configuration.getNamingStrategy())) {
                notifier.setNamingStrategy(MicrometerRouteEventNotifierNamingStrategy.LEGACY);
            }
            managementStrategy.addEventNotifier(notifier);
        }

        if (configuration.isEnableMessageHistory()) {
            if (!camelContext.isMessageHistory()) {
                camelContext.setMessageHistory(true);
            }
            MicrometerMessageHistoryFactory factory = new MicrometerMessageHistoryFactory();
            factory.setCamelContext(camelContext);
            factory.setMeterRegistry(meterRegistry);
            if ("legacy".equalsIgnoreCase(configuration.getNamingStrategy())) {
                factory.setNamingStrategy(MicrometerMessageHistoryNamingStrategy.LEGACY);
            }
            camelContext.setMessageHistoryFactory(factory);
        }

        if (configuration.isEnableInstrumentedThreadPoolFactory()) {
            InstrumentedThreadPoolFactory instrumentedThreadPoolFactory = new InstrumentedThreadPoolFactory(
                    meterRegistry,
                    camelContext.getExecutorServiceManager().getThreadPoolFactory());
            camelContext.getExecutorServiceManager().setThreadPoolFactory(instrumentedThreadPoolFactory);
        }
    }

}