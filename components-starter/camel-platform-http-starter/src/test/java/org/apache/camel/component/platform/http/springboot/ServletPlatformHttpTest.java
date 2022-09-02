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
package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.springboot.ServletComponentAutoConfiguration;
import org.apache.camel.component.servlet.springboot.ServletMappingAutoConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootApplication
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {
				CamelAutoConfiguration.class,
				ServletPlatformHttpTest.class,
				ServletPlatformHttpTest.TestConfiguration.class,
				ServletPlatformHttpAutoConfiguration.class,
				ServletComponentAutoConfiguration.class,
				ServletMappingAutoConfiguration.class
		}
)
public class ServletPlatformHttpTest extends ServletPlatformHttpBase {

	// *************************************
	// Config
	// *************************************
	@Configuration
	public static class TestConfiguration {

		@Bean
		public RouteBuilder servletPlatformHttpRouteBuilder() {
			return new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("platform-http:/get")
							.setBody().constant("get");
					from("platform-http:/post")
							.transform().body(String.class, b -> b.toUpperCase());
				}
			};
		}
	}
}
