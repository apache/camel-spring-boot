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
package org.apache.camel.component.file.remote.springboot.sftp;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.springboot.ftp.BaseFtp;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.FileUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SftpUseListFalseTest.class
        }
)
//Based on SftpUseListFalseIT
public class SftpUseListFalseTest extends BaseSftp {

    @EndpointInject("mock:result")
    private MockEndpoint mock;

    @Test
    public void testSftpUseListFalse() throws Exception {
        String expected = "Hello World";

        // create file using regular file
        template.sendBodyAndHeader("file://" + service.getFtpRootDir(), expected, Exchange.FILE_NAME, "report.txt");

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(Exchange.FILE_NAME, "report.txt");
        mock.expectedBodiesReceived(expected);

        context.getRouteController().startRoute("foo");

        assertMockEndpointsSatisfied();
    }

    @BeforeEach
    public void addRoute() throws Exception {
        context.addRoutes(new TestConfiguration().routeBuilder());
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
                    from("sftp://localhost:" + getPort() + "/" + getRootDir()
                            + "?username=admin&password=admin&delay=10000&disconnect=true&stepwise=false&useList=false&fileName=report.txt&delete=true")
                            .routeId("foo").noAutoStartup()
                            .to("mock:result");
                }
            };
        }
    }
}
