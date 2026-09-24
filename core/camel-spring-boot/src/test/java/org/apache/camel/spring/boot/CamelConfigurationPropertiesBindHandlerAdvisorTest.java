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

import java.util.Map;

import com.example.springboot.ThirdPartySettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CamelConfigurationPropertiesBindHandlerAdvisorTest {

    private static final ObjectMapper MY_MAPPER = new ObjectMapper();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CamelAutoConfiguration.class))
            .withUserConfiguration(DummyConfiguration.class);

    @Test
    public void nonEnumerableSourceDoesNotBindIntoThirdPartyTypes() {
        // without the advisor Spring Boot binds into the internals of the ObjectMapper, in case the source has a
        // property there, and fails
        runner.withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addLast(nonEnumerable(Map.of())))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    DummyComponentConfiguration config = ctx.getBean(DummyComponentConfiguration.class);
                    assertNull(config.getObjectMapper());
                    assertNull(config.getConfig().getObjectMapper());
                });
    }

    @Test
    public void nonEnumerableSourceStillBindsConfiguredOptions() {
        Map<String, Object> values = Map.of(
                "camel.component.dummy.object-mapper", "myMapper",
                "camel.component.dummy.config.name", "foo",
                "camel.component.dummy.config.object-mapper", "myMapper");
        runner.withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addLast(nonEnumerable(values)))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    DummyComponentConfiguration config = ctx.getBean(DummyComponentConfiguration.class);
                    assertSame(MY_MAPPER, config.getObjectMapper());
                    assertEquals("foo", config.getConfig().getName());
                    assertSame(MY_MAPPER, config.getConfig().getObjectMapper());
                });
    }

    @Test
    public void propertiesBelowThirdPartyTypesAreStillBound() {
        runner.withPropertyValues("camel.component.dummy.settings.name=foo")
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    assertEquals("foo", ctx.getBean(DummyComponentConfiguration.class).getSettings().getName());
                });
    }

    private static PropertySource<Object> nonEnumerable(Map<String, Object> values) {
        return new PropertySource<>("nonEnumerable") {
            @Override
            public Object getProperty(String name) {
                return values.get(name);
            }
        };
    }

    @Configuration
    @EnableConfigurationProperties(DummyComponentConfiguration.class)
    static class DummyConfiguration {
        @Bean
        @ConfigurationPropertiesBinding
        static ObjectMapperConverter objectMapperConverter() {
            return new ObjectMapperConverter();
        }
    }

    static class ObjectMapperConverter implements Converter<String, ObjectMapper> {
        @Override
        public ObjectMapper convert(String source) {
            return "myMapper".equals(source) ? MY_MAPPER : null;
        }
    }

    @ConfigurationProperties(prefix = "camel.component.dummy")
    public static class DummyComponentConfiguration {
        private ObjectMapper objectMapper;
        private ThirdPartySettings settings;
        private DummyEndpointConfig config = new DummyEndpointConfig();

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public void setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public ThirdPartySettings getSettings() {
            return settings;
        }

        public void setSettings(ThirdPartySettings settings) {
            this.settings = settings;
        }

        public DummyEndpointConfig getConfig() {
            return config;
        }

        public void setConfig(DummyEndpointConfig config) {
            this.config = config;
        }
    }

    public static class DummyEndpointConfig {
        private String name;
        private ObjectMapper objectMapper;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public void setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
    }
}
