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
package org.apache.camel.spring.boot.k;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ApplicationShutdownCustomizer implements CamelContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownCustomizer.class);

    private final ApplicationContext applicationContext;
    private final ApplicationConfiguration config;

    public ApplicationShutdownCustomizer(
            ApplicationContext applicationContext,
            ApplicationConfiguration config) {

        this.applicationContext = applicationContext;
        this.config = config;
    }

    @Override
    public void configure(CamelContext camelContext) {
        if (this.config.getShutdown().getMaxMessages() > 0) {
            LOGGER.info(
                    "Configure the JVM to terminate after {} messages and none inflight (strategy: {})",
                    this.config.getShutdown().getMaxMessages(),
                    this.config.getShutdown().getStrategy());

            camelContext.getManagementStrategy().addEventNotifier(
                    new ShutdownEventHandler(applicationContext, camelContext, config));
        }
    }

    private static final class ShutdownEventHandler extends EventNotifierSupport {
        private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownEventHandler.class);

        private final ApplicationContext applicationContext;
        private final CamelContext camelContext;
        private final ApplicationConfiguration config;
        private final AtomicInteger counter;
        private final AtomicBoolean shutdownStarted;

        ShutdownEventHandler(
                ApplicationContext applicationContext,
                CamelContext camelContext,
                ApplicationConfiguration config) {

            this.applicationContext = applicationContext;
            this.camelContext = camelContext;
            this.config = config;
            this.counter = new AtomicInteger();
            this.shutdownStarted = new AtomicBoolean();
        }

        @Override
        public void notify(CamelEvent event) throws Exception {
            final int currentCounter = counter.incrementAndGet();
            final int currentInflight = camelContext.getInflightRepository().size();

            LOGGER.debug("CamelEvent received (max: {}, handled: {}, inflight: {})",
                    config.getShutdown().getMaxMessages(),
                    currentCounter,
                    currentInflight);

            if (currentCounter < config.getShutdown().getMaxMessages() || currentInflight != 0) {
                return;
            }

            if (!shutdownStarted.compareAndExchange(false, true)) {
                camelContext.getExecutorServiceManager().newThread("ShutdownStrategy", () -> {

                    try {
                        LOGGER.info("Initiate runtime shutdown (max: {}, handled: {})",
                                config.getShutdown().getMaxMessages(),
                                currentCounter);

                        if (config.getShutdown().getStrategy() == ApplicationConfiguration.ShutdownStrategy.APPLICATION) {
                            SpringApplication.exit(applicationContext, () -> 0);
                        } else {
                            camelContext.shutdown();
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Error while shutting down the runtime", e);
                    }
                }).start();
            }
        }

        @Override
        public boolean isEnabled(CamelEvent event) {
            return (event instanceof CamelEvent.ExchangeCompletedEvent || event instanceof CamelEvent.ExchangeFailedEvent);
        }
    }
}
