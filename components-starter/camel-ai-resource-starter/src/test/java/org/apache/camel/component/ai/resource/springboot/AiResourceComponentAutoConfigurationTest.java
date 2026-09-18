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
package org.apache.camel.component.ai.resource.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.resource.AiResourceComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, AiResourceComponentAutoConfiguration.class },
                 properties = {
                         "camel.component.ai-resource.mime-type = application/json",
                         "camel.component.ai-resource.tags = demo" })
public class AiResourceComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    public void componentIsAutoConfigured() {
        AiResourceComponent component
                = assertInstanceOf(AiResourceComponent.class, context.getComponent("ai-resource"));

        // set through camel.component.ai-resource.*, so this asserts the Spring Boot
        // properties are copied onto the component's nested AiResourceConfiguration by
        // the starter
        assertEquals("application/json", component.getConfiguration().getMimeType());
        assertEquals("demo", component.getConfiguration().getTags());
    }
}
