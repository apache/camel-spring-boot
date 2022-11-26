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
package org.apache.camel.component.micrometer.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;

@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@EnableConfigurationProperties({CamelMicrometerConfiguration.class})
@ConditionalOnHierarchicalProperties({"camel.metrics"})
@AutoConfigureAfter({CamelAutoConfiguration.class})
public class CamelMicrometerAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;
    private final CamelContext camelContext;
    @Autowired
    private CamelMicrometerConfiguration configuration;

    public CamelMicrometerAutoConfiguration(
            CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Lazy
    @Bean
    public CamelContextCustomizer configureMicrometer() {
        return new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                if (configuration.isEnableRoutePolicy()) {
                    camelContext.addRoutePolicyFactory(new MicrometerRoutePolicyFactory());
                }

                ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
                if (configuration.isEnableExchangeEventNotifier()) {
                    managementStrategy.addEventNotifier(new MicrometerExchangeEventNotifier());
                }

                if (configuration.isEnableRouteEventNotifier()) {
                    managementStrategy.addEventNotifier(new MicrometerRouteEventNotifier());
                }

                if (configuration.isEnableMessageHistory()) {
                    if (!camelContext.isMessageHistory()) {
                        camelContext.setMessageHistory(true);
                    }
                    camelContext.setMessageHistoryFactory(new MicrometerMessageHistoryFactory());
                }
            }
        };
    }
}