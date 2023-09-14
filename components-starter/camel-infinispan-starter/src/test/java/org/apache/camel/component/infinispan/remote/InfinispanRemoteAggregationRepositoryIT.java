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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
		classes = {
				CamelAutoConfiguration.class,
				InfinispanRemoteAggregationRepositoryIT.class
		}
)
@Disabled("https://issues.apache.org/jira/browse/CAMEL-19869")
public class InfinispanRemoteAggregationRepositoryIT extends InfinispanRemoteTestSupport {
	public static final int COMPLETION_SIZE = 4;
	public static final String CORRELATOR_HEADER = "CORRELATOR_HEADER";

	@Bean
	public InfinispanRemoteAggregationRepository infinispanRemoteAggregationRepository() {
		InfinispanRemoteConfiguration configuration = new InfinispanRemoteConfiguration();
		configuration.setCacheContainerConfiguration(getConfiguration().build());

		InfinispanRemoteAggregationRepository repo = new InfinispanRemoteAggregationRepository(getCacheName());
		repo.setConfiguration(configuration);

		return repo;
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void checkAggregationFromOneRoute() throws Exception {
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:start")
						.aggregate(header(CORRELATOR_HEADER))
						.aggregationRepository(applicationContext.getBean(InfinispanRemoteAggregationRepository.class))
						.aggregationStrategy((oldExchange, newExchange) -> {
							if (oldExchange == null) {
								return newExchange;
							} else {
								Integer n = newExchange.getIn().getBody(Integer.class);
								Integer o = oldExchange.getIn().getBody(Integer.class);
								Integer v = (o == null ? 0 : o) + (n == null ? 0 : n);
								oldExchange.getIn().setBody(v, Integer.class);
								return oldExchange;
							}
						})
						.completionSize(COMPLETION_SIZE)
						.to("mock:result");
			}
		});

		MockEndpoint mock = getMockEndpoint("mock:result");
		mock.expectedMessageCount(2);
		mock.expectedBodiesReceived(1 + 3 + 4 + 5, 6 + 7 + 20 + 21);

		template.sendBodyAndHeader("direct:start", 1, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 3, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 4, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 5, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 6, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 7, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 20, CORRELATOR_HEADER, CORRELATOR_HEADER);
		template.sendBodyAndHeader("direct:start", 21, CORRELATOR_HEADER, CORRELATOR_HEADER);

		mock.assertIsSatisfied();
	}
}
