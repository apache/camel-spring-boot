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

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The starter must start outside a servlet web application, where Spring Boot registers neither
 * {@link ServerProperties} nor {@code WebMvcProperties}, for example {@code @SpringBootTest(webEnvironment = NONE)}.
 */
public class SpringBootPlatformHttpNonWebContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CamelAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
                    PlatformHttpComponentAutoConfiguration.class, PlatformHttpComponentConverter.class,
                    SpringBootPlatformHttpAutoConfiguration.class, SpringBootPlatformWebMvcConfiguration.class))
            .withBean(RouteBuilder.class, () -> new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/hello").routeId("hello").setBody().constant("hello");
                }
            });

    @Test
    void startsWithoutServerProperties() {
        runner.withPropertyValues("server.port=12345").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CamelContext.class).getRouteController().getRouteStatus("hello"))
                    .isEqualTo(ServiceStatus.Started);
            assertThat(((SpringBootPlatformHttpEngine) context.getBean(PlatformHttpEngine.class)).getServerPort())
                    .isEqualTo(12345);
        });
    }

    @Test
    void defaultsToPort8080() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(((SpringBootPlatformHttpEngine) context.getBean(PlatformHttpEngine.class)).getServerPort())
                    .isEqualTo(8080);
        });
    }

    @Test
    void serverPropertiesPortWins() {
        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(23456);
        runner.withBean(ServerProperties.class, () -> serverProperties).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(((SpringBootPlatformHttpEngine) context.getBean(PlatformHttpEngine.class)).getServerPort())
                    .isEqualTo(23456);
        });
    }
}
