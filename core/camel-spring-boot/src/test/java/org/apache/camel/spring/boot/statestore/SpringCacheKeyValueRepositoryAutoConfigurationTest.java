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
package org.apache.camel.spring.boot.statestore;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.spi.KeyValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringCacheKeyValueRepositoryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SpringCacheKeyValueRepositoryAutoConfiguration.class
            ))
            .withUserConfiguration(DefaultCacheManagerConfig.class);

    @Configuration
    static class DefaultCacheManagerConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("camel-kvr");
        }
    }

    @Test
    @DisplayName("Should auto-configure SpringCacheKeyValueRepository when CacheManager is present")
    void shouldAutoConfigureWhenCacheManagerPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("springCacheKeyValueRepository");
            assertThat(context).hasSingleBean(KeyValueRepository.class);

            KeyValueRepository repo = context.getBean(KeyValueRepository.class);
            assertThat(repo).isInstanceOf(SpringCacheKeyValueRepository.class);

            SpringCacheKeyValueRepository springRepo = (SpringCacheKeyValueRepository) repo;
            assertThat(springRepo.getCacheName()).isEqualTo("camel-kvr");
        });
    }

    @Test
    @DisplayName("Should NOT auto-configure repository when CacheManager is strictly absent")
    void shouldNotAutoConfigureWhenCacheManagerAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpringCacheKeyValueRepositoryAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean("springCacheKeyValueRepository");
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    @DisplayName("Should respect user-defined KeyValueRepository bean precedence")
    void shouldRespectUserDefinedRepositoryPrecedence() {
        contextRunner.withUserConfiguration(CustomRepositoryConfig.class).run(context -> {
            assertThat(context).hasBean("customRepo");
            assertThat(context).hasSingleBean(KeyValueRepository.class);

            KeyValueRepository repo = context.getBean(KeyValueRepository.class);
            assertThat(repo).isInstanceOf(StubKeyValueRepository.class);
            assertThat(repo).isNotInstanceOf(SpringCacheKeyValueRepository.class);
        });
    }

    @Test
    @DisplayName("Should correctly handle full CRUD lifecycle and sequential previous/removed return value semantics")
    void shouldHandleCrudLifecycleAndReturnValues() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        assertThat(repo.contains("key1")).isFalse();
        assertThat(repo.get("key1")).isNull();

        Object previous = repo.put("key1", "val1");
        assertThat(previous).isNull();
        assertThat(repo.contains("key1")).isTrue();
        assertThat(repo.get("key1")).isEqualTo("val1");

        Object updatedPrevious = repo.put("key1", "val2");
        assertThat(updatedPrevious).isEqualTo("val1");
        assertThat(repo.get("key1")).isEqualTo("val2");

        Object deletedValue = repo.delete("key1");
        assertThat(deletedValue).isEqualTo("val2");
        assertThat(repo.contains("key1")).isFalse();

        assertThat(repo.delete("non-existent")).isNull();
    }

    @Test
    @DisplayName("Should handle putIfAbsent correctly")
    void shouldHandlePutIfAbsent() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        Object existing1 = repo.putIfAbsent("key1", "val1");
        assertThat(existing1).isNull();
        assertThat(repo.get("key1")).isEqualTo("val1");

        Object existing2 = repo.putIfAbsent("key1", "newVal");
        assertThat(existing2).isEqualTo("val1");
        assertThat(repo.get("key1")).isEqualTo("val1");
    }

    @Test
    @DisplayName("Should clear all entries in the target cache")
    void shouldClearCache() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        repo.put("key1", "val1");
        repo.put("key2", "val2");
        repo.clear();

        assertThat(repo.contains("key1")).isFalse();
        assertThat(repo.contains("key2")).isFalse();
    }

    @Test
    @DisplayName("Should inherit and execute SPI default conditional operations (replace and delete)")
    void shouldSupportInheritedConditionalOperations() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        repo.put("key1", "val1");

        boolean replaced = repo.replace("key1", "val1", "val2", null);
        assertThat(replaced).isTrue();
        assertThat(repo.get("key1")).isEqualTo("val2");

        boolean deletedMismatch = repo.delete("key1", "wrongVal");
        assertThat(deletedMismatch).isFalse();
        assertThat(repo.contains("key1")).isTrue();

        boolean deletedMatch = repo.delete("key1", "val2");
        assertThat(deletedMatch).isTrue();
        assertThat(repo.contains("key1")).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalStateException when requested named cache is missing in strict CacheManager")
    void shouldThrowExceptionWhenCacheIsMissing() {
        SimpleCacheManager strictCacheManager = new SimpleCacheManager();
        strictCacheManager.setCaches(Collections.singletonList(new ConcurrentMapCache("other-cache")));
        strictCacheManager.initializeCaches();

        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(strictCacheManager);

        assertThatThrownBy(() -> repo.get("key1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Spring Cache 'camel-kvr' not found in CacheManager");
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for non-portable keys() and size()")
    void shouldThrowUnsupportedForKeysAndSize() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        KeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        assertThatThrownBy(repo::keys)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("portable key enumeration API");

        assertThatThrownBy(repo::size)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("portable size API");
    }

    @Test
    @DisplayName("Should execute TTL-bearing operations successfully without throwing errors")
    void shouldExecuteTtlOperationsSuccessfully() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager);

        repo.put("k1", "v1", Duration.ofMinutes(5));
        repo.putIfAbsent("k2", "v2", Duration.ofMinutes(10));
        repo.put("k1", "v3", Duration.ofMinutes(2));

        assertThat(repo.get("k1")).isEqualTo("v3");
        assertThat(repo.get("k2")).isEqualTo("v2");
    }

    @Test
    @DisplayName("Should maintain complete multi-store isolation between separate repository instances")
    void shouldMaintainMultiStoreIsolation() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders", "inventory");

        SpringCacheKeyValueRepository ordersRepo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        SpringCacheKeyValueRepository inventoryRepo = new SpringCacheKeyValueRepository(cacheManager, "inventory");

        ordersRepo.put("item123", "ORDER_PLACED");
        inventoryRepo.put("item123", "IN_STOCK");

        assertThat(ordersRepo.get("item123")).isEqualTo("ORDER_PLACED");
        assertThat(inventoryRepo.get("item123")).isEqualTo("IN_STOCK");

        ordersRepo.clear();

        assertThat(ordersRepo.contains("item123")).isFalse();
        assertThat(inventoryRepo.contains("item123")).isTrue();
    }

    @Test
    @DisplayName("Should support custom storeName routing mapped 1:1 to Spring Cache names")
    void shouldSupportCustomStoreNameMapping() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("myStore");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "myStore");

        repo.put("myKey", "myVal");
        assertThat(repo.get("myKey")).isEqualTo("myVal");
        assertThat(repo.getCacheName()).isEqualTo("myStore");
    }

    @Test
    @DisplayName("Should auto-configure SpringCacheKeyValueRepository with Caffeine-backed or custom multi-cache Manager")
    void shouldAutoConfigureWithCustomCacheNamesManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SpringCacheKeyValueRepositoryAutoConfiguration.class
                ))
                .withUserConfiguration(CaffeineCacheConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("springCacheKeyValueRepository");
                    assertThat(context).hasSingleBean(KeyValueRepository.class);

                    KeyValueRepository repo = context.getBean(KeyValueRepository.class);
                    assertThat(repo).isInstanceOf(SpringCacheKeyValueRepository.class);

                    repo.put("caffeineKey", "caffeineVal");
                    assertThat(repo.get("caffeineKey")).isEqualTo("caffeineVal");
                    assertThat(repo.contains("caffeineKey")).isTrue();

                    Object prev = repo.put("caffeineKey", "newCaffeineVal");
                    assertThat(prev).isEqualTo("caffeineVal");
                    assertThat(repo.get("caffeineKey")).isEqualTo("newCaffeineVal");

                    repo.delete("caffeineKey");
                    assertThat(repo.contains("caffeineKey")).isFalse();
                });
    }

    @Configuration
    static class CaffeineCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("camel-kvr", "myStore");
        }
    }

    static class StubKeyValueRepository implements KeyValueRepository {
        private final Map<String, Object> store = new HashMap<>();

        @Override public void start() { }
        @Override public void stop() { }
        @Override public Object get(String key) { return store.get(key); }
        @Override public Object put(String key, Object value) { return store.put(key, value); }
        @Override public Object put(String key, Object value, Duration ttl) { return store.put(key, value); }
        @Override public Object putIfAbsent(String key, Object value) { return store.putIfAbsent(key, value); }
        @Override public Object putIfAbsent(String key, Object value, Duration ttl) { return store.putIfAbsent(key, value); }
        @Override public Object delete(String key) { return store.remove(key); }
        @Override public boolean contains(String key) { return store.containsKey(key); }
        @Override public void clear() { store.clear(); }
        @Override public Set<String> keys() { return store.keySet(); }
        @Override public int size() { return store.size(); }
    }

    @Configuration
    static class CustomRepositoryConfig {
        @Bean
        public KeyValueRepository customRepo() {
            return new StubKeyValueRepository();
        }
    }
}
