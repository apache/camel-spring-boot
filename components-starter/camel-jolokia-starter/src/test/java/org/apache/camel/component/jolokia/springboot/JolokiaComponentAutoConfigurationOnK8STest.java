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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = {CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class},
		properties = "camel.component.jolokia.kubernetesUseDefaultCa=false")
public class JolokiaComponentAutoConfigurationOnK8STest extends JolokiaComponentTestBase {

	@DynamicPropertySource
	static void customProperties(DynamicPropertyRegistry registry) {
		registry.add("camel.component.jolokia.serverConfig.caCert",
				() -> {
					try {
						final Path caPath = Files.createTempFile("csb", ".ca");
						caPath.toFile().deleteOnExit();
						return caPath.toAbsolutePath().toString();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
		registry.add("camel.component.jolokia.serverConfig.port",
				() -> String.valueOf(TestSocketUtils.findAvailableTcpPort()));
	}

	@Test
	void sslConfigurationTest() {
		Assertions.assertThat(agent.getServerConfig().getCaCert()).as("check caCert ssl configuration")
						.isNotBlank()
						.startsWith(String.format("%s%scsb", System.getProperty("java.io.tmpdir"),
								System.getProperty("java.io.tmpdir").endsWith(File.separator) ? "" : File.separator))
						.endsWith(".ca");
		Assertions.assertThat(agent.getServerConfig().getProtocol()).as("check ssl protocol configuration")
				.isEqualTo("https");
		Assertions.assertThat(agent.getServerConfig().useSslClientAuthentication()).as("check useSslClientAuthentication ssl configuration")
						.isTrue();
		Assertions.assertThat(agent).as("check default configuration")
				.hasFieldOrProperty("config").isNotNull()
				.extracting("config")
				.hasFieldOrPropertyWithValue("protocol", "https")
				.hasFieldOrPropertyWithValue("context", "/jolokia/")
				.hasFieldOrPropertyWithValue("useSslClientAuthentication", true)
				.hasFieldOrProperty("caCert").isNotNull();
	}
}
