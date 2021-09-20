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

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.ehcache.EhcacheComponent;
import org.apache.camel.component.ehcache.springboot.EhcacheComponentAutoConfiguration;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@DirtiesContext
@EnableAutoConfiguration
@SpringBootTest(
    classes = {
        CacheManagerCustomizerOverrideTest.TestConfiguration.class
    },
    properties = {
        "debug=false",
        "camel.component.ehcache.customizer.cache-manager.enabled=true",
        "camel.component.ehcache.customizer.cache-manager.override=true"
    })
public class CacheManagerCustomizerOverrideTest {
    private static final CacheManager CACHE_MANAGER = CacheManagerBuilder.newCacheManagerBuilder().build();

    @Autowired
    CacheManager cacheManager;
    @Autowired
    CamelContext context;

    @Test
    public void testComponentConfiguration() throws Exception {
        EhcacheComponent component = context.getComponent("ehcache", EhcacheComponent.class);

        Assertions.assertNotNull(cacheManager);
        Assertions.assertNotNull(component);
        Assertions.assertNotNull(component.getCacheManager());
        Assertions.assertSame(cacheManager, component.getCacheManager());
    }

    @Configuration
    @AutoConfigureAfter(CamelAutoConfiguration.class)
    @AutoConfigureBefore(EhcacheComponentAutoConfiguration.class)
    public static class TestConfiguration {
        @Bean
        public ComponentCustomizer customizer() {
            return ComponentCustomizer.builder(EhcacheComponent.class)
                .withOrder(Ordered.HIGHEST)
                .build(component -> component.setCacheManager(CACHE_MANAGER));
        }

        @Bean(initMethod = "init", destroyMethod = "close")
        public CacheManager cacheManager() {
            return CacheManagerBuilder.newCacheManagerBuilder().build();
        }
    }
}