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

import java.util.Collection;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Camel {@link HealthIndicator}.
 */
public class CamelHealthCheckIndicator extends AbstractHealthIndicator {

    private final CamelContext camelContext;

    public CamelHealthCheckIndicator(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.withDetail("name", "camel-health-check");
        builder.up();

        if (camelContext != null) {
            Collection<HealthCheck.Result> results = HealthCheckHelper.invoke(camelContext);

            for (HealthCheck.Result result : results) {
                Map<String, Object> details = result.getDetails();
                boolean enabled = true;

                if (details.containsKey(AbstractHealthCheck.CHECK_ENABLED)) {
                    enabled = (boolean) details.get(AbstractHealthCheck.CHECK_ENABLED);
                }

                if (enabled) {
                    builder.withDetail(result.getCheck().getId(), result.getState().name());
                    if (result.getState() == HealthCheck.State.DOWN) {
                        builder.down();
                    }
                }
            }
        }
    }

}
