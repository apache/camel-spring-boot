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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FtpSoTimeoutTest.class,
                FtpSoTimeoutTest.TestConfiguration.class
        }
)
//Based on FtpSoTimeoutTest
public class FtpSoTimeoutTest {

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate template;

    @AfterEach
    public void after() throws IOException {
        context.getRegistry().findSingleByType(ServerSocket.class).close();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWithDefaultTimeout() {
        assertThrows(CamelExecutionException.class, () -> {
            // send exchange to the route using the custom FTPClient (with a
            // default timeout)
            // the soTimeout triggers in time and test is successful
            template.sendBody("direct:with", "");
        });
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWithoutDefaultTimeout() {
        assertThrows(CamelExecutionException.class, () -> {
            // send exchange to the route using the default FTPClient (without a
            // default timeout)
            // the soTimeout never triggers and test fails after its own timeout
            template.sendBody("direct:without", "");
        });
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseFtp.TestConfiguration {

        @Bean
        public ServerSocket createServerSocket() throws IOException {
            return new ServerSocket(0);
        }
        @Bean
        public RouteBuilder routeBuilder(ServerSocket serverSocket) {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:with").to("ftp://localhost:" + serverSocket.getLocalPort()
                            + "?ftpClient=#myftpclient&connectTimeout=300&soTimeout=300&reconnectDelay=100");

                    from("direct:without").to("ftp://localhost:" + serverSocket.getLocalPort()
                            + "?connectTimeout=300&soTimeout=300&reconnectDelay=100");
                }
            };
        }

        @Bean(value = "myftpclient")
        public FTPClient createFtpClient() {
            FTPClient ftpClient = new FTPClient();
            ftpClient.setDefaultTimeout(300);
            return ftpClient;
        }
    }
}
