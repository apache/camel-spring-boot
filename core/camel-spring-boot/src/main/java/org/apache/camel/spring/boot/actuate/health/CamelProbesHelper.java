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
import java.util.stream.Collectors;

import org.apache.camel.health.HealthCheck;
import org.slf4j.Logger;

public final class CamelProbesHelper {

    private CamelProbesHelper() {
    }

    public static boolean checkProbeState(Collection<HealthCheck.Result> results, Logger log) {
        boolean isUp = true;
        for (HealthCheck.Result result : results) {
            if (!HealthCheck.State.UP.equals(result.getState())) {
                isUp = false;

                log.warn("Probe in group '{}', with id '{}' failed with message '{}'", result.getCheck().getGroup(),
                        result.getCheck().getId(), result.getMessage().orElse(""));
                result.getError().ifPresent(error -> log.warn(error.getMessage(), error));
                log.debug("Probe in group '{}', with id '{}' failed with message '{}' details: \n {}", result.getCheck().getGroup(),
                        result.getMessage().orElse(""), result.getCheck().getId(), result.getDetails().entrySet().stream()
                                .map(x -> x.getKey() + ": " + x.getValue())
                                .collect(Collectors.joining("\n")));
            }
        }
        return isUp;
    }
}
