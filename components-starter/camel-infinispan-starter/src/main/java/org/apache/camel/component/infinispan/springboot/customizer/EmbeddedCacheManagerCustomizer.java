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
package org.apache.camel.component.infinispan.springboot.customizer;

import org.apache.camel.Component;
import org.apache.camel.component.infinispan.InfinispanComponent;
import org.apache.camel.component.infinispan.springboot.InfinispanComponentAutoConfiguration;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.spi.HasId;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean({ EmbeddedCacheManager.class, CamelAutoConfiguration.class })
@AutoConfigureAfter({ CamelAutoConfiguration.class, CacheAutoConfiguration.class })
@EnableConfigurationProperties(EmbeddedCacheManagerCustomizerConfiguration.class)
public class EmbeddedCacheManagerCustomizer implements HasId, ComponentCustomizer {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private EmbeddedCacheManager cacheManager;
    @Autowired
    private EmbeddedCacheManagerCustomizerConfiguration configuration;

    @Override
    public void configure(String name, Component target) {
        InfinispanComponent component = (InfinispanComponent)target;

        // Set the cache manager only if the customizer is configured to always
        // set it or if no cache manager is already configured on component
        if (configuration.isOverride() || component.getConfiguration().getCacheContainer() == null) {
            component.getConfiguration().setCacheContainer(cacheManager);
        }
    }

    @Override
    public boolean isEnabled(String name, Component target) {
        return HierarchicalPropertiesEvaluator.evaluate(
            applicationContext,
            "camel.component.customizer",
            "camel.component.infinispan.customizer",
            getId())
            && target instanceof InfinispanComponent;
    }

    @Override
    public int getOrder() {
        return 101;
    }

    @Override
    public String getId() {
        return "camel.component.infinispan.customizer.embedded-cache-manager";
    }
}
