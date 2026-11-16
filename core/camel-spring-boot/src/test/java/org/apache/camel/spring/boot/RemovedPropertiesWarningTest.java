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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.health.HealthCheckRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The properties under a prefix that was removed or renamed, such as <tt>camel.springboot.</tt> (removed in Camel
 * 4.13), and the <tt>camel.main.</tt> properties that are not an option, such as <tt>camel.main.backlog-tracing</tt>
 * (moved to <tt>camel.trace.enabled</tt> in Camel 4.5), are not bound. Each one is logged at WARN when the application
 * starts, with the property to use instead.
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
        try (Started app = start("camel.springboot.xml-routes-reload-directory=src/main/resources/camel")) {
            String warning = app.warningAbout("camel.springboot.xml-routes-reload-directory");
            assertTrue(warning.contains("has no effect"), warning);
            assertFalse(warning.contains(" Use "), warning);
        }
    }

    @Test
    public void xmlRoutesAreReportedWithRoutesIncludePattern() {
        try (Started app = start("camel.springboot.xml-routes=classpath:camel/*.xml",
                "camel.springboot.xml-rests=classpath:camel-rest/*.xml",
                "camel.springboot.xml-route-templates=classpath:camel-template/*.xml")) {
            for (String option : List.of("xml-routes", "xml-rests", "xml-route-templates")) {
                String warning = app.warningAbout("camel.springboot." + option);
                assertTrue(warning.contains("Use camel.main.routes-include-pattern instead"), warning);
            }
        }
    }

    @Test
    public void supervisingControllerOptionsAreReportedWithTheirReplacement() {
        try (Started app = start("camel.supervising.controller.enabled=true",
                "camel.supervising.controller.default-back-off.delay=1000")) {
            assertFalse(app.camel().getRouteController().isSupervising(),
                    "camel.supervising.controller.enabled is not bound");
            String enabled = app.warningAbout("camel.supervising.controller.enabled");
            assertTrue(enabled.contains("Use camel.routecontroller.enabled instead"), enabled);
            String delay = app.warningAbout("camel.supervising.controller.default-back-off.delay");
            assertTrue(delay.contains("Use camel.routecontroller.back-off-delay instead"), delay);
        }
    }

    @Test
    public void renamedDataFormatsAreReportedWithTheirNewName() {
        try (Started app = start("camel.dataformat.securexml.key-cipher-algorithm=RSA_OAEP",
                "camel.dataformat.yaml-snakeyaml.prettyFlow=true")) {
            String secureXml = app.warningAbout("camel.dataformat.securexml.key-cipher-algorithm");
            assertTrue(secureXml.contains("Use camel.dataformat.xml-security.key-cipher-algorithm instead"), secureXml);
            String snakeYaml = app.warningAbout("camel.dataformat.yaml-snakeyaml.prettyFlow");
            assertTrue(snakeYaml.contains("Use camel.dataformat.snake-yaml.pretty-flow instead"), snakeYaml);
        }
    }

    @Test
    public void removedHealthOptionsAreReportedWithTheirReplacement() {
        try (Started app = start("camel.health.components-enabled=true", "camel.health.config[netty].enabled=false")) {
            assertFalse(producerHealthChecksEnabled(app), "camel.health.components-enabled is not bound");
            String components = app.warningAbout("camel.health.components-enabled");
            assertTrue(components.contains("Use camel.health.producers-enabled instead"), components);
            String config = app.warningAbout("camel.health.config[netty].enabled");
            assertTrue(config.contains("Use camel.health.exclude-pattern instead"), config);
        }
    }

    @Test
    public void currentOptionsAreAppliedAndNotReported() {
        try (Started app = start("camel.health.producers-enabled=true", "camel.dataformat.snake-yaml.pretty-flow=true",
                "camel.routecontroller.back-off-delay=1000")) {
            assertTrue(producerHealthChecksEnabled(app));
            assertTrue(app.warnings().stream().noneMatch(m -> m.contains("has no effect")),
                    "no WARN about current options: " + app.warnings());
        }
    }

    private static boolean producerHealthChecksEnabled(Started app) {
        HealthCheckRegistry registry
                = app.camel().getCamelContextExtension().getContextPlugin(HealthCheckRegistry.class);
        return registry.getRepository("producers").map(HealthCheckRepository::isEnabled).orElse(false);
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

    /**
     * The camel.main options that Camel moved to another prefix or replaced, with the option to use instead.
     */
    private static final Map<String, String> MOVED_MAIN_OPTIONS = movedMainOptions();

    private static Map<String, String> movedMainOptions() {
        Map<String, String> answer = new HashMap<>();
        answer.put("camel.main.debugging", "camel.debug.enabled");
        answer.put("camel.main.debugger", "camel.debug.enabled");
        answer.put("camel.main.backlog-tracing", "camel.trace.enabled");
        answer.put("camel.main.backlog-tracing-standby", "camel.trace.standby");
        answer.put("camel.main.backlog-tracing-templates", "camel.trace.trace-templates");
        answer.put("camel.main.route-controller-supervise-enabled", "camel.routecontroller.enabled");
        answer.put("camel.main.route-controller-back-off-delay", "camel.routecontroller.back-off-delay");
        answer.put("camel.main.route-controller-include-routes", "camel.routecontroller.include-routes");
        answer.put("camel.main.error-registry-enabled", "camel.errorregistry.enabled");
        answer.put("camel.main.error-registry-maximum-entries", "camel.errorregistry.maximum-entries");
        answer.put("camel.main.error-registry-time-to-live-seconds", "camel.errorregistry.time-to-live-seconds");
        answer.put("camel.main.xml-routes", "camel.main.routes-include-pattern");
        answer.put("camel.main.xml-rests", "camel.main.routes-include-pattern");
        answer.put("camel.main.xml-route-templates", "camel.main.routes-include-pattern");
        return Collections.unmodifiableMap(answer);
    }

    @Test
    public void movedMainOptionsAreNotBound() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        MOVED_MAIN_OPTIONS.keySet().forEach(key -> source.put(key, "1000"));
        source.put("camel.main.name", "Bound");
        CamelConfigurationProperties.Main main = new CamelConfigurationProperties().getMain();

        BindException e = assertThrows(BindException.class, () -> new Binder(source).bind("camel.main",
                Bindable.ofInstance(main), new NoUnboundElementsBindHandler(BindHandler.DEFAULT)));

        UnboundConfigurationPropertiesException unbound
                = assertInstanceOf(UnboundConfigurationPropertiesException.class, e.getCause());
        assertEquals(MOVED_MAIN_OPTIONS.keySet(), unbound.getUnboundProperties().stream()
                .map(p -> p.getName().toString()).collect(Collectors.toSet()));
        assertEquals("Bound", main.getName());
    }

    @Test
    public void movedMainOptionsAreReportedWithTheirReplacement() {
        String[] properties = MOVED_MAIN_OPTIONS.keySet().stream().map(key -> key + "=true").toArray(String[]::new);
        try (Started app = start(properties)) {
            assertNotEquals(Boolean.TRUE, app.camel().isBacklogTracing(), "camel.main.backlog-tracing is not bound");
            assertNotEquals(Boolean.TRUE, app.camel().isDebugging(), "camel.main.debugging is not bound");
            assertFalse(app.camel().getRouteController().isSupervising(),
                    "camel.main.route-controller-supervise-enabled is not bound");
            assertFalse(app.camel().getErrorRegistry().isEnabled(), "camel.main.error-registry-enabled is not bound");
            for (Map.Entry<String, String> option : MOVED_MAIN_OPTIONS.entrySet()) {
                String warning = app.warningAbout(option.getKey());
                assertTrue(warning.contains("has no effect"), warning);
                assertTrue(warning.contains("Use " + option.getValue() + " instead"), warning);
            }
        }
    }

    @Test
    public void camelCaseMovedMainOptionIsReportedAsWritten() {
        try (Started app = start("camel.main.backlogTracingStandby=true",
                "camel.main.routeControllerBackOffDelay=5000")) {
            String standby = app.warningAbout("camel.main.backlogTracingStandby");
            assertTrue(standby.contains("Use camel.trace.standby instead"), standby);
            String delay = app.warningAbout("camel.main.routeControllerBackOffDelay");
            assertTrue(delay.contains("Use camel.routecontroller.back-off-delay instead"), delay);
        }
    }

    @Test
    public void springbootOptionMovedFromMainIsReportedWithItsReplacement() {
        try (Started app = start("camel.springboot.backlog-tracing=true")) {
            String warning = app.warningAbout("camel.springboot.backlog-tracing");
            assertTrue(warning.contains("Use camel.trace.enabled instead"), warning);
        }
    }

    @Test
    public void unknownMainOptionIsReported() {
        try (Started app = start("camel.main.nmae=Typo", "camel.main.error-registry-stack-trace-enabled=true")) {
            assertNotEquals("Typo", app.camel().getName(), "camel.main.nmae is not bound");
            for (String property : List.of("camel.main.nmae", "camel.main.error-registry-stack-trace-enabled")) {
                String warning = app.warningAbout(property);
                assertTrue(warning.endsWith("as Camel on Spring Boot has no camel.main option of this name."), warning);
                assertFalse(warning.contains(" Use "), warning);
            }
        }
    }

    @Test
    public void camelMainStandaloneOptionIsReported() {
        try (Started app = start("camel.main.base-package-scan=com.acme.routes",
                "camel.main.routes-builder-classes=com.acme.routes.MyRoute")) {
            for (String property : List.of("camel.main.base-package-scan", "camel.main.routes-builder-classes")) {
                String warning = app.warningAbout(property);
                assertTrue(warning.contains("has no effect"), warning);
            }
        }
    }

    @Test
    public void mainOptionsInEveryFormAreAppliedAndNotReported() {
        try (Started app = start("camel.main.streamCachingEnabled=false", "camel.main.shutdown_timeout=7",
                "camel.main.global-options[CamelLogDebugBodyMaxChars]=500", "camel.main.global-options.foo=bar",
                "camel.main.type-conversion=true", "camel.main.main-run-controller=false",
                "camel.main.include-non-singletons=false", "camel.main.warn-on-early-shutdown=true")) {
            assertFalse(app.camel().isStreamCaching());
            assertEquals(7, app.camel().getShutdownStrategy().getTimeout());
            assertEquals("500", app.camel().getGlobalOptions().get("CamelLogDebugBodyMaxChars"));
            assertEquals("bar", app.camel().getGlobalOptions().get("foo"));
            assertTrue(app.warnings().stream().noneMatch(m -> m.contains("has no effect")),
                    "no WARN about camel.main options: " + app.warnings());
        }
    }

    @Test
    public void mainEnvironmentVariablesAreReportedOnlyWhenMoved() {
        try (Started app = start(Map.of("CAMEL_MAIN_SHUTDOWN_TIMEOUT", "5", "CAMEL_MAIN_BACKLOG_TRACING", "true",
                "CAMEL_MAIN_NMAE", "Typo"), new String[0])) {
            assertEquals(5, app.camel().getShutdownStrategy().getTimeout());
            String warning = app.warningAbout("CAMEL_MAIN_BACKLOG_TRACING");
            assertTrue(warning.contains("Use camel.trace.enabled instead"), warning);
            // the name of an environment variable cannot tell a dash from a dot, so a typo is not reported
            assertEquals(List.of(warning), app.warnings().stream().filter(m -> m.contains("has no effect")).toList());
        }
    }

    @Test
    public void everyDocumentedMainOptionIsNotReported() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        documentedMainOptions().forEach(option -> properties.put(option, "x"));
        assertTrue(properties.size() > 100, "the camel.main options of the metadata: " + properties.keySet());
        properties.put("camel.main.type-conversion", "x");
        properties.put("camel.main.global-options.foo", "x");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger().addAppender(appender);
        try {
            CamelAutoConfiguration.warnUnboundMainProperties(environment);
        } finally {
            rootLogger().detachAppender(appender);
            appender.stop();
        }
        assertEquals(List.of(), appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    /**
     * The camel.main options in the Spring Boot configuration metadata of this module.
     */
    @SuppressWarnings("unchecked")
    private static Set<String> documentedMainOptions() throws IOException {
        List<URL> urls = Collections.list(RemovedPropertiesWarningTest.class.getClassLoader()
                .getResources("META-INF/spring-configuration-metadata.json"));
        List<String> answer = new ArrayList<>();
        for (URL url : urls) {
            try (InputStream is = url.openStream()) {
                Map<String, Object> metadata = JsonParserFactory.getJsonParser()
                        .parseMap(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                for (Object property : (List<Object>) metadata.getOrDefault("properties", List.of())) {
                    String name = (String) ((Map<String, Object>) property).get("name");
                    if (name.startsWith("camel.main.")) {
                        answer.add(name);
                    }
                }
            }
        }
        return Set.copyOf(answer);
    }
}
