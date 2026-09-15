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
package org.apache.camel.component.langchain4j.ingest.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.camel.CamelContext;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngestComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, LangChain4jIngestComponentAutoConfiguration.class },
                 properties = {
                         "camel.component.langchain4j-ingest.max-segment-size = 1000",
                         "camel.component.langchain4j-ingest.max-overlap-size = 100",
                         "camel.component.langchain4j-ingest.document-id-header = MyDocumentId" })
public class LangChain4jIngestComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    public void componentIsAutoConfigured() {
        LangChain4jIngestComponent component = assertInstanceOf(LangChain4jIngestComponent.class,
                context.getComponent("langchain4j-ingest"));

        // set through camel.component.langchain4j-ingest.*, so this asserts the Spring
        // Boot properties are copied onto the component's nested
        // LangChain4jIngestConfiguration by the starter
        assertEquals(1000, component.getConfiguration().getMaxSegmentSize());
        assertEquals(100, component.getConfiguration().getMaxOverlapSize());
        assertEquals("MyDocumentId", component.getConfiguration().getDocumentIdHeader());
    }
}
