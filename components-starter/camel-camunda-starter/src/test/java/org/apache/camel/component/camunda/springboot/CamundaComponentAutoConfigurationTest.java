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
package org.apache.camel.component.camunda.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.camel.CamelContext;
import org.apache.camel.component.camunda.CamundaComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamundaComponentAutoConfiguration.class },
                 properties = {
                         "camel.component.camunda.grpc-address = http://localhost:26500",
                         "camel.component.camunda.rest-address = http://localhost:8080",
                         "camel.component.camunda.client-id = demo-client",
                         "camel.component.camunda.region = demo-region" })
public class CamundaComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    public void componentIsAutoConfigured() {
        CamundaComponent component = assertInstanceOf(CamundaComponent.class, context.getComponent("camunda"));

        // set through camel.component.camunda.*, so this asserts the Spring Boot
        // properties are copied onto the component by the starter
        assertEquals("http://localhost:26500", component.getGrpcAddress());
        assertEquals("http://localhost:8080", component.getRestAddress());
        assertEquals("demo-client", component.getClientId());
        assertEquals("demo-region", component.getRegion());
    }
}
