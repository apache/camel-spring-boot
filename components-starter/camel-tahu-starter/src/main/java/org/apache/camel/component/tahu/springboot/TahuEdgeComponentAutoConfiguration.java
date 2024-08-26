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
package org.apache.camel.component.tahu.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.component.tahu.TahuEdgeComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.apache.camel.spring.boot.util.CamelPropertiesHelper;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties;
import org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Configuration(proxyBeanMethods = false)
@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@EnableConfigurationProperties({ComponentConfigurationProperties.class,TahuEdgeComponentConfiguration.class})
@ConditionalOnHierarchicalProperties({"camel.component", "camel.component.tahu-edge"})
@AutoConfigureAfter({CamelAutoConfiguration.class, TahuEdgeComponentConverter.class})
public class TahuEdgeComponentAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;
    private final CamelContext camelContext;
    @Autowired
    private TahuEdgeComponentConfiguration configuration;

    public TahuEdgeComponentAutoConfiguration(
            org.apache.camel.CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Lazy
    @Bean
    public ComponentCustomizer configureTahuEdgeComponent() {
        return new ComponentCustomizer() {
            @Override
            public void configure(String name, Component target) {
                CamelPropertiesHelper.copyProperties(camelContext, configuration, target);
            }
            @Override
            public boolean isEnabled(String name, Component target) {
                return HierarchicalPropertiesEvaluator.evaluate(
                        applicationContext,
                        "camel.component.customizer",
                        "camel.component.tahu-edge.customizer")
                    && target instanceof TahuEdgeComponent;
            }
        };
    }
}