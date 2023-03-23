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
package org.apache.camel.component.mapstruct.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedStartupListener;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@EnableConfigurationProperties({ComponentConfigurationProperties.class,MapstructComponentConfiguration.class})
@ConditionalOnHierarchicalProperties({"camel.component", "camel.component.mapstruct"})
@ConditionalOnProperty(prefix = "camel.component.mapstruct", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter({CamelAutoConfiguration.class, MapstructComponentAutoConfiguration.class})
public class MapstructMappingAutoConfiguration {

    @Bean
    CamelContextCustomizer registerMapstructComponent(CamelContext context) throws Exception {
        context.addStartupListener(new ExtendedStartupListener() {
            @Override
            public void onCamelContextStarting(CamelContext context, boolean alreadyStarted) throws Exception {
                // we want mapstruct to be automatically created and initialize via spring boot auto-configuation
                // so we need to force this via camel context, and do this without causing circular dependency creation
                // problems with spring boot
                if (context.hasComponent("mapstruct") == null) {
                    context.getComponent("mapstruct");
                }
            }

            @Override
            public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
                // noop
            }

            @Override
            public void onCamelContextFullyStarted(CamelContext context, boolean alreadyStarted) throws Exception {
                // noop
            }
        });
        return new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                // empty not in use
            }
        };
    }
}