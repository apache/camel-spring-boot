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
package org.apache.camel.service.lra.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.service.lra.LRASagaService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, LraServiceAutoConfiguration.class },
                properties = {
                        "camel.lra.enabled = true",
                        "camel.lra.coordinator-url = http://lra-coordinator:8080",
                        "camel.lra.local-participant-url = http://localhost:8080",
                        "camel.lra.allowed-coordinator-urls = http://lra-coordinator.lra.svc:8080/lra-coordinator",
                        "camel.lra.callback-secret = a-callback-secret" })
class LraServiceAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Autowired
    LRASagaService sagaService;

    @Test
    void optionsAreSetOnTheLraSagaService() {
        assertEquals("http://lra-coordinator:8080", sagaService.getCoordinatorUrl());
        assertEquals("http://localhost:8080", sagaService.getLocalParticipantUrl());
        assertEquals("http://lra-coordinator.lra.svc:8080/lra-coordinator", sagaService.getAllowedCoordinatorUrls());
        assertEquals("a-callback-secret", sagaService.getCallbackSecret());
        assertTrue(context.hasService(sagaService), "the LRA saga service must be added to the Camel context");
    }
}
