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

            // configure camel health check
            // context is enabled by default
            if (!config.getConfig().containsKey("context") || config.getContextEnabled() != null) {
                HealthCheck hc = (HealthCheck) hcr.resolveById("context");
                if (hc != null) {
                    if (config.getContextEnabled() != null) {
                        hc.getConfiguration().setEnabled(config.getContextEnabled());
                    }
                    hcr.register(hc);
                }
            }
            // routes is enabled by default
            if (hcr.isEnabled() && (!config.getConfig().containsKey("routes") || config.getRoutesEnabled() != null)) {
                HealthCheckRepository hc = hcr.getRepository("routes").orElse((HealthCheckRepository) hcr.resolveById("routes"));
                if (hc != null) {
                    if (config.getRoutesEnabled() != null) {
                        hc.setEnabled(config.getRoutesEnabled());
                    }
                    hcr.register(hc);
                }
            }
            // registry is enabled by default
            final CamelHealthCheckConfigurationProperties lambdaConfig = config;
            if (hcr.isEnabled() && (!config.getConfig().containsKey("registry") || config.getRegistryEnabled() != null)) {
                hcr.getRepository("registry").ifPresent(h -> {
                    if (lambdaConfig.getRegistryEnabled() != null) {
                        h.setEnabled(lambdaConfig.getRegistryEnabled());
                    }
                });
            }

            // configure health checks configurations
            for (String id : config.getConfig().keySet()) {
                CamelHealthCheckConfigurationProperties.HealthCheckConfigurationProperties hcc = config.getConfig().get(id);
                String parent = hcc.getParent();
                // lookup health check by id
                Object hc = hcr.getCheck(parent).orElse(null);
                if (hc == null) {
                    hc = hcr.resolveById(parent);
                    if (hc == null) {
                        LOG.warn("Cannot resolve HealthCheck with id: " + parent + " from classpath.");
                        continue;
                    }
                    hcr.register(hc);
                    if (hc instanceof HealthCheck) {
                        ((HealthCheck) hc).getConfiguration().setParent(hcc.getParent());
                        ((HealthCheck) hc).getConfiguration().setEnabled(hcc.getEnabled() != null ? hcc.getEnabled() : true);
                        ((HealthCheck) hc).getConfiguration().setFailureThreshold(hcc.getFailureThreshold());
                        ((HealthCheck) hc).getConfiguration().setInterval(hcc.getInterval());
                    } else if (hc instanceof HealthCheckRepository) {
                        ((HealthCheckRepository) hc).setEnabled(hcc.getEnabled() != null ? hcc.getEnabled() : true);
                        ((HealthCheckRepository) hc).addConfiguration(id, hcc.toHealthCheckConfiguration());
                    }
                }
            }

            return new CamelHealthCheckIndicator(camelContext);
        }
    }

}
