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
package org.apache.camel.component.platform.http.springboot.customizer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;

/**
 * Undertow specific configuration to use camel logging for HTTP access log.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UndertowAccessLogProperties.class)
@ConditionalOnClass(name = "io.undertow.Undertow")
@ConditionalOnProperties( {
    @ConditionalOnProperty(name = "server.undertow.accesslog.enabled", havingValue = "false"),
    @ConditionalOnProperty(name = "camel.component.platform-http.server.undertow.accesslog.use-camel-logging", havingValue = "true"),
})
public class UndertowAccessLogConfiguration {

    @Bean
    WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowServerAccessLogCustomizer(Environment env) {
        return new WebServerFactoryCustomizer<UndertowServletWebServerFactory>() {
            @Override
            public void customize(UndertowServletWebServerFactory factory) {
                factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                    deploymentInfo.addInitialHandlerChainWrapper(handler -> {
                        JBossLoggingAccessLogReceiver jbossLogReceiver = new JBossLoggingAccessLogReceiver();
                        // undertow specific HTTP log message pattern
                        // https://github.com/undertow-io/undertow/blob/2.3.22.Final/core/src/main/java/io/undertow/server/handlers/accesslog/AccessLogHandler.java
                        String pattern = env.getProperty("server.undertow.accesslog.pattern", "common");
                        return new AccessLogHandler(handler, jbossLogReceiver, pattern, getClass().getClassLoader());
                    });
                });
            }
        };
    }
}