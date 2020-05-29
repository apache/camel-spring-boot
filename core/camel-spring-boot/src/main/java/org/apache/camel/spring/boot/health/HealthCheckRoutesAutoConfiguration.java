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
package org.apache.camel.spring.boot.health;

import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.GroupCondition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(CamelAutoConfiguration.class)
@Conditional(HealthCheckRoutesAutoConfiguration.Condition.class)
@EnableConfigurationProperties(HealthCheckRoutesConfiguration.class)
public class HealthCheckRoutesAutoConfiguration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnMissingBean(RoutesHealthCheckRepository.class)
    public HealthCheckRepository routesHealthCheckRepository(HealthCheckRoutesConfiguration configuration) {
        if (configuration.isEnabled()) {
            return new RoutesHealthCheckRepository();
        } else {
            return null;
        }
    }

    // ***************************************
    // Condition
    // ***************************************

    public static class Condition extends GroupCondition {
        public Condition() {
            super(
                HealthConstants.HEALTH_PREFIX,
                HealthConstants.HEALTH_CHECK_ROUTES_PREFIX
            );
        }
    }
}
