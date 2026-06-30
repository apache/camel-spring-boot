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
package org.apache.camel.spring.boot.errorregistry;

import java.time.Duration;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ErrorRegistry;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = CamelAutoConfiguration.class)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelErrorRegistryConfigurationProperties.class)
public class CamelErrorRegistryAutoConfiguration {

    @Bean
    public ErrorRegistry errorRegistry(CamelContext camelContext, CamelErrorRegistryConfigurationProperties config) {
        // dev profile enables error registry to capture routing errors for tooling (TUI)
        if (!config.isEnabled() && "dev".equals(camelContext.getCamelContextExtension().getProfile())) {
            config.setEnabled(true);
        }
        if (!config.isEnabled()) {
            return null;
        }

        ErrorRegistry errorRegistry = camelContext.getErrorRegistry();
        errorRegistry.setEnabled(true);
        errorRegistry.setMaximumEntries(config.getMaximumEntries());
        errorRegistry.setTimeToLive(Duration.ofSeconds(config.getTimeToLiveSeconds()));
        errorRegistry.setBodyMaxChars(config.getBodyMaxChars());
        errorRegistry.setBodyIncludeStreams(config.isBodyIncludeStreams());
        errorRegistry.setBodyIncludeFiles(config.isBodyIncludeFiles());
        errorRegistry.setIncludeExchangeProperties(config.isIncludeExchangeProperties());
        errorRegistry.setIncludeExchangeVariables(config.isIncludeExchangeVariables());

        return errorRegistry;
    }

}
