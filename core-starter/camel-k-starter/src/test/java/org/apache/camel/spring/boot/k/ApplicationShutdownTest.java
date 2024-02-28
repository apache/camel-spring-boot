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
package org.apache.camel.spring.boot.k;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                ApplicationRoutesAutoConfiguration.class,
                ApplicationShutdownAutoConfiguration.class,
                ApplicationShutdownTest.TestConfiguration.class,
        },
        properties = {
                // TODO: ideally it would be nice if the camel.main.routes-include-pattern would be honoured.
                //       The camel.springboot namespace should ideally be for Spring Boot specific options.
                "camel.springboot.routes-include-pattern=classpath:camel-k/sources/test-route-002.yaml",
                // camel-k
                "camel.k.shutdown.maxMessages=1",
                "camel.k.shutdown.strategy=CAMEL",
                // misc
                "greeted.subject=Joe"
        }
)
@ExtendWith(OutputCaptureExtension.class)
public class ApplicationShutdownTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void testShutdown(CapturedOutput output) throws Exception {

        Logger l = LoggerFactory.getLogger(getClass());

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    assertThat(output).contains("Hello Joe!");
                    assertThat(output).contains("Once done");
                    assertThat(output).contains("Initiate runtime shutdown (max: 1, handled: 2)");
        });
    }

    @Configuration
    public static class TestConfiguration {
    }
}
