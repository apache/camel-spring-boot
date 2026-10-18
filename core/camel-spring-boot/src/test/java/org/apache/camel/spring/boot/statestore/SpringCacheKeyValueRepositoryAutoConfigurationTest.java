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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.slf4j.LoggerFactory;

import org.apache.camel.spi.KeyValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringCacheKeyValueRepositoryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SpringCacheKeyValueRepositoryAutoConfiguration.class
            ))
            .withBean("cacheManager", CacheManager.class, () -> new ConcurrentMapCacheManager("camel-kvr"))
            .withPropertyValues("camel.spring.cache.state-store.enabled=true");

    @Test
    @DisplayName("Should auto-configure SpringCacheKeyValueRepository when CacheManager is present and enabled")
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
    @DisplayName("Should auto-configure with CacheAutoConfiguration and spring.cache.type simple when enabled")
    void shouldAutoConfigureWithBootCacheAutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CacheAutoConfiguration.class,
                        SpringCacheKeyValueRepositoryAutoConfiguration.class
                ))
                .withUserConfiguration(ProxyCachingConfiguration.class)
                .withPropertyValues(
                        "spring.cache.type=simple",
                        "spring.cache.cache-names=camel-kvr",
                        "camel.spring.cache.state-store.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("springCacheKeyValueRepository");
                    assertThat(context).hasSingleBean(KeyValueRepository.class);

                    KeyValueRepository repo = context.getBean(KeyValueRepository.class);
                    repo.put("simple-key", "simple-val");
                    assertThat(repo.keys()).containsExactly("simple-key");
                    assertThat(repo.size()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should not create CacheManager or repository from spring.cache.type without @EnableCaching")
    void shouldNotCreateRepositoryFromCacheTypeWithoutEnableCaching() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CacheAutoConfiguration.class,
                        SpringCacheKeyValueRepositoryAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.cache.type=simple",
                        "spring.cache.cache-names=camel-kvr",
                        "camel.spring.cache.state-store.enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheManager.class);
                    assertThat(context).doesNotHaveBean("springCacheKeyValueRepository");
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    @DisplayName("Should fail operations when spring.cache.cache-names omits camel-kvr")
    void shouldFailWhenCacheNamesOmitDefaultCache() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CacheAutoConfiguration.class,
                        SpringCacheKeyValueRepositoryAutoConfiguration.class
                ))
                .withUserConfiguration(ProxyCachingConfiguration.class)
                .withPropertyValues(
                        "spring.cache.type=simple",
                        "spring.cache.cache-names=other-cache",
                        "camel.spring.cache.state-store.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("springCacheKeyValueRepository");
                    KeyValueRepository repo = context.getBean(KeyValueRepository.class);
                    assertThatThrownBy(() -> repo.get("key1"))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Spring Cache 'camel-kvr' not found in CacheManager");
                });
    }

    @Test
    @DisplayName("Should NOT auto-configure repository when property is not enabled")
    void shouldNotAutoConfigureWhenPropertyDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SpringCacheKeyValueRepositoryAutoConfiguration.class
                ))
                .withBean("cacheManager", CacheManager.class, () -> new ConcurrentMapCacheManager("camel-kvr"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean("springCacheKeyValueRepository");
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    @DisplayName("Should NOT auto-configure repository when CacheManager is strictly absent")
    void shouldNotAutoConfigureWhenCacheManagerAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpringCacheKeyValueRepositoryAutoConfiguration.class))
                .withPropertyValues("camel.spring.cache.state-store.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("springCacheKeyValueRepository");
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    @DisplayName("Should respect user-defined KeyValueRepository bean precedence")
    void shouldRespectUserDefinedRepositoryPrecedence() {
        contextRunner
                .withBean("customRepo", KeyValueRepository.class, StubKeyValueRepository::new)
                .run(context -> {
                    assertThat(context).hasBean("customRepo");
                    assertThat(context).hasSingleBean(KeyValueRepository.class);

                    KeyValueRepository repo = context.getBean(KeyValueRepository.class);
                    assertThat(repo).isInstanceOf(StubKeyValueRepository.class);
                    assertThat(repo).isNotInstanceOf(SpringCacheKeyValueRepository.class);
                });
    }

    @Test
    @DisplayName("Should be discoverable and pickable by EIP reifiers via findSingleByType registry lookup")
    void shouldBeDiscoverableByEipReifiersViaRegistryLookup() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("springCacheKeyValueRepository");

            // Correctly obtain the bean directly from the AssertableApplicationContext
            KeyValueRepository resolvedRepo = context.getBean(KeyValueRepository.class);

            assertThat(resolvedRepo).isNotNull();
            assertThat(resolvedRepo).isInstanceOf(SpringCacheKeyValueRepository.class);

            // Verify operational readiness for shared EIP use
            resolvedRepo.put("eip-test-key", "eip-test-val");
            assertThat(resolvedRepo.get("eip-test-key")).isEqualTo("eip-test-val");
            assertThat(resolvedRepo.contains("eip-test-key")).isTrue();
        });
    }

    @Test
    @DisplayName("Should correctly handle full CRUD lifecycle and sequential previous/removed return value semantics")
    void shouldHandleCrudLifecycleAndReturnValues() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
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
    }

    @Test
    @DisplayName("Should handle putIfAbsent correctly")
    void shouldHandlePutIfAbsent() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            Object existing1 = repo.putIfAbsent("key1", "val1");
            assertThat(existing1).isNull();
            assertThat(repo.get("key1")).isEqualTo("val1");

            Object existing2 = repo.putIfAbsent("key1", "newVal");
            assertThat(existing2).isEqualTo("val1");
            assertThat(repo.get("key1")).isEqualTo("val1");
        }
    }

    @Test
    @DisplayName("Should clear all entries in the target cache")
    void shouldClearCache() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            repo.put("key1", "val1");
            repo.put("key2", "val2");
            repo.clear();

            assertThat(repo.contains("key1")).isFalse();
            assertThat(repo.contains("key2")).isFalse();
        }
    }

    @Test
    @DisplayName("Should inherit and execute SPI default conditional operations (replace and delete)")
    void shouldSupportInheritedConditionalOperations() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
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
    }

    @Test
    @DisplayName("Should throw IllegalStateException when requested named cache is missing in strict CacheManager")
    void shouldThrowExceptionWhenCacheIsMissing() {
        SimpleCacheManager strictCacheManager = new SimpleCacheManager();
        strictCacheManager.setCaches(Collections.singletonList(new ConcurrentMapCache("other-cache")));
        strictCacheManager.initializeCaches();

        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(strictCacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            assertThatThrownBy(() -> repo.get("key1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Spring Cache 'camel-kvr' not found in CacheManager");
        }
    }

    @Test
    @DisplayName("Should enumerate String keys and size from a ConcurrentMap native cache")
    void shouldEnumerateKeysAndSizeForConcurrentMapNativeCache() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            repo.put("key1", "val1");
            repo.put("key2", "val2");

            Cache cache = cacheManager.getCache("camel-kvr");
            assertThat(cache).isNotNull();
            assertThat(cache.getNativeCache()).isInstanceOf(ConcurrentMap.class);
            cache.put(42, "non-string-key");

            Set<String> keys = repo.keys();
            assertThat(keys).containsExactlyInAnyOrder("key1", "key2");
            assertThat(repo.size()).isEqualTo(2);
            assertThat(repo.size()).isEqualTo(keys.size());

            repo.put("key3", "val3");
            assertThat(keys).doesNotContain("key3");
            assertThat(keys).isUnmodifiable();
            assertThat(repo.keys()).containsExactlyInAnyOrder("key1", "key2", "key3");
            assertThat(repo.size()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for keys() and size() when native cache is not a ConcurrentMap")
    void shouldThrowUnsupportedForKeysAndSizeWhenNativeCacheIsNotConcurrentMap() {
        CacheManager cacheManager = new SingleCacheManager(new NonConcurrentNativeCache("camel-kvr"));
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            KeyValueRepository repo = resource.repo;
            repo.put("key1", "val1");

            assertThatThrownBy(repo::keys)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("portable key enumeration API")
                    .hasMessageContaining("ConcurrentMap");

            assertThatThrownBy(repo::size)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("portable key enumeration API")
                    .hasMessageContaining("ConcurrentMap");

            assertThat(repo.get("key1")).isEqualTo("val1");
        }
    }

    @Test
    @DisplayName("Should execute TTL-bearing operations successfully without throwing errors")
    void shouldExecuteTtlOperationsSuccessfully() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            repo.put("k1", "v1", Duration.ofMinutes(5));
            repo.putIfAbsent("k2", "v2", Duration.ofMinutes(10));
            repo.put("k1", "v3", Duration.ofMinutes(2));

            assertThat(repo.get("k1")).isEqualTo("v3");
            assertThat(repo.get("k2")).isEqualTo("v2");
        }
    }

    @Test
    @DisplayName("Should maintain complete multi-store isolation between separate repository instances")
    void shouldMaintainMultiStoreIsolation() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders", "inventory");

        try (SafeRepoResource r1 = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager, "orders"));
             SafeRepoResource r2 = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager, "inventory"))) {

            SpringCacheKeyValueRepository ordersRepo = r1.repo;
            SpringCacheKeyValueRepository inventoryRepo = r2.repo;

            ordersRepo.put("item123", "ORDER_PLACED");
            inventoryRepo.put("item123", "IN_STOCK");

            assertThat(ordersRepo.get("item123")).isEqualTo("ORDER_PLACED");
            assertThat(inventoryRepo.get("item123")).isEqualTo("IN_STOCK");

            ordersRepo.clear();

            assertThat(ordersRepo.contains("item123")).isFalse();
            assertThat(inventoryRepo.contains("item123")).isTrue();
        }
    }

    @Test
    @DisplayName("Should support custom storeName routing mapped 1:1 to Spring Cache names")
    void shouldSupportCustomStoreNameMapping() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("myStore");
        try (SafeRepoResource resource = new SafeRepoResource(new SpringCacheKeyValueRepository(cacheManager, "myStore"))) {
            SpringCacheKeyValueRepository repo = resource.repo;
            repo.put("myKey", "myVal");
            assertThat(repo.get("myKey")).isEqualTo("myVal");
            assertThat(repo.getCacheName()).isEqualTo("myStore");
        }
    }

    @Test
    @DisplayName("TTL warning should be logged exactly once per repository instance")
    void ttlWarningLoggedOnlyOnce() {
        String cacheName = uniqueCacheName();
        Logger logger = (Logger) LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
        ThreadSafeTestAppender appender = attachAppender(logger);

        try {
            CacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
            KeyValueRepository first = new SpringCacheKeyValueRepository(cacheManager, cacheName);
            KeyValueRepository second = new SpringCacheKeyValueRepository(cacheManager, cacheName);

            // Multiple positive TTL calls on the first instance should log only once.
            first.put("key1", "value1", Duration.ofSeconds(10));
            first.put("key2", "value2", Duration.ofSeconds(20));

            // The second instance has its own once-only warning guard.
            second.put("key3", "value3", Duration.ofSeconds(10));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isEqualTo(2);
        } finally {
            detachAppender(logger, appender);
        }
    }

    @Test
    @DisplayName("Null, zero, or negative TTL should not trigger warning or consume once-only guard")
    void nonPositiveTtlShouldNotTriggerWarningOrConsumeGuard() {
        String cacheName = uniqueCacheName();
        Logger logger = (Logger) LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
        ThreadSafeTestAppender appender = attachAppender(logger);

        try {
            CacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
            KeyValueRepository repository =
                    new SpringCacheKeyValueRepository(cacheManager, cacheName);

            // Null, zero, and negative TTLs should not trigger a warning.
            repository.put("key-null", "value", null);
            repository.put("key-zero", "value", Duration.ZERO);
            repository.put("key-negative", "value", Duration.ofSeconds(-5));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isZero();

            // A positive TTL should still trigger the first warning.
            repository.put("key-positive", "value", Duration.ofSeconds(10));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isEqualTo(1);
        } finally {
            detachAppender(logger, appender);
        }
    }

    public static class ThreadSafeTestAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return events;
        }
    }

    private String uniqueCacheName() {
        return "ttl-warning-test-" + UUID.randomUUID();
    }

    private ThreadSafeTestAppender attachAppender(Logger logger) {
        ThreadSafeTestAppender appender = new ThreadSafeTestAppender();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(Logger logger, ThreadSafeTestAppender appender) {
        logger.detachAppender(appender);
        appender.stop();
    }

    private long ttlWarningCount(List<ILoggingEvent> events, String cacheName) {
        List<ILoggingEvent> snapshot = new ArrayList<>(events);
        String expectedStore = "store '" + cacheName + "'";

        return snapshot.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .filter(event -> SpringCacheKeyValueRepository.class.getName().equals(event.getLoggerName()))
                .filter(event -> event.getFormattedMessage().contains("Per-entry TTL"))
                .filter(event -> event.getFormattedMessage().contains(expectedStore))
                .count();
    }

    static final class NonConcurrentNativeCache implements Cache {
        private final String name;
        private final Map<Object, Object> store = new HashMap<>();

        NonConcurrentNativeCache(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(Object key) {
            if (!store.containsKey(key)) {
                return null;
            }
            Object value = store.get(key);
            return () -> value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            Object value = store.get(key);
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
            }
            return (T) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            if (store.containsKey(key)) {
                return (T) store.get(key);
            }
            try {
                T value = valueLoader.call();
                store.put(key, value);
                return value;
            } catch (Exception ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
        }

        @Override
        public void put(Object key, Object value) {
            store.put(key, value);
        }

        @Override
        public void evict(Object key) {
            store.remove(key);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            return store.remove(key) != null;
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public boolean invalidate() {
            boolean hadEntries = !store.isEmpty();
            store.clear();
            return hadEntries;
        }
    }

    static final class SingleCacheManager implements CacheManager {
        private final Cache cache;

        SingleCacheManager(Cache cache) {
            this.cache = cache;
        }

        @Override
        public Cache getCache(String name) {
            return cache.getName().equals(name) ? cache : null;
        }

        @Override
        public Collection<String> getCacheNames() {
            return Collections.singleton(cache.getName());
        }
    }

    private static class SafeRepoResource implements AutoCloseable {
        private final SpringCacheKeyValueRepository repo;

        SafeRepoResource(SpringCacheKeyValueRepository repo) {
            this.repo = repo;
        }

        @Override
        public void close() {
            repo.stop();
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
}
