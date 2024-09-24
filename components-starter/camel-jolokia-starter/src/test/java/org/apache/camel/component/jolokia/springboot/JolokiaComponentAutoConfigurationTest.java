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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.apache.camel.component.jolokia.springboot.restrictor.CamelRestrictor;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jolokia.support.spring.SystemPropertyMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.net.Socket;

@SpringBootTest(classes = {CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class},
		properties = "camel.component.jolokia.serverConfig.port=0")
public class JolokiaComponentAutoConfigurationTest extends JolokiaComponentTestBase {

	@Autowired
	ApplicationContext context;

	@Test
	void agentIsLoadedTest() {
		//just check the agent
	}

	@Test
	void agentIsStartedTest() {
		assertThat(agent.getAddress()).isNotNull();
		assertThat(agent.getAddress().getPort()).isGreaterThan(0);
		Assertions.assertThatCode(() -> new Socket().connect(agent.getAddress()))
				.as("check connection to %s:%s", agent.getAddress().getHostName(), agent.getAddress().getPort())
				.doesNotThrowAnyException();
	}

	@Test
	void defaultConfigurationTest() {

		Assertions.assertThat(context.getBean(JolokiaComponentConfiguration.class))
				.as("check starter configuration")
				.hasOnlyFields("enabled", "lookupConfig", "lookupServices",
						"systemPropertiesMode", "exposeApplicationContext", "serverConfig",
						"kubernetesDiscover", "kubernetesUseDefaultCa", "useCamelRestrictor", "configFromPropertiesFirst")
				.hasFieldOrPropertyWithValue("enabled", true)
				.hasFieldOrPropertyWithValue("lookupConfig", false)
				.hasFieldOrPropertyWithValue("lookupServices", false)
				.hasFieldOrPropertyWithValue("systemPropertiesMode", "never")
				.hasFieldOrPropertyWithValue("exposeApplicationContext", false)
				.hasFieldOrPropertyWithValue("kubernetesDiscover", true)
				.hasFieldOrPropertyWithValue("kubernetesUseDefaultCa", true)
				.hasFieldOrPropertyWithValue("useCamelRestrictor", true)
				.hasFieldOrPropertyWithValue("configFromPropertiesFirst", true);

		Assertions.assertThat(agent)
				.describedAs("check the default configHolder properties")
				.extracting("configHolder").isNotNull()
				.extracting("config")
				.asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
				.containsEntry("host", "0.0.0.0")
				.containsEntry("autoStart", "true")
				.containsEntry("restrictorClass", CamelRestrictor.class.getCanonicalName());

		Assertions.assertThat(agent).as("check default agent/server configuration")
				.hasFieldOrPropertyWithValue("lookupConfig", false)
				.hasFieldOrPropertyWithValue("lookupServices", false)
				.hasFieldOrPropertyWithValue("exposeApplicationContext", false)
				.hasFieldOrPropertyWithValue("systemPropertyMode", SystemPropertyMode.NEVER)
				.hasFieldOrProperty("config").isNotNull()
				.extracting("config")
					.hasFieldOrPropertyWithValue("protocol", "http")
					.hasFieldOrPropertyWithValue("context", "/jolokia/");
	}
}
