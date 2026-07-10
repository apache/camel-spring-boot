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
package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

@AutoConfiguration(after = { PlatformHttpComponentAutoConfiguration.class, PlatformHttpComponentConverter.class })
@EnableConfigurationProperties({ ComponentConfigurationProperties.class, PlatformHttpComponentConfiguration.class })
public class SpringBootPlatformHttpAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootPlatformHttpAutoConfiguration.class);

    @Bean(name = "platform-http-engine")
    @ConditionalOnMissingBean(PlatformHttpEngine.class)
    public PlatformHttpEngine springBootPlatformHttpEngine(Environment env, ServerProperties serverProperties,
                                                           List<Executor> executors) {
        if (executors == null || executors.isEmpty()) {
            throw new IllegalStateException("No Executor configured");
        }
        executors.forEach(e -> LOG.debug("Analyzing executor: {}", e.getClass().getName()));

        boolean virtualThreadsEnabled = Threading.VIRTUAL.isActive(env);

        Executor executor = executors.stream()
                .filter(e -> {
                    try {
                        return Class.forName("org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor").isInstance(e);
                    } catch (ClassNotFoundException ex) {
                        // No problem, spring-security is not configured
                        return false;
                    }
                }).findAny().orElseGet(() -> {
                    if (virtualThreadsEnabled) {
                        // Prefer SimpleAsyncTaskExecutor when virtual threads are enabled
                        return executors.stream()
                                .filter(e -> e instanceof SimpleAsyncTaskExecutor)
                                .findFirst()
                                .orElseGet(() ->
                                        executors.stream()
                                                .filter(e -> e instanceof ThreadPoolTaskExecutor)
                                                .findFirst()
                                                .orElseThrow(() -> new IllegalStateException("No SimpleAsyncTaskExecutor or ThreadPoolTaskExecutor configured"))
                                );
                    } else {
                        // Traditional behavior: prefer ThreadPoolTaskExecutor
                        return executors.stream()
                                .filter(e -> e instanceof ThreadPoolTaskExecutor || e instanceof SimpleAsyncTaskExecutor)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No ThreadPoolTaskExecutor, SimpleAsyncTaskExecutor or DelegatingSecurityContextAsyncTaskExecutor configured"));
                    }
                });

        if (virtualThreadsEnabled) {
            LOG.info("Virtual threads enabled - using executor: {} for platform-http", executor.getClass().getName());
        } else {
            LOG.debug("Using executor: {}", executor.getClass().getName());
        }
        int port = serverProperties.getPort() != null ? serverProperties.getPort() : 8080;
        return new SpringBootPlatformHttpEngine(port, executor);
    }

    @Bean
    @Lazy
    public CamelRequestHandlerMapping platformHttpEngineRequestMapping(PlatformHttpEngine engine, CamelContext camelContext) {
        PlatformHttpComponent component = camelContext.getComponent("platform-http", PlatformHttpComponent.class);
        return new CamelRequestHandlerMapping(component, engine);
    }

}
