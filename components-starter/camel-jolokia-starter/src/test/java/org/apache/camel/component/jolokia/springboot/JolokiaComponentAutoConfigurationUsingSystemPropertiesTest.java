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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;
import org.jolokia.support.spring.SystemPropertyMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.TestSocketUtils;

import java.util.Map;

@SpringBootTest(classes = {CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class},
		properties = "camel.component.jolokia.systemPropertiesMode=override")
public class JolokiaComponentAutoConfigurationUsingSystemPropertiesTest extends JolokiaComponentTestBase {

	private static final Map<String, String> SYS_VARS = Map.of("jolokia.port", String.valueOf(TestSocketUtils.findAvailableTcpPort()));

	@BeforeAll
	public static void setSystemProperties() {
		SYS_VARS.entrySet().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
	}

	@AfterAll
	public static void cleanSystemProperties() {
		SYS_VARS.keySet().forEach(System::clearProperty);
	}

	@Test
	void systemPropertyTest() {
		Assertions.assertThat(agent).as("check systemPropertiesMode property on agent")
				.hasFieldOrPropertyWithValue("systemPropertyMode", SystemPropertyMode.OVERRIDE);
	}

	@Test
	void customPortTest() {
		assertThat(agent.getAddress().getPort())
				.describedAs("check the port is configured from system property")
				.isEqualTo(Integer.parseInt(SYS_VARS.get("jolokia.port")));
	}
}
