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
package org.apache.camel.spring.boot.actuate.accesslog;

import org.apache.catalina.valves.AccessLogValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;

/**
 * Management context configuration for controlling access logging on the management server.
 * <p>
 * This configuration is only loaded in the management context (when management runs on a separate port) and provides
 * customizers to disable access logging for actuator endpoints while keeping access logs for the main application.
 * </p>
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ManagementAccessLogProperties.class)
public class ManagementAccessLogConfiguration {

    /**
     * Undertow-specific configuration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.undertow.Undertow")
    static class UndertowAccessLogCustomizerConfiguration {

        /**
         * Disable access logging in the management context.
         */
        @Bean
        @ConditionalOnProperty(name = "management.server.accesslog.enabled", havingValue = "false")
        WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowManagementAccessLogCustomizer() {
            return factory -> factory.setAccessLogEnabled(false);
        }

        /**
         * Undertow HTTP access log is managed by whatever camel logging mechanism.
         */
        @Bean
        @ConditionalOnProperty(name = "management.server.undertow.accesslog.use-camel-logging", havingValue = "true")
        public WebServerFactoryCustomizer<UndertowServletWebServerFactory> managementAccessLogProvider(
                @Value("${management.server.accesslog.pattern:common}") String pattern) {
            return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                deploymentInfo.addInitialHandlerChainWrapper(handler -> new AccessLogHandler(handler,
                        new JBossLoggingAccessLogReceiver(), pattern, AccessLogHandler.class.getClassLoader()));
            });
        }
    }

    /**
     * Tomcat-specific configuration to disable access logging in the management context.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    @ConditionalOnProperty(name = "management.server.accesslog.enabled", havingValue = "false")
    static class TomcatAccessLogCustomizerConfiguration {

        @Bean
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatManagementAccessLogCustomizer() {
            return factory -> factory.getEngineValves().removeIf(valve -> valve instanceof AccessLogValve);
        }
    }

}
