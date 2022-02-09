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
package org.apache.camel.spring.boot.actuate.health;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({HealthIndicator.class})
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelHealthCheckConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class CamelHealthCheckAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelHealthCheckAutoConfiguration.class);

    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnClass({CamelContext.class})
    @ConditionalOnMissingBean(CamelHealthCheckIndicator.class)
    protected class CamelHealthCheckIndicatorInitializer {

        @Bean(name = "camelHealth")
        public HealthIndicator camelHealthCheckIndicator(CamelContext camelContext, CamelHealthCheckConfigurationProperties config) {
            if (config != null && config.getEnabled() != null && !config.getEnabled()) {
                // health check is disabled
                return null;
            }
            if (config == null) {
                config = new CamelHealthCheckConfigurationProperties();
            }

            HealthCheckRegistry hcr = camelContext.getExtension(HealthCheckRegistry.class);
            if (hcr == null) {
                LOG.warn("Cannot find HealthCheckRegistry from classpath. Add camel-health to classpath.");
                return null;
            }
            // lets signal we are integrated with spring boot
            hcr.setId("camel-spring-boot");

            if (config.getEnabled() != null) {
                hcr.setEnabled(config.getEnabled());
            }
            if (config.getExcludePattern() != null) {
                hcr.setExcludePattern(config.getExcludePattern());
            }

            // context is enabled by default
            if (hcr.isEnabled()) {
                HealthCheck hc = (HealthCheck) hcr.resolveById("context");
                if (hc != null) {
                    if (config.getContextEnabled() != null) {
                        hc.setEnabled(config.getContextEnabled());
                    }
                    hcr.register(hc);
                }
            }
            // routes are enabled by default
            if (hcr.isEnabled()) {
                HealthCheckRepository hc = hcr.getRepository("routes").orElse((HealthCheckRepository) hcr.resolveById("routes"));
                if (hc != null) {
                    if (config.getRoutesEnabled() != null) {
                        hc.setEnabled(config.getRoutesEnabled());
                    }
                    hcr.register(hc);
                }
            }
            // consumers are enabled by default
            if (hcr.isEnabled()) {
                HealthCheckRepository hc
                        = hcr.getRepository("consumers").orElse((HealthCheckRepository) hcr.resolveById("consumers"));
                if (hc != null) {
                    if (config.getConsumersEnabled() != null) {
                        hc.setEnabled(config.getConsumersEnabled());
                    }
                    hcr.register(hc);
                }
            }
            // registry are enabled by default
            if (hcr.isEnabled()) {
                HealthCheckRepository hc
                        = hcr.getRepository("registry").orElse((HealthCheckRepository) hcr.resolveById("registry"));
                if (hc != null) {
                    if (config.getRegistryEnabled() != null) {
                        hc.setEnabled(config.getRegistryEnabled());
                    }
                    hcr.register(hc);
                }
            }

            return new CamelHealthCheckIndicator(camelContext);
        }
    }

}
