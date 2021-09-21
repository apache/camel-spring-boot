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
package org.apache.camel.spring.boot;

import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest
public class PlainTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired(required = false)
    CamelContext camelContext;

    // turn off Camel auto configuration which should not inject a CamelContext
    @Configuration
    @EnableAutoConfiguration(exclude = CamelAutoConfiguration.class)
    public static class MyConfiguration {
        // empty
    }

    @Test
    public void testPlain() {
        Assertions.assertNull(camelContext, "Should not auto configure CamelContext");
        Assertions.assertEquals(0, applicationContext.getBeanNamesForType(CamelAutoConfiguration.class).length,
                "Should not auto configure CamelAutoConfiguration");
    }
}
