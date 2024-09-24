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
package org.apache.camel.component.jolokia.springboot;

import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class},
		properties = {"camel.component.jolokia.kubernetesDiscover=false", "camel.component.jolokia.serverConfig.port=0"})
public class JolokiaComponentAutoConfigurationOnK8SDisabledTest extends JolokiaComponentTestBase {

	@Test
	void httpConfigurationTest() {
		Assertions.assertThat(agent.getServerConfig().getCaCert()).as("check caCert is not set")
				.isNull();
		Assertions.assertThat(agent.getServerConfig().getProtocol()).as("check protocol configuration")
				.isEqualTo("http");
		Assertions.assertThat(agent.getServerConfig().useSslClientAuthentication()).as("check useSslClientAuthentication ssl configuration")
						.isFalse();
	}
}
