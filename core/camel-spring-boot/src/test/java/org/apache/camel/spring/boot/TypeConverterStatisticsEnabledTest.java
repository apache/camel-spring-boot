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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The type converter registry is created when the CamelContext is built, and its statistics cannot be enabled
 * afterwards, so camel.main.type-converter-statistics-enabled must be applied before the build (CAMEL-27103).
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class }, properties = {
        "camel.main.type-converter-statistics-enabled = true" })
public class TypeConverterStatisticsEnabledTest {

    @Autowired
    private CamelContext context;

    @Test
    public void testTypeConverterStatisticsEnabled() {
        assertTrue(context.isTypeConverterStatisticsEnabled());

        TypeConverterRegistry.Statistics statistics = context.getTypeConverterRegistry().getStatistics();
        long before = statistics.getAttemptCounter();

        assertEquals(123, context.getTypeConverter().convertTo(int.class, "123"));

        assertEquals(before + 1, statistics.getAttemptCounter());
    }
}
