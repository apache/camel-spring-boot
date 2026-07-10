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
import org.apache.camel.spi.SupervisingRouteController;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = CamelAutoConfiguration.class)
@ConditionalOnBooleanProperty("camel.routecontroller.enabled")
@EnableConfigurationProperties(SupervisingRouteControllerConfiguration.class)
public class SupervisingRouteControllerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SupervisingRouteController supervisingRouteController(SupervisingRouteControllerConfiguration config) {
        // switch to supervising route controller
        SupervisingRouteController src = new DefaultSupervisingRouteController();
        if (config.getIncludeRoutes() != null) {
            src.setIncludeRoutes(config.getIncludeRoutes());
        }
        if (config.getExcludeRoutes() != null) {
            src.setExcludeRoutes(config.getExcludeRoutes());
        }
        if (config.getThreadPoolSize() > 0) {
            src.setThreadPoolSize(config.getThreadPoolSize());
        }
        if (config.getBackOffDelay() != null && config.getBackOffDelay().toMillis() > 0) {
            src.setBackOffDelay(config.getBackOffDelay().toMillis());
        }
        if (config.getInitialDelay() != null && config.getInitialDelay().toMillis() > 0) {
            src.setInitialDelay(config.getInitialDelay().toMillis());
        }
        if (config.getBackOffMaxAttempts() > 0) {
            src.setBackOffMaxAttempts(config.getBackOffMaxAttempts());
        }
        if (config.getBackOffMaxDelay() != null && config.getBackOffMaxDelay().toMillis() > 0) {
            src.setBackOffMaxDelay(config.getBackOffMaxDelay().toMillis());
        }
        if (config.getBackOffMaxElapsedTime() != null && config.getBackOffMaxElapsedTime().toMillis() > 0) {
            src.setBackOffMaxElapsedTime(config.getBackOffMaxElapsedTime().toMillis());
        }
        if (config.getBackOffMultiplier() > 0) {
            src.setBackOffMultiplier(config.getBackOffMultiplier());
        }
        src.setUnhealthyOnExhausted(config.isUnhealthyOnExhausted());
        src.setUnhealthyOnRestarting(config.isUnhealthyOnRestarting());

        return src;
    }

}
