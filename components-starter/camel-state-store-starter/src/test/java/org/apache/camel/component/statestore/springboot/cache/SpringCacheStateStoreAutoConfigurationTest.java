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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.statestore.StateStoreBackendFactory;
import org.apache.camel.component.statestore.StateStoreComponent;
import org.apache.camel.component.statestore.StateStoreConstants;
import org.apache.camel.component.statestore.springboot.StateStoreComponentAutoConfiguration;
import org.apache.camel.component.statestore.springboot.StateStoreComponentConverter;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.spi.KeyValueRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.MemoryKeyValueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SpringCacheStateStoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringCacheStateStoreAutoConfiguration.class))
            .withPropertyValues("spring.main.banner-mode=off");

    private final ApplicationContextRunner camelRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CamelAutoConfiguration.class,
                    StateStoreComponentAutoConfiguration.class,
                    SpringCacheStateStoreAutoConfiguration.class))
            .withUserConfiguration(StateStoreComponentConverter.class)
            .withPropertyValues("spring.main.banner-mode=off");

    @Test
    void doesNotRegisterFactoryWhenPropertyIsAbsent() {
        contextRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SpringCacheStateStoreAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    void doesNotRegisterFactoryWhenPropertyIsFalse() {
        contextRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withPropertyValues("camel.state-store.spring-cache.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    void doesNotRegisterFactoryWhenCacheManagerIsAbsent() {
        contextRunner
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    void registersFactoryWhenEnabledWithSingleCacheManager() {
        contextRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);

                    StateStoreBackendFactory factory = context.getBean(StateStoreBackendFactory.class);
                    KeyValueRepository repository = factory.createBackend("orders");
                    assertThat(repository).isInstanceOf(SpringCacheKeyValueRepository.class);
                    assertThat(((SpringCacheKeyValueRepository) repository).getCacheName()).isEqualTo("orders");
                });
    }

    @Test
    void doesNotRegisterFactoryWhenMultipleCacheManagersPresent() {
        contextRunner
                .withBean("cacheManagerOne", CacheManager.class, ConcurrentMapCacheManager::new)
                .withBean("cacheManagerTwo", CacheManager.class, ConcurrentMapCacheManager::new)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context.getBeansOfType(CacheManager.class)).hasSize(2);
                    assertThat(context).doesNotHaveBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    void backsOffWhenCustomFactoryExists() {
        StateStoreBackendFactory customFactory = storeName -> new MemoryKeyValueRepository();
        contextRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withBean("customFactory", StateStoreBackendFactory.class, () -> customFactory)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(StateStoreBackendFactory.class);
                    assertThat(context.getBean(StateStoreBackendFactory.class)).isSameAs(customFactory);
                    assertThat(context.getBean(StateStoreBackendFactory.class).createBackend("orders"))
                            .isInstanceOf(MemoryKeyValueRepository.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                });
    }

    @Test
    void createsIndependentRepositoriesPerStoreName() {
        contextRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    StateStoreBackendFactory factory = context.getBean(StateStoreBackendFactory.class);
                    KeyValueRepository orders = factory.createBackend("orders");
                    KeyValueRepository users = factory.createBackend("users");

                    assertThat(((SpringCacheKeyValueRepository) orders).getCacheName()).isEqualTo("orders");
                    assertThat(((SpringCacheKeyValueRepository) users).getCacheName()).isEqualTo("users");

                    orders.put("id", "order-1");
                    users.put("id", "user-1");

                    assertThat(orders.get("id")).isEqualTo("order-1");
                    assertThat(users.get("id")).isEqualTo("user-1");

                    orders.clear();
                    assertThat(orders.contains("id")).isFalse();
                    assertThat(users.get("id")).isEqualTo("user-1");
                });
    }

    @Test
    void doesNotExposeGlobalRepositoryToUnrelatedEips() {
        camelRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(StateStoreBackendFactory.class);
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);

                    CamelContext camelContext = context.getBean(CamelContext.class);
                    assertThat(camelContext.getRegistry().findSingleByType(KeyValueRepository.class)).isNull();
                    assertThat(camelContext.getRegistry().findByType(KeyValueRepository.class)).isEmpty();

                    StateStoreComponent component = camelContext.getComponent("state-store", StateStoreComponent.class);
                    assertThat(component.getBackendFactory())
                            .isSameAs(context.getBean(StateStoreBackendFactory.class));

                    ProducerTemplate template = camelContext.createProducerTemplate();
                    try {
                        template.requestBodyAndHeader("state-store:orders?operation=put", "order-1",
                                StateStoreConstants.KEY, "id");
                        template.requestBodyAndHeader("state-store:users?operation=put", "user-1",
                                StateStoreConstants.KEY, "id");

                        assertThat(template.requestBodyAndHeader("state-store:orders?operation=get", null,
                                StateStoreConstants.KEY, "id")).isEqualTo("order-1");
                        assertThat(template.requestBodyAndHeader("state-store:users?operation=get", null,
                                StateStoreConstants.KEY, "id")).isEqualTo("user-1");
                    } finally {
                        template.stop();
                    }

                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager.getCache("orders").get("id").get()).isEqualTo("order-1");
                    assertThat(cacheManager.getCache("users").get("id").get()).isEqualTo("user-1");
                });
    }

    @Test
    void optimisticLockingAggregatorStartsWhenFactoryEnabled() {
        camelRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withBean(RouteBuilder.class, this::optimisticLockingRoute)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                    CamelContext camelContext = context.getBean(CamelContext.class);
                    assertThat(camelContext.getStatus().isStarted()).isTrue();
                    assertThat(camelContext.getRoute("agg")).isNotNull();
                });
    }

    @Test
    void completionTimeoutAggregatorStartsWhenFactoryEnabled() {
        camelRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withBean(RouteBuilder.class, this::completionTimeoutRoute)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CamelContext camelContext = context.getBean(CamelContext.class);
                    assertThat(camelContext.getStatus().isStarted()).isTrue();
                    assertThat(camelContext.getRoute("timeout")).isNotNull();
                });
    }

    @Test
    void cacheEipTtlStillExpiresWhenFactoryEnabled() {
        camelRunner
                .withBean("cacheManager", CacheManager.class, ConcurrentMapCacheManager::new)
                .withBean(RouteBuilder.class, this::cacheEipRoute)
                .withPropertyValues("camel.state-store.spring-cache.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(KeyValueRepository.class);
                    CamelContext camelContext = context.getBean(CamelContext.class);
                    ProducerTemplate template = camelContext.createProducerTemplate();
                    try {
                        assertThat(callCache(template, 1)).isEqualTo("computed-1");
                        assertThat(callCache(template, 2)).isEqualTo("computed-1");
                        await().atMost(5, TimeUnit.SECONDS)
                                .pollDelay(Duration.ofMillis(250))
                                .untilAsserted(() -> assertThat(callCache(template, 3)).isEqualTo("computed-3"));
                    } finally {
                        template.stop();
                    }
                });
    }

    private RouteBuilder optimisticLockingRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:agg").routeId("agg")
                        .aggregate(header("id"), new UseLatestAggregationStrategy())
                        .completionSize(2).optimisticLocking()
                        .to("mock:agg");
            }
        };
    }

    private RouteBuilder completionTimeoutRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:timeout").routeId("timeout")
                        .aggregate(header("id"), new UseLatestAggregationStrategy())
                        .completionTimeout(1000)
                        .to("mock:timeout");
            }
        };
    }

    private RouteBuilder cacheEipRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:cached")
                        .cache(header("id"))
                            .ttl(200)
                            .setBody(simple("computed-${header.n}"))
                        .end();
            }
        };
    }

    private static Object callCache(ProducerTemplate template, int n) {
        return template.requestBodyAndHeaders("direct:cached", null, Map.of("id", "k", "n", n));
    }
}
