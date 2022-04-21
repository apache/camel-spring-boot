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

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.processor.idempotent.SpringCacheIdempotentRepository;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.infinispan.spring.remote.provider.SpringRemoteCacheManagerFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.Properties;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
		classes = {
				CamelAutoConfiguration.class,
				SpringInfinispanRemoteIdempotentRepositorySpringIT.class
		},
		properties = {"camel.springboot.routes-include-pattern=file:src/test/resources/org/apache/camel/component/infinispan/spring/SpringInfinispanRemoteIdempotentRepositorySpringTest.xml"}
)
public class SpringInfinispanRemoteIdempotentRepositorySpringIT
		extends SpringInfinispanRemoteIdempotentRepositoryTestSupport {

	@Bean
	public SpringRemoteCacheManagerFactoryBean springRemoteCacheManagerFactoryBean() throws Exception {
		Properties props = new Properties();
		props.putAll(Map.of("infinispan.client.hotrod.server_list", service.getServiceAddress(),
				"infinispan.client.hotrod.force_return_values", true,
				"infinispan.client.hotrod.auth_server_name", "infinispan",
				"infinispan.client.hotrod.auth_username", service.username(),
				"infinispan.client.hotrod.auth_password", service.password(),
				"infinispan.client.hotrod.auth_realm", "default",
				"infinispan.client.hotrod.sasl_mechanism", "DIGEST-MD5"
		));
		SpringRemoteCacheManagerFactoryBean springRemoteCacheManagerFactoryBean = new SpringRemoteCacheManagerFactoryBean();
		springRemoteCacheManagerFactoryBean.setConfigurationProperties(props);
		springRemoteCacheManagerFactoryBean.afterPropertiesSet();

		return springRemoteCacheManagerFactoryBean;
	}

	@Bean
	public SpringCacheIdempotentRepository repo() throws Exception {
		SpringCacheIdempotentRepository springCacheIdempotentRepository =
				new SpringCacheIdempotentRepository(springRemoteCacheManagerFactoryBean().getObject(), "idempotent");

		return springCacheIdempotentRepository;
	}
}
