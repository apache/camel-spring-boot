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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the stack trace of a failing health check is only exposed in the full exposure level.
 */
public class CamelHealthHelperTest {

    private static final String MY_CHECK_ID = "my-check";

    @Test
    public void defaultExposureLevelShouldNotIncludeStackTrace() {
        Health health = applyDownResult("default");

        assertEquals("Cannot connect to broker", health.getDetails().get("error.message"));
        Map<String, String> data = data(health);
        assertEquals("my-route", data.get("route.id"));
        assertFalse(data.containsKey("error.stacktrace"), "Stack trace should not be exposed at default level");
    }

    @Test
    public void fullExposureLevelShouldIncludeStackTrace() {
        Health health = applyDownResult("full");

        assertEquals("Cannot connect to broker", health.getDetails().get("error.message"));
        Map<String, String> data = data(health);
        assertEquals("my-route", data.get("route.id"));
        String stackTrace = data.get("error.stacktrace");
        assertTrue(stackTrace != null && stackTrace.contains("Cannot connect to broker"),
                "Stack trace should be exposed at full level");
        assertTrue(stackTrace.contains(CamelHealthHelperTest.class.getName()), "Stack trace should contain the frames");
    }

    @Test
    public void onelineExposureLevelShouldNotIncludeAnyDetail() {
        Health health = applyDownResult("oneline");

        assertNull(health.getDetails().get("error.message"));
        assertNull(health.getDetails().get(MY_CHECK_ID + ".data"));
    }

    private static Health applyDownResult(String exposureLevel) {
        HealthCheck check = new MyHealthCheck();
        HealthCheck.Result result = HealthCheckResultBuilder.on(check)
                .down()
                .error(new IllegalStateException("Cannot connect to broker"))
                .detail("route.id", "my-route")
                .build();

        Health.Builder builder = new Health.Builder();
        CamelHealthHelper.applyHealthDetail(builder, result, exposureLevel);
        return builder.down().build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> data(Health health) {
        Object data = health.getDetails().get(MY_CHECK_ID + ".data");
        assertInstanceOf(Map.class, data, "Expected health check data to be present");
        return (Map<String, String>) data;
    }

    private static final class MyHealthCheck extends AbstractHealthCheck {

        private MyHealthCheck() {
            super(MY_CHECK_ID);
        }

        @Override
        protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
            builder.down();
        }
    }

}
