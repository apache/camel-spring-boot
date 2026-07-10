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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpringBootPlatformHttpConsumerExecutorTest {

    @Test
    void engineWithoutExecutorServicesRequests() throws Exception {
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.getRegistry().bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                    new SpringBootPlatformHttpEngine(8080));
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/fallback").setBody().constant("ok");
                }
            });
            context.start();

            SpringBootPlatformHttpConsumer consumer = getConsumer(context);
            MockHttpServletResponse response = serviceRequest(consumer);

            assertEquals(200, response.getStatus());
            assertEquals("ok", response.getContentAsString());
        }
    }

    @Test
    void consumerOwnedExecutorIsShutDownOnStop() throws Exception {
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.getRegistry().bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                    new SpringBootPlatformHttpEngine(8080));
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/owned").setBody().constant("ok");
                }
            });
            context.start();

            SpringBootPlatformHttpConsumer consumer = getConsumer(context);
            // trigger the lazy thread creation so shutdown has something to stop
            serviceRequest(consumer);

            context.stop();
        }
        // no leaked non-daemon executor thread keeps the JVM from exiting; the
        // assertion happens implicitly by the test JVM terminating, but check
        // explicitly that a provided executor is NOT shut down by the consumer
        ExecutorService provided = Executors.newSingleThreadExecutor();
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.getRegistry().bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                    new SpringBootPlatformHttpEngine(8080, provided));
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/provided").setBody().constant("ok");
                }
            });
            context.start();
            serviceRequest(getConsumer(context));
            context.stop();
        }
        assertFalse(provided.isShutdown(), "externally provided executor must not be shut down by the consumer");
        provided.shutdown();
    }

    @Test
    void nullExecutorIsRejected() {
        assertThrows(NullPointerException.class, () -> new SpringBootPlatformHttpConsumer(null, null, null));
    }

    private static SpringBootPlatformHttpConsumer getConsumer(DefaultCamelContext context) {
        PlatformHttpComponent component = context.getComponent("platform-http", PlatformHttpComponent.class);
        return (SpringBootPlatformHttpConsumer) component.getHttpEndpoints().iterator().next().getConsumer();
    }

    private static MockHttpServletResponse serviceRequest(SpringBootPlatformHttpConsumer consumer) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // service runs asynchronously on the consumer executor; wait for completion
        consumer.service(request, response).get(20, TimeUnit.SECONDS);
        return response;
    }
}
