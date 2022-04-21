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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanIdempotentRepositoryTestSupport;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeEach;

import org.infinispan.commons.api.BasicCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
		classes = {
				CamelAutoConfiguration.class,
				InfinispanRemoteIdempotentRepositoryIT.class
		}
)
public class InfinispanRemoteIdempotentRepositoryIT extends InfinispanRemoteTestSupport
		implements InfinispanIdempotentRepositoryTestSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	@Lazy
	public IdempotentRepository repo() {
		InfinispanRemoteIdempotentRepository repo = new InfinispanRemoteIdempotentRepository(getCacheName());
		repo.setCacheContainer(cacheContainer);

		return repo;
	}

	@BeforeEach
	protected void beforeEach() {
		// cleanup the default test cache before each run
		getCache().clear();
	}

	@Override
	public IdempotentRepository getIdempotentRepository() {
		return applicationContext.getBean(IdempotentRepository.class);
	}

	@Override
	public BasicCache<Object, Object> getCache() {
		return super.getCache();
	}

	@Override
	public MockEndpoint getMockEndpoint(String id) {
		return super.getMockEndpoint(id);
	}

	@Bean
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:start")
						.idempotentConsumer(
								header("MessageID"),
								getIdempotentRepository())
						.skipDuplicate(true)
						.to("mock:result");
			}
		};
	}
}
