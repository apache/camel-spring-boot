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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = { "org.apache.camel.component.servlet.springboot.PlatformHttpComponentAutoConfiguration",
        "org.apache.camel.component.servlet.springboot.PlatformHttpComponentConverter" })
public class SpringBootPlatformHttpAutoConfiguration {

    @Autowired
    CamelContext camelContext;

    @Autowired
    List<Executor> executors;

    @Bean(name = "platform-http-engine")
    @ConditionalOnMissingBean(PlatformHttpEngine.class)
    public PlatformHttpEngine springBootPlatformHttpEngine(Environment env) {
        Executor executor;

        if (executors != null && !executors.isEmpty()) {
            executor = executors.stream()
                    .filter(e -> e instanceof ThreadPoolTaskExecutor || e instanceof SimpleAsyncTaskExecutor)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No ThreadPoolTaskExecutor or SimpleAsyncTaskExecutor configured"));
        } else {
            throw new RuntimeException("No Executor configured");
        }
        int port = Integer.parseInt(env.getProperty("server.port", "8080"));
        return new SpringBootPlatformHttpEngine(port, executor);
    }

    @Bean
    @DependsOn("configurePlatformHttpComponent")
    public CamelRequestHandlerMapping platformHttpEngineRequestMapping(PlatformHttpEngine engine) {
        PlatformHttpComponent component = camelContext.getComponent("platform-http", PlatformHttpComponent.class);
        CamelRequestHandlerMapping answer = new CamelRequestHandlerMapping(component, engine);
        return answer;
    }

}
