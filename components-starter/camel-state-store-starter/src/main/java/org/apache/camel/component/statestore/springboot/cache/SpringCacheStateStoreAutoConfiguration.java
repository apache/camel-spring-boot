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
package org.apache.camel.component.statestore.springboot.cache;

import org.apache.camel.component.statestore.StateStoreBackendFactory;
import org.apache.camel.spi.KeyValueRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Opt-in auto-configuration that registers a {@link StateStoreBackendFactory}
 * backed by Spring Cache. Disabled unless
 * {@code camel.state-store.spring-cache.enabled=true}. The factory creates one
 * {@link SpringCacheKeyValueRepository} per state-store name and does not
 * register a global {@link KeyValueRepository} bean.
 */
@AutoConfiguration(after = CamelAutoConfiguration.class,
        afterName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnClass({ CacheManager.class, StateStoreBackendFactory.class, KeyValueRepository.class })
@ConditionalOnProperty(prefix = "camel.state-store.spring-cache", name = "enabled", havingValue = "true")
public class SpringCacheStateStoreAutoConfiguration {

    @Bean
    @ConditionalOnSingleCandidate(CacheManager.class)
    @ConditionalOnMissingBean(StateStoreBackendFactory.class)
    public StateStoreBackendFactory springCacheStateStoreBackendFactory(CacheManager cacheManager) {
        return storeName -> new SpringCacheKeyValueRepository(cacheManager, storeName);
    }
}
