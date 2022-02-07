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
package org.apache.camel.integration.springboot.infinispan;

import org.apache.camel.Exchange;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.integration.springboot.Application;
import org.apache.camel.integration.springboot.SpringBootBaseIntegration;
import org.apache.camel.test.infra.infinispan.services.InfinispanService;
import org.apache.camel.test.infra.infinispan.services.InfinispanServiceFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.UUID;

@SpringBootTest(classes = { Application.class, InfinispanTest.InfinispanConfiguration.class })
public class InfinispanTest extends SpringBootBaseIntegration {

	@RegisterExtension
	public static InfinispanService service = InfinispanServiceFactory.createService();

	@Test
	public void producer() throws InterruptedException {
		final String msg = UUID.randomUUID().toString();
		final Integer key = new Random().nextInt();

		final MockEndpoint mock = camelContext.getEndpoint("mock:result", MockEndpoint.class);
		mock.expectedMessageCount(1);
		mock.expectedBodiesReceived(msg);

		sendMessage("direct:put", key, msg);

		sendMessage("direct:get", key, msg);

		mock.assertIsSatisfied();
	}

	@Test
	public void consumer() throws InterruptedException {
		final String msg = UUID.randomUUID().toString();
		final Integer key = new Random().nextInt();

		MockEndpoint mock = camelContext.getEndpoint("mock:consumerResult", MockEndpoint.class);
		mock.expectedMessageCount(1);
		mock.expectedHeaderReceived("CamelInfinispanKey", key);

		sendMessage("direct:consumer", key, msg);

		mock.assertIsSatisfied();
	}

	private Exchange sendMessage(String endpointUri, Integer key, String msg) {
		return producerTemplate.send(endpointUri, exchange -> {
			exchange.getIn().setHeader(InfinispanConstants.KEY, key);
			exchange.getIn().setHeader(InfinispanConstants.VALUE, msg);
		});
	}

	@Configuration
	public static class InfinispanConfiguration {

		protected static ConfigurationBuilder getConfiguration() {
			ConfigurationBuilder clientBuilder = new ConfigurationBuilder();

			// for default tests, we force return value for all the
			// operations
			clientBuilder
					.forceReturnValues(true);

			// add server from the test infra service
			clientBuilder
					.addServer()
					.host(service.host())
					.port(service.port());

			// add security info
			clientBuilder
					.security()
					.authentication()
					.username(service.username())
					.password(service.password())
					.serverName("infinispan")
					.saslMechanism("DIGEST-MD5")
					.realm("default");

			return clientBuilder;
		}

		@Bean("infinispanRemoteComponent")
		public InfinispanRemoteComponent infinispanRemoteComponent() {
			InfinispanRemoteConfiguration infinispanRemoteConfiguration = new InfinispanRemoteConfiguration();

			infinispanRemoteConfiguration.setHosts(service.host() + ":" + service.port());

			infinispanRemoteConfiguration.setUsername(service.username());
			infinispanRemoteConfiguration.setPassword(service.password());

			RemoteCacheManager cacheContainer = new RemoteCacheManager(getConfiguration().build());
			cacheContainer.administration()
					.getOrCreateCache(
							"myCache",
							new org.infinispan.configuration.cache.ConfigurationBuilder()
									.clustering()
									.cacheMode(CacheMode.DIST_SYNC).build());

			cacheContainer.administration()
					.getOrCreateCache(
							"myConsumerCache",
							new org.infinispan.configuration.cache.ConfigurationBuilder()
									.clustering()
									.cacheMode(CacheMode.DIST_SYNC).build());

			infinispanRemoteConfiguration.setCacheContainer(cacheContainer);
			InfinispanRemoteComponent component = new InfinispanRemoteComponent();
			component.setConfiguration(infinispanRemoteConfiguration);

			return component;
		}
	}
}
