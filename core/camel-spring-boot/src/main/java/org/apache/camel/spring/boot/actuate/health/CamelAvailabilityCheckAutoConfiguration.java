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
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.actuate.health.liveness.CamelLivenessStateHealthIndicator;
import org.apache.camel.spring.boot.actuate.health.readiness.CamelReadinessStateHealthIndicator;

import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ CamelAutoConfiguration.class, ApplicationAvailabilityAutoConfiguration.class })
@ConditionalOnBean(CamelAutoConfiguration.class)
@ConditionalOnClass(LivenessStateHealthIndicator.class)
public class CamelAvailabilityCheckAutoConfiguration {

    @Bean
    public CamelLivenessStateHealthIndicator camelLivenessStateHealthIndicator(
            ApplicationAvailability applicationAvailability, CamelContext camelContext) {
        return new CamelLivenessStateHealthIndicator(applicationAvailability, camelContext);
    }

    @Bean
    public CamelReadinessStateHealthIndicator camelReadinessStateHealthIndicator(
            ApplicationAvailability applicationAvailability, CamelContext camelContext) {
        return new CamelReadinessStateHealthIndicator(applicationAvailability, camelContext);
    }
}
