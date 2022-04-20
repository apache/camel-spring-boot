package org.apache.camel.component.file;/*
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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileConsumerPreMoveTest.class,
                FileConsumerPreMoveTest.TestConfiguration.class
        }
)
public class FileConsumerPreMoveTest extends BaseFile {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @AfterEach
    public void reset() {
        resultEndpoint.reset();
    }

    @Test
    public void testPreMove() throws Exception {
        resultEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, "hello.txt");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPreMoveSameFileTwice() throws Exception {
        resultEndpoint.expectedBodiesReceived("Hello World");

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, "hello.txt");

        assertMockEndpointsSatisfied();
        oneExchangeDone.matchesWaitTime();

        // reset and drop the same file again
        resultEndpoint.reset();
        resultEndpoint.expectedBodiesReceived("Hello Again World");

        template.sendBodyAndHeader(fileUri(), "Hello Again World", Exchange.FILE_NAME, "hello.txt");
        assertMockEndpointsSatisfied();
    }

    public static class MyPreMoveCheckerProcessor implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            Class<?> cl = getClass();
            while (cl.getEnclosingClass() != null) {
                cl = cl.getEnclosingClass();
            }
            Path file = testDirectory(cl, false).resolve("work/work-hello.txt");
            assertTrue(Files.exists(file), "Pre move file should exist");
        }
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUri("?preMove=work/work-${file:name}&initialDelay=0&delay=10"))
                            .process(new MyPreMoveCheckerProcessor()).to("mock:result");
                }
            };
        }
    }
}
