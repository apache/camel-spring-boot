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
package org.apache.camel.xml.jaxb.springboot;

import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JAXBRuntimeHints}.
 */
class JAXBRuntimeHintsTest {
    private final RuntimeHints hints = new RuntimeHints();

    @BeforeEach
    void init() {
        new JAXBRuntimeHints().registerHints(hints, getClass().getClassLoader());
    }

    @Test
    void shouldRegisterHintsForJAXB() throws Exception {
        assertThat(RuntimeHintsPredicates.resource().forResource("jakarta/xml/bind/Messages.properties")).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("org/apache/camel/spring/boot/aot/jaxb.index")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Book.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethod(Book.class, "getName")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(IdentifiedType.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethod(IdentifiedType.class, "setId")).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("org/apache/camel/core/xml/util/jsse/jaxb.index")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(BeanScope.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethod("org.glassfish.jaxb.core.v2.model.nav.ReflectionNavigator", "getInstance")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(USAddress.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(CollapsedStringAdapter.class)).accepts(hints);
    }
}
