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
package org.apache.camel.component.file.remote.springboot.ftp;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FromFtpMoveFileRecursiveTest.class,
                FromFtpMoveFileRecursiveTest.TestConfiguration.class
        }
)
// Based on FromFtpMoveFileRecursiveIT
public class FromFtpMoveFileRecursiveTest extends BaseFtp {

    @EndpointInject("mock:result")
    private MockEndpoint result;

    protected String getFtpUrl() {
        return "ftp://admin@localhost:" + getPort() + "/movefile?password=admin&recursive=true&binary=false"
               + "&move=.done/${file:name}.old&initialDelay=2500&delay=5000";
    }

    @BeforeEach
    public void setUp() throws Exception {
        prepareFtpServer();
    }

    @Test
    public void testPollFileAndShouldBeMoved() throws Exception {
        result.expectedBodiesReceivedInAnyOrder("Hello", "Bye", "Goodday");
        result.expectedFileExists(ftpFile("movefile/.done/hello.txt.old"));
        result.expectedFileExists(ftpFile("movefile/.done/bye/bye.txt.old"));
        result.expectedFileExists(ftpFile("movefile/.done/goodday/goodday.txt.old"));

        result.assertIsSatisfied();
    }

    private void prepareFtpServer() {
        sendFile(getFtpUrl(), "Hello", "hello.txt");
        sendFile(getFtpUrl(), "Bye", "bye/bye.txt");
        sendFile(getFtpUrl(), "Goodday", "goodday/goodday.txt");
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseFtp.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(getFtpUrl()).to("mock:result");
                }
            };
        }
    }
}
