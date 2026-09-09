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
package org.apache.camel.component.http.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
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
 * The starter customizer is applied to a component the application registered itself. Only the options configured
 * through Spring Boot properties may be copied onto it, a catalog default must not overwrite what was set in code.
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, HttpComponentConverter.class,
        HttpComponentAutoConfiguration.class, HttpComponentUserBeanTest.TestConfiguration.class },
                properties = { "camel.component.http.log-http-activity = true" })
public class HttpComponentUserBeanTest {

    @Autowired
    CamelContext context;

    @Test
    public void testCatalogDefaultDoesNotOverwriteUserBean() {
        HttpComponent http = context.getComponent("http", HttpComponent.class);

        // set in code and never configured through properties, so the catalog default (false) must not win
        assertTrue(http.isSkipRequestHeaders());
        // configured through properties, so it is applied
        assertTrue(http.isLogHttpActivity());
    }

    @Configuration
    public static class TestConfiguration {

        @Bean("http")
        public HttpComponent http() {
            HttpComponent http = new HttpComponent();
            http.setSkipRequestHeaders(true);
            return http;
        }
    }
}
