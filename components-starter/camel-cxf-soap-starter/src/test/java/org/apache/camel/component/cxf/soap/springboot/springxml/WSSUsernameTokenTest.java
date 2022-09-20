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
package org.apache.camel.component.cxf.soap.springboot.springxml;

import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.security.GreetingService;
import org.apache.camel.component.cxf.security.jaas.SimpleLoginModule;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
		CamelAutoConfiguration.class,
		WSSUsernameTokenTest.class,
		SimpleLoginModule.class,
		CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ImportResource({
		"classpath:routes/soap-security.xml"
})
public class WSSUsernameTokenTest {
	
	private static final String BAD_PASSWORD = "123";

	private static final URL WSDL_URL;
	
	static int port = CXFTestSupport.getPort1();

	static {
		try {
			WSDL_URL = new URL("http://localhost:" + port 
			                   + "/services/greeting-service?wsdl");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private static final QName SERVICE_NAME = new QName("http://security.cxf.component.camel.apache.org/",
			"GreetingServiceImplService");
	
	@Bean
	public ServletWebServerFactory servletWebServerFactory() {
	    return new UndertowServletWebServerFactory(port);
	}

	private void addWSSUsernameTokenHandler(Service service, final String username, final String password) {
		// set a handler resolver providing WSSUsernameTokenHandler in the handler chain
		final HandlerResolver handlerResolver = new HandlerResolver() {
			@Override
			public List<Handler> getHandlerChain(PortInfo portInfo) {
				final ArrayList<Handler> handlerChain = new ArrayList<>();
				handlerChain.add(new WSSUsernameTokenHandler(username, password));
				return handlerChain;
			}
		};
		service.setHandlerResolver(handlerResolver);
	}

	@Test
	public void testAuthenticationCorrectCredentials() throws Exception {
		final Service service = Service.create(WSDL_URL, SERVICE_NAME);
		addWSSUsernameTokenHandler(service, SimpleLoginModule.USERNAME, SimpleLoginModule.PASSWORD);
		final GreetingService greetingService = service.getPort(GreetingService.class);

		final String reply = greetingService.greet("you");
		Assertions.assertEquals(reply, "Hello you");
	}

	@Test
	public void testAuthenticationIncorrectCredentials() throws Exception {
		final Service service = Service.create(WSDL_URL, SERVICE_NAME);
		addWSSUsernameTokenHandler(service, SimpleLoginModule.USERNAME, BAD_PASSWORD);
		final GreetingService greetingService = service.getPort(GreetingService.class);

		try {
			greetingService.greet("you");
			Assertions.fail("Authentication should failed");
		} catch (Exception e) {
			Assertions.assertTrue(e.getMessage().contains("Authentication failed"));
		}
	}

	@Test
	public void testAuthenticationMissingCredentials() throws Exception {
		final Service service = Service.create(WSDL_URL, SERVICE_NAME);
		final GreetingService greetingService = service.getPort(GreetingService.class);

		try {
			greetingService.greet("you");
			Assertions.fail("Authentication should failed");
		} catch (Exception e) {
			Assertions.assertTrue(e.getMessage().contains("security error"));
		}
	}
}
