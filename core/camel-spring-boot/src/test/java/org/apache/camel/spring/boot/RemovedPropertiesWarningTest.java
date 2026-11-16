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
package org.apache.camel.spring.boot;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The properties under the <tt>camel.springboot.</tt> prefix, which was removed in Camel 4.13, are not bound. Each one
 * is logged at WARN when the application starts, with the property to use instead.
 */
public class RemovedPropertiesWarningTest {

    @Configuration
    @EnableAutoConfiguration
    public static class App {
        @Bean
        RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:a").routeId("a").to("mock:b");
                }
            };
        }
    }

    private static final class Started implements AutoCloseable {
        private final ConfigurableApplicationContext ctx;
        private final ListAppender<ILoggingEvent> logs;

        Started(ConfigurableApplicationContext ctx, ListAppender<ILoggingEvent> logs) {
            this.ctx = ctx;
            this.logs = logs;
        }

        CamelContext camel() {
            return ctx.getBean(CamelContext.class);
        }

        List<String> warnings() {
            return logs.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN))
                    .map(ILoggingEvent::getFormattedMessage).toList();
        }

        /**
         * The single WARN that names the given property.
         */
        String warningAbout(String property) {
            List<String> found = warnings().stream().filter(m -> m.contains(property + " ")).toList();
            assertEquals(1, found.size(), "one WARN about " + property + " expected, the WARNs are: " + warnings());
            return found.get(0);
        }

        @Override
        public void close() {
            try {
                ctx.close();
            } finally {
                rootLogger().detachAppender(logs);
                logs.stop();
            }
        }
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    private static Started start(String... properties) {
        return start(Map.of(), new String[0], properties);
    }

    /**
     * Starts the application with the given environment variables, command line arguments and properties. The log
     * appender is attached from an initializer, that is after Spring Boot has initialized the logging system, so it
     * sees the whole startup.
     */
    private static Started start(Map<String, Object> environmentVariables, String[] args, String... properties) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(App.class)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .initializers(c -> {
                    rootLogger().addAppender(appender);
                    if (!environmentVariables.isEmpty()) {
                        c.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                                "test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                environmentVariables));
                    }
                })
                .run(args);
        return new Started(ctx, appender);
    }

    @Test
    public void environmentVariableIsReportedByItsName() {
        try (Started app = start(Map.of("CAMEL_SPRINGBOOT_MAIN_RUN_CONTROLLER", "true",
                "CAMEL_SPRINGBOOT_LENIENT_CONFIGURATION_BINDING", "true"), new String[0])) {
            String warning = app.warningAbout("CAMEL_SPRINGBOOT_MAIN_RUN_CONTROLLER");
            assertTrue(warning.contains("Use camel.main.run-controller instead"), warning);
            assertTrue(app.warnings().stream().noneMatch(m -> m.contains("LENIENT")),
                    "camel.springboot.lenient-configuration-binding is read: " + app.warnings());
        }
    }

    @Test
    public void propertyInSeveralSourcesIsReportedOnce() {
        try (Started app = start(Map.of(), new String[] { "--camel.springboot.name=FromArgs" },
                "camel.springboot.name=Legacy")) {
            String warning = app.warningAbout("camel.springboot.name");
            assertTrue(warning.contains("commandLineArgs"), "the source that wins is named: " + warning);
        }
    }

    @Test
    public void nameIsReportedWithItsReplacement() {
        try (Started app = start("camel.springboot.name=Legacy")) {
            assertNotEquals("Legacy", app.camel().getName(), "camel.springboot.name is not bound");
            String warning = app.warningAbout("camel.springboot.name");
            assertTrue(warning.contains("has no effect"), warning);
            assertTrue(warning.contains("Use camel.main.name instead"), warning);
        }
    }

    @Test
    public void mainRunControllerIsReportedWithRunController() {
        try (Started app = start("camel.springboot.main-run-controller=true")) {
            String warning = app.warningAbout("camel.springboot.main-run-controller");
            assertTrue(warning.contains("Use camel.main.run-controller instead"), warning);
        }
    }

    @Test
    public void typeConversionIsReportedWithItsReplacement() {
        try (Started app = start("camel.springboot.type-conversion=false")) {
            String warning = app.warningAbout("camel.springboot.type-conversion");
            assertTrue(warning.contains("Use camel.main.type-conversion instead"), warning);
        }
    }

    @Test
    public void routeControllerOptionsAreReportedWithTheirReplacement() {
        try (Started app = start("camel.springboot.route-controller-supervise-enabled=true",
                "camel.springboot.route-controller-back-off-delay=1000")) {
            assertFalse(app.camel().getRouteController().isSupervising(),
                    "camel.springboot.route-controller-supervise-enabled is not bound");
            String enabled = app.warningAbout("camel.springboot.route-controller-supervise-enabled");
            assertTrue(enabled.contains("Use camel.routecontroller.enabled instead"), enabled);
            String delay = app.warningAbout("camel.springboot.route-controller-back-off-delay");
            assertTrue(delay.contains("Use camel.routecontroller.back-off-delay instead"), delay);
        }
    }

    @Test
    public void camelCaseNameIsReportedAsWritten() {
        try (Started app = start("camel.springboot.streamCachingEnabled=false")) {
            assertTrue(app.camel().isStreamCaching(), "camel.springboot.streamCachingEnabled is not bound");
            String warning = app.warningAbout("camel.springboot.streamCachingEnabled");
            assertTrue(warning.contains("Use camel.main.stream-caching-enabled instead"), warning);
        }
    }

    @Test
    public void optionWithoutReplacementIsReported() {
        try (Started app = start("camel.springboot.xml-routes=classpath:routes.xml")) {
            String warning = app.warningAbout("camel.springboot.xml-routes");
            assertTrue(warning.contains("has no effect"), warning);
            assertFalse(warning.contains(" Use "), warning);
        }
    }

    @Test
    public void lenientConfigurationBindingIsNotReported() {
        try (Started app = start("camel.springboot.lenient-configuration-binding=true")) {
            assertTrue(app.warnings().stream().noneMatch(m -> m.contains("camel.springboot.")),
                    "camel.springboot.lenient-configuration-binding is read: " + app.warnings());
        }
    }

    @Test
    public void mainOptionIsAppliedAndNotReported() {
        try (Started app = start("camel.main.name=Current", "camel.main.run-controller=false")) {
            assertEquals("Current", app.camel().getName());
            assertTrue(app.warnings().stream().noneMatch(m -> m.contains("has no effect")),
                    "no WARN about camel.main options: " + app.warnings());
        }
    }
}
