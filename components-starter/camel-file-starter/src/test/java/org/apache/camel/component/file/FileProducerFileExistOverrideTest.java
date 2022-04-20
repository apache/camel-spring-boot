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
package org.apache.camel.component.file;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileProducerFileExistOverrideTest.class,
                FileProducerFileExistOverrideTest.TestConfiguration.class
        }
)
public class FileProducerFileExistOverrideTest extends BaseFile {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;


    @Test
    public void testOverride() throws Exception {
        resultEndpoint.expectedBodiesReceived("Bye World");
        resultEndpoint.expectedFileExists(testFile("hello.txt"), "Bye World");

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, "hello.txt");
        template.sendBodyAndHeader(fileUri("?fileExist=Override"), "Bye World", Exchange.FILE_NAME, "hello.txt");

        context.getRouteController().startAllRoutes();

        assertMockEndpointsSatisfied();
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
                    from(fileUri("?noop=true&initialDelay=0&delay=10")).noAutoStartup().convertBodyTo(String.class)
                            .to("mock:result");
                }
            };
        }
    }

}
