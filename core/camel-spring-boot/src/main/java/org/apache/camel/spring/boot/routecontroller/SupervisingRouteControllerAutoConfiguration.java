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
package org.apache.camel.spring.boot.routecontroller;

import org.apache.camel.impl.engine.DefaultSupervisingRouteController;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.SupervisingRouteController;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(CamelAutoConfiguration.class)
@ConditionalOnProperty(prefix = "camel.routecontroller", name = "enabled")
@EnableConfigurationProperties(SupervisingRouteControllerConfiguration.class)
public class SupervisingRouteControllerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SupervisingRouteController supervisingRouteController(SupervisingRouteControllerConfiguration config) {
        SupervisingRouteController src = null;

        if (config.isEnabled()) {
            // switch to supervising route controller
            src = new DefaultSupervisingRouteController();
            if (config.getIncludeRoutes() != null) {
                src.setIncludeRoutes(config.getIncludeRoutes());
            }
            if (config.getExcludeRoutes() != null) {
                src.setExcludeRoutes(config.getExcludeRoutes());
            }
            if (config.getThreadPoolSize() > 0) {
                src.setThreadPoolSize(config.getThreadPoolSize());
            }
            if (config.getBackOffDelay() > 0) {
                src.setBackOffDelay(config.getBackOffDelay());
            }
            if (config.getInitialDelay() > 0) {
                src.setInitialDelay(config.getInitialDelay());
            }
            if (config.getBackOffMaxAttempts() > 0) {
                src.setBackOffMaxAttempts(config.getBackOffMaxAttempts());
            }
            if (config.getBackOffMaxDelay() > 0) {
                src.setBackOffMaxDelay(config.getBackOffMaxDelay());
            }
            if (config.getBackOffMaxElapsedTime() > 0) {
                src.setBackOffMaxElapsedTime(config.getBackOffMaxElapsedTime());
            }
            if (config.getBackOffMultiplier() > 0) {
                src.setBackOffMultiplier(config.getBackOffMultiplier());
            }
            src.setUnhealthyOnExhausted(config.isUnhealthyOnExhausted());
            src.setUnhealthyOnRestarting(config.isUnhealthyOnRestarting());
        }

        return src;
    }

}
