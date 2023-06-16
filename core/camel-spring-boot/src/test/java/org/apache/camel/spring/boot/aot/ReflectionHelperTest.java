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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReflectionHelperTest}.
 */
class ReflectionHelperTest {

    private final AtomicInteger counter = new AtomicInteger();

    @Test
    void shouldAlwaysApply() {
        ReflectionHelper.applyIfMatch(Foo.class, All.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(11);
    }

    @Test
    void shouldNeverApply() {
        ReflectionHelper.applyIfMatch(Foo.class, None.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isZero();
    }

    @Test
    void shouldApplyToClassOnly() {
        ReflectionHelper.applyIfMatch(Foo.class, OnlyType.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldApplyToOneSpecificConstructor() {
        ReflectionHelper.applyIfMatch(Foo.class, OnlyConstructor.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldApplyToOneSpecificField() {
        ReflectionHelper.applyIfMatch(Foo.class, OnlyField.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldApplyToOneSpecificMethod() {
        ReflectionHelper.applyIfMatch(Foo.class, OnlyMethod.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldApplyToSpecificParameters() {
        ReflectionHelper.applyIfMatch(Foo.class, OnlyParameter.class, a -> null, x -> counter.incrementAndGet());
        assertThat(counter.get()).isEqualTo(2);
    }

    @OnlyType
    @All
    public static class Foo {

        @All
        private String someField1;
        @OnlyField
        @All
        private String someField2;

        @All
        private Foo() {}

        @OnlyConstructor
        @All
        private Foo(@OnlyParameter @All String someParam1, @All String someParam2) {}

        @All
        private void someMethod1(@All String someParam1, @All @OnlyParameter String someParam2) {}

        @OnlyMethod
        @All
        private void someMethod2() {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface All {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface None {

    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnlyType {

    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnlyField {

    }

    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnlyConstructor {

    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnlyMethod {

    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnlyParameter {

    }
}
