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

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.processor.idempotent.IdempotentConsumer;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.KeyValueAggregationRepository;
import org.apache.camel.support.KeyValueIdempotentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the Camel EIP call paths that use {@link SpringCacheKeyValueRepository}.
 * Enumeration-dependent paths are covered for a ConcurrentMap native cache and for a
 * cache whose native store is not a ConcurrentMap. These tests do not claim that every
 * Spring Cache provider supports Aggregate recovery or Idempotent Consumer cleanup.
 */
class SpringCacheKeyValueRepositoryEipCompatibilityTest {

    @Test
    @DisplayName("Aggregate getKeys and scan work when the native cache is a ConcurrentMap")
    void aggregateRecoveryPathsWorkForConcurrentMapCache() throws Exception {
        SpringCacheKeyValueRepository repo = concurrentMapRepository();
        CamelContext context = new DefaultCamelContext();
        KeyValueAggregationRepository aggregationRepository = new KeyValueAggregationRepository(repo);
        aggregationRepository.setCamelContext(context);
        aggregationRepository.start();
        try {
            Exchange exchange = new DefaultExchange(context);
            exchange.getMessage().setBody("payload");
            aggregationRepository.add(context, "corr-1", exchange);

            assertThat(aggregationRepository.getKeys()).containsExactly("corr-1");
            assertThat(aggregationRepository.scan(context)).isEmpty();

            aggregationRepository.remove(context, "corr-1", exchange);
            assertThat(aggregationRepository.getKeys()).isEmpty();
            assertThat(aggregationRepository.scan(context)).containsExactly(exchange.getExchangeId());
        } finally {
            aggregationRepository.stop();
            context.stop();
        }
    }

    @Test
    @DisplayName("Aggregate getKeys and scan throw when the native cache is not a ConcurrentMap")
    void aggregateRecoveryPathsThrowWhenNativeCacheIsNotConcurrentMap() throws Exception {
        SpringCacheKeyValueRepository repo = nonConcurrentRepository();
        CamelContext context = new DefaultCamelContext();
        KeyValueAggregationRepository aggregationRepository = new KeyValueAggregationRepository(repo);
        aggregationRepository.setCamelContext(context);
        aggregationRepository.start();
        try {
            Exchange exchange = new DefaultExchange(context);
            exchange.getMessage().setBody("payload");
            aggregationRepository.add(context, "corr-1", exchange);

            assertThatThrownBy(aggregationRepository::getKeys)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ConcurrentMap");
            assertThatThrownBy(() -> aggregationRepository.scan(context))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ConcurrentMap");
        } finally {
            aggregationRepository.stop();
            context.stop();
        }
    }

