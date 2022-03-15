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
package org.apache.camel.openapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.engine.DefaultClassResolver;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.openapi.producer.DummyRestProducerFactory;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.openapi.models.OasDocument;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
		classes = {
				CamelAutoConfiguration.class,
				RestOpenApiReaderPropertyPlaceholderTest.class,
				RestOpenApiReaderPropertyPlaceholderTest.TestConfiguration.class,
				DummyRestConsumerFactory.class
		},
		properties = {"foo=hello", "bar=bye"}
)
public class RestOpenApiReaderPropertyPlaceholderTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@BindToRegistry("dummy-rest")
	private final DummyRestProducerFactory factory = new DummyRestProducerFactory();

	@BindToRegistry("dummy-rest-consumer")
	private final DummyRestConsumerFactory consumerFactory = new DummyRestConsumerFactory();

	@Autowired
	CamelContext context;

	@Configuration
	public class TestConfiguration {

		@Bean
		public RouteBuilder routeBuilder() {
			return new RouteBuilder() {

				@Override
				public void configure() throws Exception {
					// this user REST service is json only
					rest("/{{foo}}").consumes("application/json").produces("application/json").get("/hi/{name}")
							.description("Saying hi").param().name("name").type(RestParamType.path)
							.dataType("string").description("Who is it").endParam().to("log:hi").get("/{{bar}}/{name}")
							.description("Saying bye").param().name("name")
							.type(RestParamType.path).dataType("string").description("Who is it").endParam().responseMessage()
							.code(200).message("A reply message").endResponseMessage()
							.to("log:bye").post("/{{bar}}").description("To update the greeting message")
							.consumes("application/xml").produces("application/xml").param().name("greeting")
							.type(RestParamType.body).dataType("string").description("Message to use as greeting").endParam()
							.to("log:bye");
				}
			};
		}
	}

	@Test
	public void testReaderRead() throws Exception {
		BeanConfig config = new BeanConfig();
		config.setHost("localhost:8080");
		config.setSchemes(new String[] {"http"});
		config.setBasePath("/api");
		config.setVersion("2.0");
		RestOpenApiReader reader = new RestOpenApiReader();

		RestOpenApiSupport support = new RestOpenApiSupport();
		List<RestDefinition> rests = support.getRestDefinitions(context);

		OasDocument openApi = reader.read(context, rests, config, context.getName(), new DefaultClassResolver());
		assertNotNull(openApi);

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		Object dump = Library.writeNode(openApi);
		String json = mapper.writeValueAsString(dump);

		log.info(json);

		assertTrue(json.contains("\"host\" : \"localhost:8080\""));
		assertTrue(json.contains("\"basePath\" : \"/api\""));
		assertTrue(json.contains("\"/hello/bye\""));
		assertTrue(json.contains("\"summary\" : \"To update the greeting message\""));
		assertTrue(json.contains("\"/hello/bye/{name}\""));
		assertTrue(json.contains("\"/hello/hi/{name}\""));
		assertFalse(json.contains("{foo}"));
		assertFalse(json.contains("{bar}"));

		context.stop();
	}
}
