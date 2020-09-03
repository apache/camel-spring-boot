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
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
    classes = {
        CacheManagerCustomizerNoOverrideTest.TestConfiguration.class
    },
    properties = {
        "debug=false",
        "camel.component.ehcache.customizer.cache-manager.enabled=true",
        "camel.component.ehcache.customizer.cache-manager.override=false"
    })
public class CacheManagerCustomizerNoOverrideTest {
    private static final CacheManager CACHE_MANAGER = CacheManagerBuilder.newCacheManagerBuilder().build();
    @Autowired
    CacheManager cacheManager;
    @Autowired
    CamelContext context;

    @Test
    public void testComponentConfiguration() {
        EhcacheComponent component = context.getComponent("ehcache", EhcacheComponent.class);

        Assert.assertNotNull(cacheManager);
        Assert.assertNotNull(component);
        Assert.assertNotNull(component.getCacheManager());
        Assert.assertSame(CACHE_MANAGER, component.getCacheManager());
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