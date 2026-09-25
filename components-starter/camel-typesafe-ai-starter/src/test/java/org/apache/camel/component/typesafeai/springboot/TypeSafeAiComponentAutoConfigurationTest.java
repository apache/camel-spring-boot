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
package org.apache.camel.component.typesafeai.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.typesafeai.TypeSafeAiComponent;
import org.apache.camel.language.typesafeai.TypeSafeAiLanguage;
import org.apache.camel.language.typesafeai.springboot.TypeSafeAiLanguageAutoConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, TypeSafeAiComponentAutoConfiguration.class,
        TypeSafeAiLanguageAutoConfiguration.class }, properties = {
                "camel.component.typesafe-ai.base-url=http://127.0.0.1:8000",
                "camel.component.typesafe-ai.model=local-model",
                "camel.component.typesafe-ai.api-key=test-key",
                "camel.component.typesafe-ai.threshold=0.75",
                "camel.language.typesafe-ai.endpoint=direct:ai-test",
                "camel.language.typesafe-ai.threshold=0.85",
                "camel.language.typesafe-ai.uncertainty=0.15",
                "camel.language.typesafe-ai.state=active" })
class TypeSafeAiComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    void componentAndLanguageAreAutoConfigured() {
        TypeSafeAiComponent component = assertInstanceOf(TypeSafeAiComponent.class,
                context.getComponent("typesafe-ai"));
        assertEquals("http://127.0.0.1:8000", component.getConfiguration().getBaseUrl());
        assertEquals("local-model", component.getConfiguration().getModel());
        assertEquals("test-key", component.getConfiguration().getApiKey());
        assertEquals(0.75, component.getConfiguration().getThreshold());

        TypeSafeAiLanguage language = assertInstanceOf(TypeSafeAiLanguage.class, context.resolveLanguage("typesafe-ai"));
        assertEquals("direct:ai-test", language.getEndpoint());
        assertEquals(0.85, language.getThreshold());
        assertEquals(0.15, language.getUncertainty());
        assertEquals("active", language.getState());
    }
}
