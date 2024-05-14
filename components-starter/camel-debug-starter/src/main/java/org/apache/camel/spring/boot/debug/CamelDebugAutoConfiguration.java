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
package org.apache.camel.spring.boot.debug;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.debugger.DebuggerJmxConnectorService;
import org.apache.camel.impl.debugger.DefaultBacklogDebugger;
import org.apache.camel.spi.BacklogDebugger;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelSpringBootApplicationListener;
import org.apache.camel.support.LifecycleStrategySupport;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelDebugConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
@AutoConfigureBefore(CamelSpringBootApplicationListener.class)
public class CamelDebugAutoConfiguration {

    @Bean
    public BacklogDebugger backlogDebugger(CamelContext camelContext, CamelDebugConfigurationProperties config)
            throws Exception {

        // enable debugger on camel
        camelContext.setDebugging(config.isEnabled());
        camelContext.setDebugStandby(config.isStandby());

        if (config.isEnabled() || config.isStandby()) {
            // must enable source location and history
            // so debugger tooling knows to map breakpoints to source code
            camelContext.setSourceLocationEnabled(true);
            camelContext.setMessageHistory(true);
        }

        BacklogDebugger debugger = DefaultBacklogDebugger.createDebugger(camelContext);
        debugger.setStandby(config.isStandby());
        debugger.setInitialBreakpoints(config.getBreakpoints());
        debugger.setSingleStepIncludeStartEnd(config.isSingleStepIncludeStartEnd());
        debugger.setBodyMaxChars(config.getBodyMaxChars());
        debugger.setBodyIncludeStreams(config.isBodyIncludeStreams());
        debugger.setBodyIncludeFiles(config.isBodyIncludeFiles());
        debugger.setIncludeExchangeProperties(config.isIncludeExchangeProperties());
        debugger.setIncludeExchangeVariables(config.isIncludeExchangeVariables());
        debugger.setIncludeException(config.isIncludeException());
        debugger.setLoggingLevel(config.getLoggingLevel().name());
        debugger.setSuspendMode(config.isWaitForAttach());
        debugger.setFallbackTimeout(config.getFallbackTimeout());

        // enable jmx connector if port is set
        if (config.isJmxConnectorEnabled()) {
            DebuggerJmxConnectorService connector = new DebuggerJmxConnectorService();
            connector.setCreateConnector(true);
            connector.setRegistryPort(config.getJmxConnectorPort());
            camelContext.addService(connector);
        }

        // start debugger after context is started
        camelContext.addLifecycleStrategy(new LifecycleStrategySupport() {
            @Override
            public void onContextStarted(CamelContext context) {
                // only enable debugger if not in standby mode
                if (config.isEnabled() && !debugger.isStandby()) {
                    debugger.enableDebugger();
                }
            }

            @Override
            public void onContextStopping(CamelContext context) {
                if (debugger.isEnabled()) {
                    debugger.disableDebugger();
                }
            }
        });

        camelContext.addService(debugger);

        return debugger;
    }

}
