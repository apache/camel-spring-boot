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
package org.apache.camel.component.ai.tool.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, AiToolComponentConverter.class,
        AiToolComponentAutoConfiguration.class, AiToolUnsetHintsAutoConfiguration.class }, properties = {
                "camel.component.ai-tool.read-only-hint = true",
                "camel.component.ai-tool.destructive-hint = false" })
public class AiToolComponentExplicitHintsAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    public void explicitHintsAreStillCopiedOntoTheComponent() {
        AiToolComponent component = assertInstanceOf(AiToolComponent.class, context.getComponent("ai-tool"));
        assertEquals(Boolean.TRUE, component.getConfiguration().getReadOnlyHint());
        // an explicit false is a real value and must be kept
        assertEquals(Boolean.FALSE, component.getConfiguration().getDestructiveHint());
        // hints that were not configured stay unset
        assertNull(component.getConfiguration().getIdempotentHint());
        assertNull(component.getConfiguration().getOpenWorldHint());
        assertNull(component.getConfiguration().getReturnDirect());
    }
}
