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
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.statestore.StateStoreBackendFactory;
import org.apache.camel.component.statestore.StateStoreConstants;
import org.apache.camel.component.statestore.springboot.StateStoreComponentAutoConfiguration;
import org.apache.camel.component.statestore.springboot.StateStoreComponentConverter;
import org.apache.camel.spi.KeyValueRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.KeyValueAggregationRepository;
import org.apache.camel.support.KeyValueIdempotentRepository;
import org.apache.camel.test.infra.redis.services.RedisService;
import org.apache.camel.test.infra.redis.services.RedisServiceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@DisabledIfSystemProperty(named = "ci.env.name", matches = "github.com",
        disabledReason = "Disabled on GH Action due to Docker limit")
class SpringCacheStateStoreRedisIT {

    @RegisterExtension
    static RedisService redis = RedisServiceFactory.createService();

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CamelAutoConfiguration.class,
                        DataRedisAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        StateStoreComponentAutoConfiguration.class,
                        SpringCacheStateStoreAutoConfiguration.class))
                .withUserConfiguration(ProxyCachingConfiguration.class, StateStoreComponentConverter.class)
                .withPropertyValues(
                        "spring.main.banner-mode=off",
                        "camel.state-store.spring-cache.enabled=true",
                        "spring.cache.type=redis",
                        "spring.cache.cache-names=orders,users,idempotent,aggregate",
                        "spring.cache.redis.time-to-live=250ms",
                        "spring.data.redis.host=" + redis.host(),
                        "spring.data.redis.port=" + redis.port());
    }

    @Test
    void stateStoreOperationsUseRedisCacheManager() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
            assertThat(context).hasSingleBean(StateStoreBackendFactory.class);
            assertThat(context).doesNotHaveBean(KeyValueRepository.class);

            CamelContext camelContext = context.getBean(CamelContext.class);
            ProducerTemplate template = camelContext.createProducerTemplate();
            try {
                Object previous = template.requestBodyAndHeader("state-store:orders?operation=put", "order-1",
                        StateStoreConstants.KEY, "1");
                assertThat(previous).isNull();
                assertThat(template.requestBodyAndHeader("state-store:orders?operation=get", null,
                        StateStoreConstants.KEY, "1")).isEqualTo("order-1");
                assertThat(template.requestBodyAndHeader("state-store:orders?operation=contains", null,
                        StateStoreConstants.KEY, "1")).isEqualTo(true);

                Object existing = template.requestBodyAndHeader("state-store:orders?operation=putIfAbsent", "order-2",
                        StateStoreConstants.KEY, "1");
                assertThat(existing).isEqualTo("order-1");
                assertThat(template.requestBodyAndHeader("state-store:orders?operation=get", null,
                        StateStoreConstants.KEY, "1")).isEqualTo("order-1");

                Object deleted = template.requestBodyAndHeader("state-store:orders?operation=delete", null,
                        StateStoreConstants.KEY, "1");
                assertThat(deleted).isEqualTo("order-1");
                assertThat(template.requestBodyAndHeader("state-store:orders?operation=contains", null,
                        StateStoreConstants.KEY, "1")).isEqualTo(false);

                template.requestBodyAndHeader("state-store:users?operation=put", "user-1",
                        StateStoreConstants.KEY, "1");
                assertThat(template.requestBodyAndHeader("state-store:users?operation=get", null,
                        StateStoreConstants.KEY, "1")).isEqualTo("user-1");
            } finally {
                template.stop();
            }
        });
    }

    @Test
    void keysAndSizeThrowUnsupportedOperationException() {
        runner().run(context -> {
            StateStoreBackendFactory factory = context.getBean(StateStoreBackendFactory.class);
            KeyValueRepository repository = factory.createBackend("orders");
            repository.put("k", "v");

            assertThatThrownBy(repository::keys).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(repository::size).isInstanceOf(UnsupportedOperationException.class);
            assertThat(repository.get("k")).isEqualTo("v");
        });
    }

    @Test
    void explicitEipAdaptersRoundTripSerializableValues() {
        runner().run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            CamelContext camelContext = context.getBean(CamelContext.class);

            KeyValueIdempotentRepository idempotent
                    = new KeyValueIdempotentRepository(new SpringCacheKeyValueRepository(cacheManager, "idempotent"));
            idempotent.start();
            try {
                assertThat(idempotent.add("msg-1")).isTrue();
                assertThat(idempotent.add("msg-1")).isFalse();
                assertThat(idempotent.contains("msg-1")).isTrue();
            } finally {
                idempotent.stop();
            }

            SpringCacheKeyValueRepository aggregateStore
                    = new SpringCacheKeyValueRepository(cacheManager, "aggregate");
            KeyValueAggregationRepository aggregationRepository = new KeyValueAggregationRepository(aggregateStore);
            aggregationRepository.setCamelContext(camelContext);
            aggregationRepository.start();
            try {
                Exchange exchange = new DefaultExchange(camelContext);
                exchange.getMessage().setBody("payload");
                aggregationRepository.add(camelContext, "corr-1", exchange);

                Exchange restored = aggregationRepository.get(camelContext, "corr-1");
                assertThat(restored).isNotNull();
                assertThat(restored.getMessage().getBody()).isEqualTo("payload");
            } finally {
                aggregationRepository.stop();
            }
        });
    }

    @Test
    void providerLevelTimeToLiveExpiresEntries() {
        runner().run(context -> {
            StateStoreBackendFactory factory = context.getBean(StateStoreBackendFactory.class);
            KeyValueRepository repository = factory.createBackend("orders");
            repository.put("expiring", "value");
            assertThat(repository.get("expiring")).isEqualTo("value");

            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() -> assertThat(repository.get("expiring")).isNull());
        });
    }
}
