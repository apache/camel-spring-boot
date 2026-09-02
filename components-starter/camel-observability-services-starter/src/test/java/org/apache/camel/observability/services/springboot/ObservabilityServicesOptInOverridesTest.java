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
package org.apache.camel.observability.services.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the wider settings the starter no longer ships by default can be opted back into, which is what
 * a deployment reached over the pod network has to do.
 */
@SpringBootTest(classes = ObservabilityServicesTestApplication.class,
                properties = {
                        "management.server.address=0.0.0.0",
                        "management.endpoint.health.show-details=always",
                        "camel.health.exposure-level=full" })
public class ObservabilityServicesOptInOverridesTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void bindAddressCanBeWidened() {
        assertEquals("0.0.0.0", environment.getProperty("management.server.address"));
    }

    @Test
    public void healthDetailsCanBeAlwaysShown() {
        assertEquals("always", environment.getProperty("management.endpoint.health.show-details"));
    }

    @Test
    public void camelHealthExposureLevelCanBeSetToFull() {
        assertEquals("full", environment.getProperty("camel.health.exposure-level"));
    }
}
