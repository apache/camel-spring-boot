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

import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import org.jolokia.support.spring.SpringJolokiaConfigHolder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@SpringBootTest(classes = {JolokiaComponentAutoConfigurationPrefersBeanTest.TestConfiguration.class, CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class},
		properties = {"camel.component.jolokia.serverConfig.port=8778",
				"camel.component.jolokia.configFromPropertiesFirst=false",
				"logging.level.org.apache.camel.component.jolokia=TRACE"})
public class JolokiaComponentAutoConfigurationPrefersBeanTest extends JolokiaComponentTestBase {

	@Test
	void checkPortIsRandomTest() {
		assertThat(agent.getAddress().getPort())
				.describedAs("check the value from custom bean overrides the properties")
				.isNotZero()
				.isNotEqualTo(8778);
	}

	// *************************************
	// Config
	// *************************************

	@Configuration
	public static class TestConfiguration {

		@Bean("camelConfigHolder")
		public SpringJolokiaConfigHolder configHolder() {
			final SpringJolokiaConfigHolder myConfig = new SpringJolokiaConfigHolder();
			myConfig.setConfig(Map.of("port", "0"));
			return myConfig;
		}
	}
}
