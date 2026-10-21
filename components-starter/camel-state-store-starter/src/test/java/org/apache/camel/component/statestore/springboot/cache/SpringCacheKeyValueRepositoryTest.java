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

import org.apache.camel.spi.KeyValueRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringCacheKeyValueRepositoryTest {

    @Test
    void shouldHandleCrudLifecycleAndReturnValues() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
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
    void shouldHandlePutIfAbsent() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        Object existing1 = repo.putIfAbsent("key1", "val1");
        assertThat(existing1).isNull();
        assertThat(repo.get("key1")).isEqualTo("val1");

        Object existing2 = repo.putIfAbsent("key1", "newVal");
        assertThat(existing2).isEqualTo("val1");
        assertThat(repo.get("key1")).isEqualTo("val1");
    }

    @Test
    void shouldClearOnlyTheNamedCache() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        repo.put("key1", "val1");
        repo.put("key2", "val2");
        repo.clear();

        assertThat(repo.contains("key1")).isFalse();
        assertThat(repo.contains("key2")).isFalse();
    }

    @Test
    void shouldSupportInheritedConditionalOperations() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
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
    void shouldThrowWhenNamedCacheIsMissing() {
        SimpleCacheManager strictCacheManager = new SimpleCacheManager();
        strictCacheManager.setCaches(Collections.singletonList(new ConcurrentMapCache("other-cache")));
        strictCacheManager.initializeCaches();

        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(strictCacheManager, "orders");
        assertThatThrownBy(() -> repo.get("key1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Spring Cache 'orders' not found in CacheManager");
    }

    @Test
    void shouldEnumerateKeysAndSizeForConcurrentMapNativeCache() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        repo.put("key1", "val1");
        repo.put("key2", "val2");

        Cache cache = cacheManager.getCache("orders");
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

    @Test
    void shouldThrowUnsupportedForKeysAndSizeWithRealCaffeineCache() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("orders");
        KeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        repo.put("key1", "val1");

        Cache cache = cacheManager.getCache("orders");
        assertThat(cache).isNotNull();
        assertThat(cache.getNativeCache()).isNotInstanceOf(ConcurrentMap.class);
        assertThat(repo.get("key1")).isEqualTo("val1");

        assertThatThrownBy(repo::keys)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ConcurrentMap");
        assertThatThrownBy(repo::size)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ConcurrentMap");
    }

    @Test
    void shouldThrowUnsupportedForKeysAndSizeWhenNativeCacheIsNotConcurrentMap() {
        CacheManager cacheManager = new SingleCacheManager(new NonConcurrentNativeCache("orders"));
        KeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
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

    @Test
    void shouldStoreEntriesWhenTtlIsPositiveWithoutApplyingExpiration() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders");
        SpringCacheKeyValueRepository repo = new SpringCacheKeyValueRepository(cacheManager, "orders");
        repo.put("k1", "v1", Duration.ofMinutes(5));
        repo.putIfAbsent("k2", "v2", Duration.ofMinutes(10));
        repo.put("k1", "v3", Duration.ofMinutes(2));

        assertThat(repo.get("k1")).isEqualTo("v3");
        assertThat(repo.get("k2")).isEqualTo("v2");
    }

    @Test
    void shouldMaintainIsolationBetweenStoreNames() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("orders", "users");
        SpringCacheKeyValueRepository orders = new SpringCacheKeyValueRepository(cacheManager, "orders");
        SpringCacheKeyValueRepository users = new SpringCacheKeyValueRepository(cacheManager, "users");

        orders.put("id", "ORDER_PLACED");
        users.put("id", "IN_STOCK");

        assertThat(orders.get("id")).isEqualTo("ORDER_PLACED");
        assertThat(users.get("id")).isEqualTo("IN_STOCK");

        orders.clear();

        assertThat(orders.contains("id")).isFalse();
        assertThat(users.contains("id")).isTrue();
    }

    @Test
    void ttlWarningLoggedOnlyOncePerRepositoryInstance() {
        String cacheName = uniqueCacheName();
        Logger logger = (Logger) LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
        ThreadSafeTestAppender appender = attachAppender(logger);

        try {
            CacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
            KeyValueRepository first = new SpringCacheKeyValueRepository(cacheManager, cacheName);
            KeyValueRepository second = new SpringCacheKeyValueRepository(cacheManager, cacheName);

            first.put("key1", "value1", Duration.ofSeconds(10));
            first.put("key2", "value2", Duration.ofSeconds(20));
            second.put("key3", "value3", Duration.ofSeconds(10));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isEqualTo(2);
        } finally {
            detachAppender(logger, appender);
        }
    }

    @Test
    void nonPositiveTtlShouldNotTriggerWarningOrConsumeGuard() {
        String cacheName = uniqueCacheName();
        Logger logger = (Logger) LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
        ThreadSafeTestAppender appender = attachAppender(logger);

        try {
            CacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
            KeyValueRepository repository = new SpringCacheKeyValueRepository(cacheManager, cacheName);

            repository.put("key-null", "value", null);
            repository.put("key-zero", "value", Duration.ZERO);
            repository.put("key-negative", "value", Duration.ofSeconds(-5));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isZero();

            repository.put("key-positive", "value", Duration.ofSeconds(10));

            assertThat(ttlWarningCount(appender.getEvents(), cacheName)).isEqualTo(1);
        } finally {
            detachAppender(logger, appender);
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

    static final class ThreadSafeTestAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return events;
        }
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
}
