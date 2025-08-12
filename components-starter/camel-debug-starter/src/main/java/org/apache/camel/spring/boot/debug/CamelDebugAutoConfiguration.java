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

import org.apache.camel.main.DebuggerConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "camel.debug.enabled", matchIfMissing = true)
@AutoConfigureBefore(name = "org.apache.camel.spring.boot.CamelAutoConfiguration") // configure early to have Camel debugger during startup
@EnableConfigurationProperties({ CamelDebugConfigurationProperties.class })
public class CamelDebugAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DebuggerConfigurationProperties.class)
    public DebuggerConfigurationProperties backlogDebugger(CamelDebugConfigurationProperties config)
            throws Exception {

        DebuggerConfigurationProperties answer = new DebuggerConfigurationProperties(null);
        answer.setBodyIncludeFiles(config.isBodyIncludeFiles());
        answer.setBodyIncludeStreams(config.isBodyIncludeStreams());
        answer.setBodyMaxChars(config.getBodyMaxChars());
        answer.setBreakpoints(config.getBreakpoints());
        answer.setEnabled(config.isEnabled());
        answer.setFallbackTimeout(config.getFallbackTimeout());
        answer.setIncludeException(config.isIncludeException());
        answer.setIncludeExchangeProperties(config.isIncludeExchangeProperties());
        answer.setIncludeExchangeVariables(config.isIncludeExchangeProperties());
        answer.setJmxConnectorEnabled(config.isJmxConnectorEnabled());
        answer.setJmxConnectorPort(config.getJmxConnectorPort());
        answer.setLoggingLevel(config.getLoggingLevel());
        answer.setSingleStepIncludeStartEnd(config.isSingleStepIncludeStartEnd());
        answer.setStandby(config.isStandby());
        answer.setWaitForAttach(config.isWaitForAttach());

        return answer;
    }

}
