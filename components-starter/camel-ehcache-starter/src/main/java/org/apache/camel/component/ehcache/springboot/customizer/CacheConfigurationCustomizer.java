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
package org.apache.camel.component.ehcache.springboot.customizer;

import java.util.Map;

import org.apache.camel.Component;
import org.apache.camel.Ordered;
import org.apache.camel.component.ehcache.EhcacheComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.spi.HasId;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties;
import org.ehcache.config.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A simple implementation of {@link ComponentCustomizer} that auto discovers a
 * {@link org.ehcache.config.CacheConfiguration} instance and bind it to the
 * {@link EhcacheComponent} component.
 *
 * This customizer can be disabled/enabled with different strategies:
 *
 * 1. globally using:
 *    camel.component.customizer.enable = true/false
 * 2. for component:
 *    camel.component.ehcache.customizer.enabled = true/false
 * 3. individually:
 *    camel.component.ehcache.customizer.cache-configuration.enabled = true/false
 */
@Configuration
@ConditionalOnBean(CamelAutoConfiguration.class)
@ConditionalOnHierarchicalProperties({
    "camel.component.customizer",
    "camel.component.ehcache.customizer",
    "camel.component.ehcache.customizer.cache-configuration"})
@AutoConfigureAfter(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CacheConfigurationCustomizerConfiguration.class)
public class CacheConfigurationCustomizer implements HasId, ComponentCustomizer {
    @Autowired(required = false)
    private Map<String, CacheConfiguration> configurations;
    @Autowired
    private CacheConfigurationCustomizerConfiguration configuration;

    @Override
    public void configure(String name, Component target) {
        EhcacheComponent component = (EhcacheComponent)target;

        if (configurations != null && configuration.getMode() == CacheConfigurationCustomizerConfiguration.Mode.APPEND) {
            component.addCachesConfigurations(configurations);
        }
        if (configurations != null && configuration.getMode() == CacheConfigurationCustomizerConfiguration.Mode.REPLACE) {
            component.setCachesConfigurations(configurations);
        }
    }

    @Override
    public boolean isEnabled(String name, Component target) {
        return target instanceof EhcacheComponent;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST;
    }

    @Override
    public String getId() {
        return "camel.component.ehcache.customizer.cache-configuration";
    }
}
