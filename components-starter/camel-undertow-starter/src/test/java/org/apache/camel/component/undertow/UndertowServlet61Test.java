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
package org.apache.camel.component.undertow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CamelSpringBootTest
@DirtiesContext
@SpringBootTest(classes = UndertowServlet61Test.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UndertowServlet61Test {

    @RegisterExtension
    static AvailablePortFinder.Port camelPort = AvailablePortFinder.find();

    @RegisterExtension
    AvailablePortFinder.Port servletPort = AvailablePortFinder.find();

    @Autowired
    ProducerTemplate producerTemplate;

    @Test
    void servlet61AndCamelConsumerCoexist() throws Exception {
        DeploymentManager deployment = Servlets.newContainer().addDeployment(Servlets.deployment()
                .setClassLoader(getClass().getClassLoader())
                .setContextPath("/")
                .setDeploymentName("servlet61")
                .addServlet(Servlets.servlet("echo", AsyncEchoServlet.class)
                        .addMapping("/servlet").setAsyncSupported(true)));
        deployment.deploy();
        Undertow server = null;
        try {
            server = Undertow.builder().addHttpListener(servletPort.getPort(), "localhost")
                    .setHandler(deployment.start()).build();
            server.start();

            assertEquals(6, deployment.getDeployment().getServletContext().getMajorVersion());
            assertEquals(1, deployment.getDeployment().getServletContext().getMinorVersion());
            assertEquals("Hello Servlet", producerTemplate.requestBody(
                    "undertow:http://localhost:" + servletPort.getPort() + "/servlet", "Hello Servlet", String.class));
            assertEquals("Hello Camel", producerTemplate.requestBody(
                    "undertow:http://localhost:" + camelPort.getPort() + "/camel", "Hello Camel", String.class));
        } finally {
            if (server != null) {
                server.stop();
            }
            deployment.stop();
            deployment.undeploy();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestConfiguration {

        @Bean
        RouteBuilder echoRoute() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("undertow:http://localhost:" + camelPort.getPort() + "/camel")
                            .convertBodyTo(String.class);
                }
            };
        }
    }

    public static class AsyncEchoServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
            AsyncContext async = request.startAsync();
            async.start(() -> {
                try {
                    response.setCharacterEncoding(StandardCharsets.UTF_8);
                    response.getOutputStream().write(ByteBuffer.wrap(request.getInputStream().readAllBytes()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    async.complete();
                }
            });
        }
    }
}
