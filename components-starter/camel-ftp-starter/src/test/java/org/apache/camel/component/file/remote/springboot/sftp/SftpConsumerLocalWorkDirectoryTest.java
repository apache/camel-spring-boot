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

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.springboot.ftp.BaseFtp;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
                SftpConsumerLocalWorkDirectoryTest.class
        }
)
//Based on SftpConsumerLocalWorkDirectoryIT
public class SftpConsumerLocalWorkDirectoryTest extends BaseSftp {

    @EndpointInject("mock:result")
    private MockEndpoint mock;

    protected String getFtpUrl() {
        return "sftp://localhost:" + getPort() + "/" + getRootDir() + "/?password=admin"
               + "&localWorkDirectory=" + ftpFile("lwd")
               + "&noop=true";
    }

    @BeforeEach
    public void setUp() throws Exception {
        prepareFtpServer();
    }

    private void prepareFtpServer() throws Exception {
        // prepares the FTP Server by creating a file on the server that we want
        // to unit test that we can pool
        Endpoint endpoint = context.getEndpoint(getFtpUrl());
        Exchange exchange = endpoint.createExchange();
        exchange.getIn().setBody("Hello World");
        exchange.getIn().setHeader(Exchange.FILE_NAME, "hello.txt");
        Producer producer = endpoint.createProducer();
        producer.start();
        producer.process(exchange);
        producer.stop();
    }

    @Test
    public void testLocalWorkDirectory() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();

        mock.expectedBodiesReceived("Hello World");
        mock.expectedMessageCount(1);

        context.getRouteController().startRoute("myRoute");

        assertMockEndpointsSatisfied();

        assertMockEndpointsSatisfied();
        assertTrue(notify.matchesWaitTime());

        // and the out file should exists
        assertFileExists(ftpFile("out/hello.txt"), "Hello World");

        // now the lwd file should be deleted
        assertFileNotExists(ftpFile("lwd/hello.txt"));
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
                    from(getFtpUrl()).routeId("myRoute").noAutoStartup().process(new Processor() {
                        public void process(Exchange exchange) {
                            File body = exchange.getIn().getBody(File.class);
                            assertNotNull(body);
                            assertTrue(body.exists(), "Local work file should exists");
                            assertEquals(FileUtil.normalizePath(ftpFile("lwd/hello.txt").toString()), body.getPath());
                        }
                    }).to("mock:result", "file:" + ftpFile("out"));
                }
            };
        }
    }

}
