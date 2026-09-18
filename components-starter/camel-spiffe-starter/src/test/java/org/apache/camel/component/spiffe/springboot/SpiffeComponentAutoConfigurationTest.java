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
package org.apache.camel.component.spiffe.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.camel.CamelContext;
import org.apache.camel.component.spiffe.SpiffeComponent;
import org.apache.camel.component.spiffe.SpiffeOperation;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, SpiffeComponentAutoConfiguration.class },
                 properties = {
                         "camel.component.spiffe.audience = demo-audience",
                         "camel.component.spiffe.operation = fetchJwtSvid",
                         "camel.component.spiffe.spiffe-socket-path = unix:///tmp/agent.sock" })
public class SpiffeComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    public void componentIsAutoConfigured() {
        SpiffeComponent component = assertInstanceOf(SpiffeComponent.class, context.getComponent("spiffe"));

        // set through camel.component.spiffe.*, so this asserts the Spring Boot
        // properties are copied onto the component's nested SpiffeConfiguration by
        // the starter
        assertEquals("demo-audience", component.getConfiguration().getAudience());
        assertEquals(SpiffeOperation.fetchJwtSvid, component.getConfiguration().getOperation());
        assertEquals("unix:///tmp/agent.sock", component.getConfiguration().getSpiffeSocketPath());
    }
}