    @Test
    @DisplayName("Aggregate with completionTimeout starts when keys() is supported")
    void aggregateCompletionTimeoutStartsWithConcurrentMapCache() throws Exception {
        SpringCacheKeyValueRepository repo = concurrentMapRepository();
        DefaultCamelContext context = new DefaultCamelContext();
        try {
            context.getRegistry().bind("kvr", repo);
            context.addRoutes(completionTimeoutRoute());
            context.start();
            assertThat(context.getStatus().isStarted()).isTrue();
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("Aggregate with completionTimeout fails to start when keys() is unsupported")
    void aggregateCompletionTimeoutFailsToStartWhenKeysUnsupported() throws Exception {
        SpringCacheKeyValueRepository repo = nonConcurrentRepository();
        DefaultCamelContext context = new DefaultCamelContext();
        try {
            context.getRegistry().bind("kvr", repo);
            context.addRoutes(completionTimeoutRoute());
            assertThatThrownBy(context::start)
                    .hasRootCauseInstanceOf(UnsupportedOperationException.class);
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("Idempotent Consumer duplicate detection works; clear() works for ConcurrentMap")
    void idempotentConsumerDuplicateDetectionAndClearForConcurrentMap() throws Exception {
        SpringCacheKeyValueRepository repo = concurrentMapRepository();
        DefaultCamelContext context = new DefaultCamelContext();
        ProducerTemplate template = null;
        try {
            context.getRegistry().bind("kvr", repo);
            context.addRoutes(idempotentRoute());
            context.start();
            template = context.createProducerTemplate();

            MockEndpoint mock = context.getEndpoint("mock:idemp", MockEndpoint.class);
            mock.expectedBodiesReceived("first");
            template.sendBodyAndHeader("direct:idemp", "first", "messageId", "dup-1");
            template.sendBodyAndHeader("direct:idemp", "duplicate", "messageId", "dup-1");
            MockEndpoint.assertIsSatisfied(context);

            IdempotentConsumer consumer = context.getProcessor("idemp", IdempotentConsumer.class);
            consumer.clear();
            assertThat(repo.keys()).isEmpty();
            assertThat(repo.size()).isZero();

            mock.reset();
            mock.expectedBodiesReceived("after-clear");
            template.sendBodyAndHeader("direct:idemp", "after-clear", "messageId", "dup-1");
            MockEndpoint.assertIsSatisfied(context);
        } finally {
            if (template != null) {
                template.stop();
            }
            context.stop();
        }
    }

    @Test
    @DisplayName("Idempotent Consumer duplicate detection works; clear() throws when keys() is unsupported")
    void idempotentConsumerClearThrowsWhenKeysUnsupported() throws Exception {
        SpringCacheKeyValueRepository repo = nonConcurrentRepository();
        DefaultCamelContext context = new DefaultCamelContext();
        ProducerTemplate template = null;
        try {
            context.getRegistry().bind("kvr", repo);
            context.addRoutes(idempotentRoute());
            context.start();
            template = context.createProducerTemplate();

            MockEndpoint mock = context.getEndpoint("mock:idemp", MockEndpoint.class);
            mock.expectedBodiesReceived("first");
            template.sendBodyAndHeader("direct:idemp", "first", "messageId", "dup-1");
            template.sendBodyAndHeader("direct:idemp", "duplicate", "messageId", "dup-1");
            MockEndpoint.assertIsSatisfied(context);

            IdempotentConsumer consumer = context.getProcessor("idemp", IdempotentConsumer.class);
            assertThatThrownBy(consumer::clear)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ConcurrentMap");
            assertThat(repo.contains("idempotent:dup-1")).isTrue();
        } finally {
            if (template != null) {
                template.stop();
            }
            context.stop();
        }
    }

    @Test
    @DisplayName("KeyValueIdempotentRepository.clear() enumerates keys and is therefore provider-dependent")
    void keyValueIdempotentClearRequiresKeys() throws Exception {
        KeyValueIdempotentRepository concurrent = new KeyValueIdempotentRepository(concurrentMapRepository());
        concurrent.start();
        try {
            assertThat(concurrent.add("msg-1")).isTrue();
            assertThat(concurrent.add("msg-1")).isFalse();
            concurrent.clear();
            assertThat(concurrent.contains("msg-1")).isFalse();
        } finally {
            concurrent.stop();
        }

        KeyValueIdempotentRepository unsupported = new KeyValueIdempotentRepository(nonConcurrentRepository());
        unsupported.start();
        try {
            assertThat(unsupported.add("msg-1")).isTrue();
            assertThatThrownBy(unsupported::clear)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ConcurrentMap");
            assertThat(unsupported.contains("msg-1")).isTrue();
        } finally {
            unsupported.stop();
        }
    }

    @Test
    @DisplayName("Cache EIP caches results; positive TTL is not applied by the Spring Cache adapter")
    void cacheEipIgnoresPerEntryTtlWithConcurrentMapCache() throws Exception {
        SpringCacheKeyValueRepository repo = concurrentMapRepository();
        DefaultCamelContext context = new DefaultCamelContext();
        try {
            context.getRegistry().bind("kvr", repo);
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:cache")
                            .cache(simple("${header.productId}"))
                                .ttl(200)
                                .to("mock:service")
                                .setBody(simple("response-${header.productId}"))
                            .end()
                            .to("mock:result");
                }
            });
            context.start();
            ProducerTemplate template = context.createProducerTemplate();

            MockEndpoint service = context.getEndpoint("mock:service", MockEndpoint.class);
            MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);

            service.expectedMessageCount(1);
            result.expectedBodiesReceived("response-A");
            template.sendBodyAndHeader("direct:cache", "req1", "productId", "A");
            MockEndpoint.assertIsSatisfied(context);
            assertThat(repo.get("A")).isEqualTo("response-A");

            service.reset();
            result.reset();
            service.expectedMessageCount(0);
            result.expectedBodiesReceived("response-A");
            template.sendBodyAndHeader("direct:cache", "req2", "productId", "A");
            MockEndpoint.assertIsSatisfied(context);

            await().atMost(2, TimeUnit.SECONDS)
                    .pollDelay(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        service.reset();
                        result.reset();
                        service.expectedMessageCount(0);
                        result.expectedBodiesReceived("response-A");
                        template.sendBodyAndHeader("direct:cache", "req3", "productId", "A");
                        MockEndpoint.assertIsSatisfied(context);
                        assertThat(repo.get("A")).isEqualTo("response-A");
                    });
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("clear() invalidates the shared cache used by multiple EIP adapters")
    void clearInvalidatesSharedCacheAcrossEipAdapters() throws Exception {
        SpringCacheKeyValueRepository repo = concurrentMapRepository();
        CamelContext context = new DefaultCamelContext();
        KeyValueIdempotentRepository idempotentRepository = new KeyValueIdempotentRepository(repo);
        KeyValueAggregationRepository aggregationRepository = new KeyValueAggregationRepository(repo);
        idempotentRepository.start();
        aggregationRepository.setCamelContext(context);
        aggregationRepository.start();
        try {
            idempotentRepository.add("msg-1");
            Exchange exchange = new DefaultExchange(context);
            exchange.getMessage().setBody("payload");
            aggregationRepository.add(context, "corr-1", exchange);
            repo.put("cache-key", "cached-value");

            repo.clear();

            assertThat(idempotentRepository.contains("msg-1")).isFalse();
            assertThat(aggregationRepository.get(context, "corr-1")).isNull();
            assertThat(repo.contains("cache-key")).isFalse();
            assertThat(repo.size()).isZero();
        } finally {
            aggregationRepository.stop();
            idempotentRepository.stop();
            context.stop();
        }
    }

    private static RouteBuilder completionTimeoutRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:agg")
                        .aggregate(header("id"), new UseLatestAggregationStrategy())
                        .completionTimeout(1000)
                        .to("mock:agg");
            }
        };
    }

    private static RouteBuilder idempotentRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:idemp")
                        .idempotentConsumer(header("messageId")).id("idemp")
                        .to("mock:idemp");
            }
        };
    }

    private static SpringCacheKeyValueRepository concurrentMapRepository() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("camel-kvr");
        return new SpringCacheKeyValueRepository(cacheManager);
    }

    private static SpringCacheKeyValueRepository nonConcurrentRepository() {
        CacheManager cacheManager = new SpringCacheKeyValueRepositoryAutoConfigurationTest.SingleCacheManager(
                new SpringCacheKeyValueRepositoryAutoConfigurationTest.NonConcurrentNativeCache("camel-kvr"));
        return new SpringCacheKeyValueRepository(cacheManager);
    }
}
