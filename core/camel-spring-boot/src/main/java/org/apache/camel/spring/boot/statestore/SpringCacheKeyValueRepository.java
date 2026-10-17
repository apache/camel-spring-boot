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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.spi.KeyValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * An Apache Camel {@link KeyValueRepository} adapter backed by Spring's {@link CacheManager}.
 *
 * <h3>Design Constraints &amp; Backend Behavior:</h3>
 * <ul>
 *   <li><strong>TTL Support:</strong> Spring Cache has no standard per-entry TTL API. Providing a TTL argument
 *       triggers a single {@code WARN} log per repository instance and stores the entry without expiration.
 *       Users requiring TTL must configure it at the underlying provider/CacheManager spec level (e.g., Caffeine spec).</li>
 *   <li><strong>Atomicity:</strong> CAS operations ({@code putIfAbsent}, {@code replace}, conditional delete)
 *       delegate to Spring Cache's {@code Cache.putIfAbsent()}. Atomicity guarantees are provider-dependent
 *       (e.g., atomic in Caffeine, but potentially non-atomic in distributed backends like Lettuce/Redis).</li>
 *   <li><strong>Enumeration:</strong> {@link #keys()} and {@link #size()} intentionally throw
 *       {@link UnsupportedOperationException} as Spring Cache does not provide a portable key enumeration API.</li>
 *   <li><strong>Scope &amp; Multi-Store:</strong> Backed by a single global {@link CacheManager}. Multiple endpoints
 *       can isolate state by specifying distinct cache/store names while sharing the same underlying manager.</li>
 * </ul>
 */
public class SpringCacheKeyValueRepository implements KeyValueRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
    private static final String DEFAULT_CACHE_NAME = "camel-kvr";

    private final CacheManager cacheManager;
    private final String cacheName;
    private final AtomicBoolean ttlWarningLogged = new AtomicBoolean(false);

    public SpringCacheKeyValueRepository(CacheManager cacheManager) {
        this(cacheManager, DEFAULT_CACHE_NAME);
    }

    public SpringCacheKeyValueRepository(CacheManager cacheManager, String cacheName) {
        Assert.notNull(cacheManager, "CacheManager must not be null");
        Assert.hasText(cacheName, "CacheName must not be empty");
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException(String.format("Spring Cache '%s' not found in CacheManager of type %s",
                    cacheName, cacheManager.getClass().getName()));
        }
        return cache;
    }

    @Override
    public void start() {
        // Lifecycle start hook (managed externally by Spring CacheManager container)
    }

    @Override
    public void stop() {
        // Lifecycle stop hook (managed externally by Spring CacheManager container)
    }

    @Override
    public Object get(String key) {
        Cache.ValueWrapper wrapper = getCache().get(key);
        return wrapper != null ? wrapper.get() : null;
    }

    @Override
    public Object put(String key, Object value) {
        return put(key, value, null);
    }

    @Override
    public Object put(String key, Object value, Duration ttl) {
        checkTtl(ttl);
        Cache cache = getCache();
        Cache.ValueWrapper oldWrapper = cache.get(key);
        Object oldValue = oldWrapper != null ? oldWrapper.get() : null;
        cache.put(key, value);
        return oldValue;
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return putIfAbsent(key, value, null);
    }

    @Override
    public Object putIfAbsent(String key, Object value, Duration ttl) {
        checkTtl(ttl);
        Cache.ValueWrapper existing = getCache().putIfAbsent(key, value);
        return existing != null ? existing.get() : null;
    }

    @Override
    public Object delete(String key) {
        Cache cache = getCache();
        Cache.ValueWrapper oldWrapper = cache.get(key);
        Object oldValue = oldWrapper != null ? oldWrapper.get() : null;
        cache.evict(key);
        return oldValue;
    }

    @Override
    public boolean contains(String key) {
        return getCache().get(key) != null;
    }

    @Override
    public void clear() {
        getCache().clear();
    }

    @Override
    public Set<String> keys() {
        throw new UnsupportedOperationException("Spring Cache does not provide a portable key enumeration API.");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Spring Cache does not provide a portable size API.");
    }

    private void checkTtl(Duration ttl) {
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            if (ttlWarningLogged.compareAndSet(false, true)) {
                LOG.warn("Per-entry TTL of {} is not supported by the Spring Cache backend for store '{}'; storing without expiration.",
                        ttl, cacheName);
            }
        }
    }
}
