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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The indicator applies every health check result to one {@link Health.Builder}, so a detail written at the top level
 * is a single slot shared by all of them. Each failing check has to keep its own message addressable.
 */
public class CamelHealthHelperMultipleFailuresTest {

    private static final class TestHealthCheck extends AbstractHealthCheck {
        private TestHealthCheck(String id) {
            super("test", id);
        }

        @Override
        protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
            builder.down();
        }
    }

    private static HealthCheck.Result downResult(String id, Throwable error) {
        return HealthCheckResultBuilder.on(new TestHealthCheck(id)).down().error(error).build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> checkData(Health health, String id) {
        Object data = health.getDetails().get(id + ".data");
        assertNotNull(data, "expected per-check data for " + id + ", details were: " + health.getDetails());
        assertInstanceOf(Map.class, data);
        return (Map<String, String>) data;
    }

    @Test
    public void eachFailingCheckKeepsItsOwnMessage() {
        Health.Builder builder = new Health.Builder();

        CamelHealthHelper.applyHealthDetail(builder,
                downResult("first", new IllegalArgumentException("first-message")), "default");
        CamelHealthHelper.applyHealthDetail(builder,
                downResult("second", new IllegalStateException("second-message")), "default");

        Health health = builder.build();

        assertEquals("first-message", checkData(health, "first").get("error.message"),
                "the first check's message must survive a later failing check");
        assertEquals("second-message", checkData(health, "second").get("error.message"));
    }

    @Test
    public void topLevelErrorMessageIsStillReported() {
        Health.Builder builder = new Health.Builder();

        CamelHealthHelper.applyHealthDetail(builder,
                downResult("only", new IllegalStateException("the-message")), "default");

        assertEquals("the-message", builder.build().getDetails().get("error.message"),
                "the top-level key is kept for compatibility");
    }
}
