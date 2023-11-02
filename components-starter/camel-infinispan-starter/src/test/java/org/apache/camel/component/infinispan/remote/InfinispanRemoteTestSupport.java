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
package org.apache.camel.component.infinispan.remote;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.test.infra.infinispan.services.InfinispanService;
import org.apache.camel.test.infra.infinispan.services.InfinispanServiceFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.testcontainers.shaded.org.apache.commons.lang3.SystemUtils;

import java.util.Properties;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class InfinispanRemoteTestSupport {
	public static final String TEST_CACHE = "mycache";
	@RegisterExtension
	public static InfinispanService service = InfinispanServiceFactory.createService();
	protected static RemoteCacheManager cacheContainer;

	public static final boolean CLIENT_INTELLIGENCE_BASIC = SystemUtils.IS_OS_MAC || Boolean.parseBoolean(System.getProperty("infinispan.client_intelligence.basic", "false"));

	@Autowired
	ProducerTemplate template;

	@Autowired
	CamelContext context;

	protected BasicCache<Object, Object> getCache() {
		return getCache(getCacheName());
	}

	protected String getCacheName() {
		return TEST_CACHE;
	}

	protected MockEndpoint getMockEndpoint(String endpoint) {
		return context.getEndpoint(endpoint, MockEndpoint.class);
	}

	public FluentProducerTemplate fluentTemplate() {
		return context.createFluentProducerTemplate();
	}

	public CamelContext context() {
		return context;
	}

	public ProducerTemplate template() {
		return template;
	}

	@BeforeAll
	protected static void setupResourcesAbstract() throws Exception {
//		LoggerFactory.getLogger(getClass()).info("setupResources");

		cacheContainer = new RemoteCacheManager(getConfiguration().build());
		cacheContainer.administration()
				.getOrCreateCache(
						TEST_CACHE,
						new org.infinispan.configuration.cache.ConfigurationBuilder()
								.clustering()
								.cacheMode(CacheMode.DIST_SYNC).build());
	}

	@AfterAll
	protected static void cleanupResourcesAbstract() throws Exception {
//		LoggerFactory.getLogger(getClass()).info("setupResources");

		if (cacheContainer != null) {
			cacheContainer.stop();
		}
	}

	protected BasicCache<Object, Object> getCache(String name) {
		return cacheContainer.getCache(name);
	}

	protected static BasicCache<Object, Object> getCacheByName(String name) {
		return cacheContainer.getCache(name);
	}

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

		if (CLIENT_INTELLIGENCE_BASIC) {
			Properties properties = new Properties();
			properties.put("infinispan.client.hotrod.client_intelligence", "BASIC");
			clientBuilder.withProperties(properties);
		}

		clientBuilder.marshaller(new ProtoStreamMarshaller());

		return clientBuilder;
	}

	@Lazy
	@Bean
	public ComponentCustomizer infinispanComponentCustomizer() throws Exception {
		return ComponentCustomizer.forType(
				InfinispanRemoteComponent.class,
				component -> component.getConfiguration().setCacheContainer(cacheContainer));
	}
}
