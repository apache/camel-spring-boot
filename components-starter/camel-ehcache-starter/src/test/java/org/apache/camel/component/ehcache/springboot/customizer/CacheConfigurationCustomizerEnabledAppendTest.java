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
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

        Assertions.assertNotNull(configurations);
        Assertions.assertEquals(2, configurations.size());
        Assertions.assertNotNull(component);
        Assertions.assertNotNull(component.getCachesConfigurations());
        Assertions.assertEquals(3, component.getCachesConfigurations().size());
        Assertions.assertTrue(component.getCachesConfigurations().containsKey("myConfig1"));
        Assertions.assertTrue(component.getCachesConfigurations().containsKey("myConfig2"));
        Assertions.assertTrue(component.getCachesConfigurations().containsKey(CACHE_CONFIG_ID));
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