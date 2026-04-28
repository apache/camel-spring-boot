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
package org.apache.camel.component.kafka.springboot;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Binder behavior with different key formats.
 * This is a regression test ensuring our per-property binding approach
 * in {@link SpringKafkaPropertiesAutoConfiguration} works correctly,
 * since Binder does NOT normalize camelCase keys to kebab-case in map bindings.
 */
class BinderKeyFormatTest {

    @Test
    void binderPerPropertyBindHandlesCamelCase() {
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "camel.component.kafka.sslKeystoreLocation", "/path/to/keystore"
        )));

        Binder binder = Binder.get(env);

        // Per-property bind with kebab-case name finds camelCase source (relaxed binding)
        assertThat(binder.bind("camel.component.kafka.ssl-keystore-location", Bindable.of(String.class)).isBound())
                .isTrue();
    }

    @Test
    void binderPerPropertyBindHandlesKebabCase() {
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "camel.component.kafka.ssl-keystore-location", "/path/to/keystore"
        )));

        Binder binder = Binder.get(env);

        assertThat(binder.bind("camel.component.kafka.ssl-keystore-location", Bindable.of(String.class)).isBound())
                .isTrue();
    }
}
