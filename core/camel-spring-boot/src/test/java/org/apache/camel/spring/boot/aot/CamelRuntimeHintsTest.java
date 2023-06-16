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
package org.apache.camel.spring.boot.aot;

import org.apache.camel.CamelContext;
import org.apache.camel.language.simple.SimpleLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CamelRuntimeHints}.
 */
class CamelRuntimeHintsTest {

    private final RuntimeHints hints = new RuntimeHints();

    @BeforeEach
    void init() {
        new CamelRuntimeHints().registerHints(hints, getClass().getClassLoader());
    }

    @Test
    void shouldRegisterHintsForCamelServices() throws Exception {
        assertThat(RuntimeHintsPredicates.resource().forResource("META-INF/services/org/apache/camel/language/simple")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onConstructor(SimpleLanguage.class.getConstructor())).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethod(SimpleLanguage.class.getMethod("init"))).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethod(SimpleLanguage.class.getMethod("setCamelContext", CamelContext.class))).accepts(hints);
    }

    @Test
    void shouldRegisterHintsForCamelCatalog() {
        assertThat(RuntimeHintsPredicates.resource().forResource("org/apache/camel/main/components.properties")).accepts(hints);
    }
}
