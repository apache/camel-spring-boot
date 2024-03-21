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

import java.time.Duration;
import java.time.ZonedDateTime;
import org.apache.camel.spring.boot.actuate.health.liveness.CamelLivenessStateHealthIndicator;
import org.apache.camel.spring.boot.actuate.health.readiness.CamelReadinessStateHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration class that replace synchronous Camel Health Checks with asynchronous ones.
 *
 * This implementation is based on https://github.com/spring-projects/spring-boot/issues/2652 that most probably will be
 * added in spring boot 3.2.x as a new feature in the future.
 *
 * TODO: To be refactored once async health contributors feature will be added in spring boot.
 */
@Configuration
@ConditionalOnProperty(prefix = "camel.health", name = "async-camel-health-check", havingValue = "true")
public class AsyncHealthIndicatorAutoConfiguration implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(AsyncHealthIndicatorAutoConfiguration.class);

    private HealthContributorRegistry healthContributorRegistry;
    private TaskScheduler taskScheduler;
    private CamelHealthCheckConfigurationProperties config;

    public AsyncHealthIndicatorAutoConfiguration(HealthContributorRegistry healthContributorRegistry,
            CamelHealthCheckConfigurationProperties config) {
        this.healthContributorRegistry = healthContributorRegistry;
        this.config = config;

        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(config.getHealthCheckPoolSize());
        threadPoolTaskScheduler.setThreadNamePrefix(config.getHealthCheckThreadNamePrefix());
        threadPoolTaskScheduler.initialize();
        taskScheduler = threadPoolTaskScheduler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (NamedContributor<?> namedContributor : healthContributorRegistry) {
            final String name = namedContributor.getName();
            final Object contributor = namedContributor.getContributor();
            if (contributor instanceof CamelHealthCheckIndicator
                    || contributor instanceof CamelLivenessStateHealthIndicator
                    || contributor instanceof CamelReadinessStateHealthIndicator) {
                HealthIndicator camelHealthCheckIndicator = (HealthIndicator) contributor;
                healthContributorRegistry.unregisterContributor(name);
                log.debug("Wrapping " + contributor.getClass().getSimpleName() + " for async health scheduling");
                WrappedHealthIndicator wrappedHealthIndicator = new WrappedHealthIndicator(camelHealthCheckIndicator);
                healthContributorRegistry.registerContributor(name, wrappedHealthIndicator);
                taskScheduler.scheduleWithFixedDelay(wrappedHealthIndicator,
                        Duration.ofSeconds(config.getHealthCheckFrequency()));
            }
        }
    }

    /**
     * Health Check Indicator that executes Health Checks within a Task Scheduler
     */
    private static class WrappedHealthIndicator implements HealthIndicator, Runnable {
        private static final String LAST_CHECKED_KEY = "lastChecked";
        private static final String LAST_DURATION_KEY = "lastDuration";

        private HealthIndicator wrappedHealthIndicator;

        private Health lastHealth;

        public WrappedHealthIndicator(HealthIndicator wrappedHealthIndicator) {
            this.wrappedHealthIndicator = wrappedHealthIndicator;
        }

        @Override
        public Health health() {
            Health lastHealth = getLastHealth();
            if (lastHealth == null) {
                setLastHealth(getAndWrapHealth());
                lastHealth = getLastHealth();
            }

            return lastHealth;
        }

        private Health getAndWrapHealth() {
            ZonedDateTime startTime = ZonedDateTime.now();
            Health baseHealth = getWrappedHealthIndicator().health();
            ZonedDateTime endTime = ZonedDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            return Health.status(baseHealth.getStatus()).withDetails(baseHealth.getDetails())
                    .withDetail(LAST_CHECKED_KEY, startTime).withDetail(LAST_DURATION_KEY, duration).build();
        }

        @Override
        public void run() {
            setLastHealth(getAndWrapHealth());
        }

        public HealthIndicator getWrappedHealthIndicator() {
            return wrappedHealthIndicator;
        }

        public Health getLastHealth() {
            return lastHealth;
        }

        public void setLastHealth(Health lastHealth) {
            this.lastHealth = lastHealth;
        }
    }
}
