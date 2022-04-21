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
package org.apache.camel.component.infinispan.remote.spring;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteIdempotentRepository;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteManager;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.test.infra.infinispan.services.InfinispanService;
import org.apache.camel.test.infra.infinispan.services.InfinispanServiceFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.query.remote.client.impl.MarshallerRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.UUID;

public abstract class SpringInfinispanRemoteIdempotentRepositoryTestSupport {
	@RegisterExtension
	public static InfinispanService service = InfinispanServiceFactory.createService();

	static RemoteCacheManager manager;

	@Autowired
	public CamelContext context;

	@Autowired
	public ProducerTemplate template;

	@BeforeAll
	public static void doPreSetup() throws Exception {
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

		manager = new RemoteCacheManager(clientBuilder.create());
		MarshallerRegistration.init(MarshallerUtil.getSerializationContext(manager));
		RemoteCache<Object, Object> cache = manager.administration().getOrCreateCache("idempotent", (String) null);
		assertNotNull(cache);
	}

	@Test
	public void testIdempotent() throws Exception {
		MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
		mock.expectedMessageCount(1);

		String messageId = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			template.sendBodyAndHeader("direct:start", UUID.randomUUID().toString(), "MessageId", messageId);
		}

		mock.assertIsSatisfied();
	}
}
