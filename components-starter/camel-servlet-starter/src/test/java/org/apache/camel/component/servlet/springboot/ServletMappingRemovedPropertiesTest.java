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
package org.apache.camel.component.servlet.springboot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The servlet mapping options were named <tt>camel.component.servlet.mapping.*</tt> before Camel 3.10, and are
 * <tt>camel.servlet.mapping.*</tt> since. An option under the old prefix is not bound, and is logged at WARN when the
 * application starts, with the option to use instead.
 */
public class ServletMappingRemovedPropertiesTest {

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @Configuration
    @EnableAutoConfiguration
    public static class App {
        @Bean
        RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    restConfiguration().component("servlet");
                    rest("/hello").get().to("direct:hello");
                    from("direct:hello").setBody(constant("hello"));
                }
            };
        }
    }

    @BeforeEach
    public void startLogs() {
        logs.start();
    }

    @AfterEach
    public void stopLogs() {
        rootLogger().detachAppender(logs);
        logs.stop();
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    /**
     * Starts the web application with the given properties. The log appender is attached from an initializer, that is
     * after Spring Boot has initialized the logging system, so it sees the whole startup.
     */
    private ConfigurableApplicationContext run(String... properties) {
        return new SpringApplicationBuilder(App.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0")
                .properties(properties)
                .initializers(c -> rootLogger().addAppender(logs))
                .run();
    }

    private static HttpResponse<String> get(ConfigurableApplicationContext ctx, String path) throws Exception {
        int port = ((WebServerApplicationContext) ctx).getWebServer().getPort();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN))
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    public void oldContextPathIsReportedWithItsReplacement() throws Exception {
        try (ConfigurableApplicationContext ctx = run("camel.component.servlet.mapping.context-path=/api/*")) {
            assertThat(get(ctx, "/api/hello").statusCode()).as("the old option does not move the servlet")
                    .isEqualTo(404);
            HttpResponse<String> camel = get(ctx, "/camel/hello");
            assertThat(camel.statusCode()).as("the servlet stays at the default /camel/*").isEqualTo(200);
            assertThat(camel.body()).isEqualTo("hello");

            assertThat(warnings()).filteredOn(m -> m.contains("camel.component.servlet.mapping.context-path "))
                    .singleElement().asString()
                    .contains("has no effect")
                    .contains("Use camel.servlet.mapping.context-path instead");
        }
    }

    @Test
    public void contextPathMovesTheServletAndIsNotReported() throws Exception {
        try (ConfigurableApplicationContext ctx = run("camel.servlet.mapping.context-path=/api/*")) {
            HttpResponse<String> api = get(ctx, "/api/hello");
            assertThat(api.statusCode()).isEqualTo(200);
            assertThat(api.body()).isEqualTo("hello");
            assertThat(get(ctx, "/camel/hello").statusCode()).isEqualTo(404);

            assertThat(warnings()).noneMatch(m -> m.contains("has no effect"));
        }
    }
}
