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

package org.apache.camel.aws.xray.starter;

import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.component.aws.xray.TraceAnnotatedTracingStrategy;
import org.apache.camel.component.aws.xray.XRayTracer;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@TestPropertySource(locations = "/default.properties")
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamelAwsXRayTest.class, AwsXRayAutoConfiguration.class })
public class CamelAwsXRayTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private XRayTracer tracer;

    @Test
    public void testTraceAnnotatedTracingStrategy() {
        assertTrue(context.hasService(tracer));
        Set<String> excludePatterns = tracer.getExcludePatterns();
        assertTrue(excludePatterns.contains("excludeme"));
        assertTrue(excludePatterns.contains("foobar"));
        assertEquals(2, excludePatterns.size());
        List<InterceptStrategy> interceptStrategies = context.getCamelContextExtension().getInterceptStrategies();
        boolean found = false;
        for (InterceptStrategy is: interceptStrategies) {
            if (is instanceof TraceAnnotatedTracingStrategy) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Tracing Strategy should have been added");
    }
}
