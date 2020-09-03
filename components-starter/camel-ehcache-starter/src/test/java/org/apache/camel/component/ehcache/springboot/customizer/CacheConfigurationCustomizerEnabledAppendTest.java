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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.ehcache.EhcacheComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
        CacheConfigurationCustomizerEnabledAppendTest.TestConfiguration.class
    },
    properties = {
        "debug=false",
        "camel.component.customizer.enabled=false",
        "camel.component.ehcache.customizer.enabled=true",
        "camel.component.ehcache.customizer.cache-configuration.enabled=true",
        "camel.component.ehcache.customizer.cache-configuration.mode=APPEND"
    })
public class CacheConfigurationCustomizerEnabledAppendTest {
    private static final String CACHE_CONFIG_ID = UUID.randomUUID().toString();

    @Autowired
    Map<String, CacheConfiguration<?, ?>> configurations;
    @Autowired
    CamelContext context;

    @Test
    public void testComponentConfiguration() {
        EhcacheComponent component = context.getComponent("ehcache", EhcacheComponent.class);

        Assert.assertNotNull(configurations);
        Assert.assertEquals(2, configurations.size());
        Assert.assertNotNull(component);
        Assert.assertNotNull(component.getCachesConfigurations());
        Assert.assertEquals(3, component.getCachesConfigurations().size());
        Assert.assertTrue(component.getCachesConfigurations().containsKey("myConfig1"));
        Assert.assertTrue(component.getCachesConfigurations().containsKey("myConfig2"));
        Assert.assertTrue(component.getCachesConfigurations().containsKey(CACHE_CONFIG_ID));
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        public ComponentCustomizer customizer() {
            return ComponentCustomizer.builder(EhcacheComponent.class)
                .withOrder(Ordered.HIGHEST)
                .build(component -> {
                    component.addCachesConfigurations(Collections.singletonMap(
                        CACHE_CONFIG_ID,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                            String.class,
                            String.class,
                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(2100, EntryUnit.ENTRIES)
                                .offheap(2, MemoryUnit.MB))
                            .build()
                    ));
                });
        }

        @Bean
        public CacheConfiguration<?, ?> myConfig1() {
            return CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(100, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB))
                .build();
        }

        @Bean
        public CacheConfiguration<?, ?> myConfig2() {
            return CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(2100, EntryUnit.ENTRIES)
                    .offheap(2, MemoryUnit.MB))
                .build();
        }
    }
}