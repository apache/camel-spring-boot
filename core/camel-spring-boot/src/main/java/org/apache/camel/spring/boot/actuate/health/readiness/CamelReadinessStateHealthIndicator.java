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
package org.apache.camel.spring.boot.actuate.health.readiness;

import java.util.Collection;
import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.spring.boot.actuate.health.CamelProbesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;

public class CamelReadinessStateHealthIndicator extends ReadinessStateHealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(CamelReadinessStateHealthIndicator.class);

    private final CamelContext camelContext;

    public CamelReadinessStateHealthIndicator(ApplicationAvailability availability, CamelContext camelContext) {
        super(availability);

        this.camelContext = camelContext;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        Collection<HealthCheck.Result> results = HealthCheckHelper.invokeReadiness(camelContext);

        boolean isReady = CamelProbesHelper.checkProbeState(results, LOG);

        return isReady ? ReadinessState.ACCEPTING_TRAFFIC : ReadinessState.REFUSING_TRAFFIC;
    }
}
