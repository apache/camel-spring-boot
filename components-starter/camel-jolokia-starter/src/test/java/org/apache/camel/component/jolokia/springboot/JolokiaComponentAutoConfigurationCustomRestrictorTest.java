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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jolokia.server.core.config.ConfigKey;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {CamelAutoConfiguration.class,
		JolokiaComponentAutoConfiguration.class},
		properties = {"camel.component.jolokia.serverConfig.port=0",
				"camel.component.jolokia.serverConfig.restrictorClass=org.apache.camel.component.jolokia.springboot.support.MyRestrictor"})
public class JolokiaComponentAutoConfigurationCustomRestrictorTest extends JolokiaComponentTestBase {

	@Test
	void checkConfigurationTest() {

		Assertions.assertThat(agent)
				.describedAs("check the configHolder restrictorClass property")
				.extracting("configHolder").isNotNull()
				.extracting("config")
				.asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
				.containsEntry(ConfigKey.RESTRICTOR_CLASS.getKeyValue(),
						"org.apache.camel.component.jolokia.springboot.support.MyRestrictor");

		Assertions.assertThat(agent.getServerConfig().getJolokiaConfig().containsKey(ConfigKey.RESTRICTOR_CLASS))
				.as("check the jolokia config")
				.isTrue();

		Assertions.assertThat(agent.getServerConfig().getJolokiaConfig().getConfig(ConfigKey.RESTRICTOR_CLASS))
				.as("check the jolokia config")
				.isEqualTo("org.apache.camel.component.jolokia.springboot.support.MyRestrictor");
	}
}
