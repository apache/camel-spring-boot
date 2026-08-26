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
package org.apache.camel.spring.boot.actuate.health;

import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stack trace of a failing health check is only exposed at the {@code full} exposure level.
 */
public class CamelHealthHelperExposureTest {

    private static final String MARKER = "aMarkerOnlyPresentInTheStackTrace";

    private static final class TestHealthCheck extends AbstractHealthCheck {
        private TestHealthCheck() {
            super("test", "test-check");
        }

        @Override
        protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
            builder.down();
        }
    }

    private static HealthCheck.Result downResultWithError() {
        HealthCheck check = new TestHealthCheck();
        Exception error = new IllegalStateException("connection refused");
        // give the trace a recognisable frame so we can assert on its presence
        error.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(MARKER, "connect", "Broker.java", 42) });
        return HealthCheckResultBuilder.on(check)
                .down()
                .error(error)
                .build();
    }

    @Test
    void fullExposureLevelIncludesTheStackTrace() {
        Health.Builder builder = new Health.Builder();
        CamelHealthHelper.applyHealthDetail(builder, downResultWithError(), "full");

        String rendered = builder.build().getDetails().toString();
        assertTrue(rendered.contains(MARKER), "the full exposure level should carry the stack trace, was: " + rendered);
    }

    @Test
    void defaultExposureLevelOmitsTheStackTrace() {
        Health.Builder builder = new Health.Builder();
        CamelHealthHelper.applyHealthDetail(builder, downResultWithError(), "default");

        Health health = builder.build();
        String rendered = health.getDetails().toString();
        assertFalse(rendered.contains(MARKER),
                "the default exposure level must not carry the stack trace, was: " + rendered);
        assertFalse(rendered.contains("error.stacktrace"),
                "the default exposure level must not carry an error.stacktrace entry, was: " + rendered);
        // the failure is still identifiable
        assertTrue(rendered.contains(IllegalStateException.class.getName()),
                "the exception type should still be reported, was: " + rendered);
    }

    @Test
    void errorMessageIsReportedAtEveryExposureLevel() {
        Health.Builder builder = new Health.Builder();
        CamelHealthHelper.applyHealthDetail(builder, downResultWithError(), "default");

        Object message = builder.build().getDetails().get("error.message");
        assertNotNull(message, "error.message should be reported regardless of exposure level");
        assertEquals("connection refused", message);
    }
}
