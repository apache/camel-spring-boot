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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.spi.KeyValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.util.Assert;

/**
 * An Apache Camel {@link KeyValueRepository} adapter backed by Spring's
 * {@link CacheManager}. Each instance is bound to one Spring cache whose name
 * matches the Camel state-store name.
 * <h3>Design Constraints and Backend Behavior</h3>
 * <ul>
 *   <li><strong>TTL support:</strong> Spring Cache does not define a standard
 *       per-entry TTL API. A positive, non-zero TTL triggers a single
 *       {@code WARN} log per repository instance. The entry is stored without
 *       applying the requested TTL. {@code null}, zero, and negative TTL values
 *       do not trigger this warning or consume the warning-once guard. Users
 *       requiring expiration must configure it through the underlying cache
 *       provider or {@link CacheManager}, for example using a Caffeine cache
 *       specification.</li>
 *   <li><strong>Write visibility:</strong> {@link Cache#put(Object, Object)}
 *       may register the entry asynchronously, so a following {@code get}
 *       may not see it yet. Spring Data Redis 4 does this by default with
 *       Lettuce; enable immediate writes on its {@code RedisCacheWriter} to
 *       get the read-your-writes behavior that Camel EIPs expect.</li>
 *   <li><strong>Atomicity:</strong> Spring Cache has no atomic get-and-put
 *       API. {@code put} and {@code delete} read the previous value and then
 *       write or evict in a second call, so the returned old value is not
 *       atomic. {@code putIfAbsent} delegates to
 *       {@link Cache#putIfAbsent(Object, Object)}, whose atomicity depends on
 *       the provider. Conditional operations such as {@code replace} and
 *       conditional {@code delete} use the {@link KeyValueRepository} SPI
 *       default implementations and may perform non-atomic check-then-act
 *       operations.</li>
 *   <li><strong>Enumeration:</strong> {@link #keys()} and {@link #size()} are
 *       supported only when {@link Cache#getNativeCache()} is a
 *       {@link ConcurrentMap}, for example {@link ConcurrentMapCache}. They
 *       operate on a snapshot of current {@link String} keys; {@link #size()}
 *       is the size of that snapshot, and non-{@link String} native keys are
 *       ignored. Other native cache types throw
 *       {@link UnsupportedOperationException} from {@link #keys()} and
 *       {@link #size()} only; {@code get}, {@code put}, {@code delete}, and
 *       related operations still delegate to Spring Cache. Spring's Caffeine,
 *       Redis, and JCache adapters do not return a {@code ConcurrentMap} from
 *       {@code getNativeCache()}, so enumeration is unsupported for those
 *       backends. When this adapter is bound explicitly, Camel operations that
 *       enumerate keys (Aggregate recovery, {@code completionTimeout} restore
 *       via {@code getKeys()}, and Idempotent Consumer {@code clear()})
 *       therefore require a {@code ConcurrentMap} native cache.</li>
 *   <li><strong>Scope:</strong> The repository is bound to the cache named by
 *       the constructor. {@link #clear()} invalidates that entire cache. Use a
 *       distinct store name (and therefore cache name) for each isolated
 *       store.</li>
 * </ul>
 */
public class SpringCacheKeyValueRepository implements KeyValueRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SpringCacheKeyValueRepository.class);
    private static final String ENUMERATION_UNSUPPORTED
            = "Spring Cache does not provide a portable key enumeration API; keys() and size() are supported only when Cache.getNativeCache() is a ConcurrentMap.";

    private final CacheManager cacheManager;
    private final String cacheName;
    private final AtomicBoolean ttlWarningLogged = new AtomicBoolean(false);

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
        cache.evictIfPresent(key);
        return oldValue;
    }

    @Override
    public boolean contains(String key) {
        return getCache().get(key) != null;
    }

    @Override
    public void clear() {
        Cache cache = getCache();
        cache.invalidate();
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(snapshotStringKeys());
    }

    @Override
    public int size() {
        return snapshotStringKeys().size();
    }

    private Set<String> snapshotStringKeys() {
        Object nativeCache = getCache().getNativeCache();
        if (!(nativeCache instanceof ConcurrentMap<?, ?> concurrentMap)) {
            throw new UnsupportedOperationException(ENUMERATION_UNSUPPORTED);
        }
        Set<String> snapshot = new HashSet<>();
        for (Object key : concurrentMap.keySet()) {
            if (key instanceof String stringKey) {
                snapshot.add(stringKey);
            }
        }
        return snapshot;
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
