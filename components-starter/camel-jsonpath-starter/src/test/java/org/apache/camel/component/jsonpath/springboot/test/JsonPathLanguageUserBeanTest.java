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
package org.apache.camel.component.jsonpath.springboot.test;

import org.apache.camel.CamelContext;
import org.apache.camel.jsonpath.JsonPathLanguage;
import org.apache.camel.jsonpath.springboot.JsonPathLanguageAutoConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The starter customizer is applied to a language the application registered itself. Only the options configured
 * through Spring Boot properties may be copied onto it, a catalog default must not overwrite what was set in code.
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, JsonPathLanguageAutoConfiguration.class,
        JsonPathLanguageUserBeanTest.TestConfiguration.class },
                properties = { "camel.language.jsonpath.write-as-string = true" })
public class JsonPathLanguageUserBeanTest {

    @Autowired
    CamelContext context;

    @Test
    public void testCatalogDefaultDoesNotOverwriteUserBean() {
        JsonPathLanguage jsonpath = (JsonPathLanguage) context.resolveLanguage("jsonpath");

        // set in code and never configured through properties, so the catalog default (false) must not win
        assertTrue(jsonpath.isSuppressExceptions());
        // configured through properties, so it is applied
        assertTrue(jsonpath.isWriteAsString());
    }

    @Configuration
    public static class TestConfiguration {

        @Bean("jsonpath")
        public JsonPathLanguage jsonpath() {
            JsonPathLanguage jsonpath = new JsonPathLanguage();
            jsonpath.setSuppressExceptions(true);
            return jsonpath;
        }
    }
}
