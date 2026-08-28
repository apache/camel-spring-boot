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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.PropertiesFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEarlyResolutionPropertiesParserTest {

    private static final String GUARD = "camel.component.test-vault.early-resolve-properties";
    private static final String OVERRIDE_SOURCE = "overridden-camel-test-vault-properties";

    /**
     * Records every key the parser asks the environment for, so that the order in which the guard and the
     * opt-out flag are read can be asserted.
     */
    private static final class RecordingEnvironment extends StandardEnvironment {
        private final List<String> queried = new ArrayList<>();

        @Override
        public String getProperty(String key) {
            queried.add(key);
            return super.getProperty(key);
        }
    }

    /**
     * Stands in for a vault client. Records what it was asked to resolve and fails for a configured set of
     * remainders, so no container or network is needed.
     */
    private static final class StubPropertiesFunction implements PropertiesFunction {
        private final Map<String, String> values;
        private final Set<String> failing;
        private final List<String> applied = new ArrayList<>();

        StubPropertiesFunction(Map<String, String> values, Set<String> failing) {
            this.values = values;
            this.failing = failing;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String apply(String remainder) {
            applied.add(remainder);
            if (failing.contains(remainder)) {
                throw new IllegalStateException("cannot resolve " + remainder);
            }
            return values.get(remainder);
        }
    }

    private static final class TestParser extends AbstractEarlyResolutionPropertiesParser {
        private final StubPropertiesFunction function;
        private int factoryCalls;

        TestParser(StubPropertiesFunction function) {
            this.function = function;
        }

        @Override
        protected String getEarlyResolutionProperty() {
            return GUARD;
        }

        @Override
        protected String getOverridePropertySourceName() {
            return OVERRIDE_SOURCE;
        }

        @Override
        protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
            factoryCalls++;
            return function;
        }
    }

    private static ApplicationEnvironmentPreparedEvent eventFor(ConfigurableEnvironment environment) {
        return new ApplicationEnvironmentPreparedEvent(
                new DefaultBootstrapContext(), new SpringApplication(), new String[0], environment);
    }

    /**
     * Accepts {@code Map<String, ?>} so that both {@code Map.of("k", "v")} (which infers
     * {@code Map<String, String>}) and maps holding an OriginTrackedValue can be passed.
     */
    private static RecordingEnvironment environmentWith(Map<String, ?> properties) {
        RecordingEnvironment environment = new RecordingEnvironment();
        environment.getPropertySources()
                .addFirst(new MapPropertySource("test-properties", new LinkedHashMap<>(properties)));
        return environment;
    }

    @Test
    public void guardDisabledDoesNotBuildTheClient() {
        RecordingEnvironment environment = environmentWith(Map.of(
                "my.secret", "{{test:some/secret}}"));
        TestParser parser = new TestParser(new StubPropertiesFunction(Map.of(), Set.of()));

        parser.onApplicationEvent(eventFor(environment));

        assertEquals(0, parser.factoryCalls,
                "building a vault client while early resolution is disabled would abort startup for every "
                        + "application that never opted in");
        assertNull(environment.getPropertySources().get(OVERRIDE_SOURCE),
                "no override property source may be added when early resolution is disabled");
    }

    @Test
    public void guardEnabledBuildsTheClientAndAddsTheOverrideSource() {
        RecordingEnvironment environment = environmentWith(Map.of(GUARD, "true"));
        TestParser parser = new TestParser(new StubPropertiesFunction(Map.of(), Set.of()));

        parser.onApplicationEvent(eventFor(environment));

        assertEquals(1, parser.factoryCalls, "the client is built exactly once per event");
        assertNotNull(environment.getPropertySources().get(OVERRIDE_SOURCE),
                "the override source must be registered so later property resolution sees it");
    }

    @Test
    public void resolvesWholeValuePlaceholders() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                "my.secret", "{{test:database/password}}"));
        TestParser parser = new TestParser(
                new StubPropertiesFunction(Map.of("database/password", "pazzword"), Set.of()));

        parser.onApplicationEvent(eventFor(environment));

        assertEquals("pazzword", environment.getProperty("my.secret"),
                "the resolved value must take effect, because the placeholder text is not a usable secret");
    }

    @Test
    public void leavesNonMatchingValuesAlone() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                "embedded.placeholder", "jdbc://{{test:host}}/db",
                "foreign.prefix", "{{other:host}}",
                "plain.value", "just-a-string"));
        StubPropertiesFunction function = new StubPropertiesFunction(Map.of("host", "resolved-host"), Set.of());
        TestParser parser = new TestParser(function);

        parser.onApplicationEvent(eventFor(environment));

        assertEquals(List.of(), function.applied,
                "early resolution deliberately handles only values that are entirely one placeholder; "
                        + "embedded placeholders belong to Camel's normal property parser");
        assertEquals("jdbc://{{test:host}}/db", environment.getProperty("embedded.placeholder"));
        assertEquals("{{other:host}}", environment.getProperty("foreign.prefix"));
        assertEquals("just-a-string", environment.getProperty("plain.value"));
    }

    @Test
    public void stripsOnlyTheLeadingPrefixAndTrailingDelimiters() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                "my.secret", "{{test:a}}b}}"));
        StubPropertiesFunction function = new StubPropertiesFunction(Map.of("a}}b", "resolved"), Set.of());
        TestParser parser = new TestParser(function);

        parser.onApplicationEvent(eventFor(environment));

        assertEquals(List.of("a}}b"), function.applied,
                "a global replace of the delimiters would corrupt a remainder that itself contains them");
        assertEquals("resolved", environment.getProperty("my.secret"));
    }

    @Test
    public void resolvesOriginTrackedValues() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(GUARD, "true");
        properties.put("my.secret", OriginTrackedValue.of("{{test:database/password}}"));
        RecordingEnvironment environment = environmentWith(properties);
        TestParser parser = new TestParser(
                new StubPropertiesFunction(Map.of("database/password", "pazzword"), Set.of()));

        parser.onApplicationEvent(eventFor(environment));

        assertEquals("pazzword", environment.getProperty("my.secret"),
                "values loaded from application.properties arrive wrapped in OriginTrackedValue");
    }

    @Test
    public void guardDisabledDoesNotReadTheIgnoreFlag() {
        RecordingEnvironment environment = environmentWith(Map.of(
                "my.secret", "{{test:some/secret}}"));
        TestParser parser = new TestParser(new StubPropertiesFunction(Map.of(), Set.of()));

        parser.onApplicationEvent(eventFor(environment));

        assertFalse(environment.queried.contains(
                AbstractEarlyResolutionPropertiesParser.IGNORE_RESOLUTION_FAILURES),
                "the opt-out flag is only meaningful once early resolution is enabled, so it must not be "
                        + "evaluated before the guard");
    }

    @Test
    public void failClosedReportsEveryFailingPropertyTogether() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                "first.secret", "{{test:missing/one}}",
                "second.secret", "{{test:missing/two}}"));
        StubPropertiesFunction function = new StubPropertiesFunction(
                Map.of(), Set.of("missing/one", "missing/two"));
        TestParser parser = new TestParser(function);

        RuntimeCamelException thrown = assertThrows(RuntimeCamelException.class,
                () -> parser.onApplicationEvent(eventFor(environment)));

        assertTrue(thrown.getMessage().contains("first.secret"),
                "the failure must name every property that could not be resolved, was: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("second.secret"),
                "the failure must name every property that could not be resolved, was: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(
                AbstractEarlyResolutionPropertiesParser.IGNORE_RESOLUTION_FAILURES),
                "the failure must point at the opt-out property, was: " + thrown.getMessage());
        assertEquals(2, thrown.getSuppressed().length,
                "each individual failure is attached so its cause survives");
        assertEquals(2, function.applied.size(),
                "aborting on the first failure would hide the remaining broken placeholders from the operator");
    }

    @Test
    public void failClosedDoesNotRegisterAnOverrideSource() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                "my.secret", "{{test:missing}}"));
        TestParser parser = new TestParser(new StubPropertiesFunction(Map.of(), Set.of("missing")));

        assertThrows(RuntimeCamelException.class, () -> parser.onApplicationEvent(eventFor(environment)));

        assertNull(environment.getPropertySources().get(OVERRIDE_SOURCE),
                "a partially populated override source must not be left behind after an aborted startup");
    }

    @Test
    public void ignoreResolutionFailuresKeepsStartupGoing() {
        RecordingEnvironment environment = environmentWith(Map.of(
                GUARD, "true",
                AbstractEarlyResolutionPropertiesParser.IGNORE_RESOLUTION_FAILURES, "true",
                "good.secret", "{{test:present}}",
                "bad.secret", "{{test:missing}}"));
        TestParser parser = new TestParser(
                new StubPropertiesFunction(Map.of("present", "resolved"), Set.of("missing")));

        assertDoesNotThrow(() -> parser.onApplicationEvent(eventFor(environment)));

        assertEquals("resolved", environment.getProperty("good.secret"),
                "a failure elsewhere must not discard the values that did resolve");
        assertEquals("{{test:missing}}", environment.getProperty("bad.secret"),
                "in tolerant mode the unresolved placeholder is deliberately left in place");
    }
}
