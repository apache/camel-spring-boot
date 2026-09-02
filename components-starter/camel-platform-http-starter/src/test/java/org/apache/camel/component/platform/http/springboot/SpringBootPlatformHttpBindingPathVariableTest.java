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

import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The consumer services the request on its own executor, so the path variables must be resolved without relying on the
 * request path the dispatch cached, which may already have been removed by then.
 */
public class SpringBootPlatformHttpBindingPathVariableTest {

    @Test
    void pathVariablesAreResolvedWithoutACachedRequestPath() throws Exception {
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.getRegistry().bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                    new SpringBootPlatformHttpEngine(8080));
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/greeting/{name}")
                            .transform().simple("${header.name}|${header.CamelHttpPath}");
                }
            });
            context.start();

            PlatformHttpComponent component = context.getComponent("platform-http", PlatformHttpComponent.class);
            SpringBootPlatformHttpConsumer consumer
                    = (SpringBootPlatformHttpConsumer) component.getHttpEndpoints().iterator().next().getConsumer();

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/greeting/%61dmin;v=1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            consumer.service(request, response).get(20, TimeUnit.SECONDS);

            assertEquals(200, response.getStatus());
            // the placeholder is decoded and carries no matrix parameter, CamelHttpPath stays raw
            assertEquals("admin|/greeting/%61dmin;v=1", response.getContentAsString());
        }
    }
}
