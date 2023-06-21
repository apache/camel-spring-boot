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
package org.apache.camel.component.file.remote.springboot.ftps;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.springboot.ftp.BaseFtp;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileToFtpsWithCustomTrustStorePropertiesTest.class
        }
)
//based on FileToFtpsWithCustomTrustStorePropertiesIT
public class FileToFtpsWithCustomTrustStorePropertiesTest extends BaseFtpsClientAuth {

    @EndpointInject("mock:result")
    private MockEndpoint result;

    private String getFtpUrl() {
        return "ftps://admin@localhost:" + getPort()
               + "/tmp2/camel?password=admin&initialDelay=2000&disableSecureDataChannelDefaults=true"
               + "&securityProtocol=TLSv1.3&implicit=false&ftpClient.trustStore.file=./src/test/resources/server.jks&ftpClient.trustStore.type=PKCS12"
               + "&ftpClient.trustStore.algorithm=SunX509&ftpClient.trustStore.password=password&delete=true";

    }

    @Test
    public void testFromFileToFtp() throws Exception {

        result.expectedMessageCount(2);

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
                    from("file:src/test/data?noop=true").log("Got ${file:name}").to(getFtpUrl());

                    from(getFtpUrl()).to("mock:result");
                }
            };
        }
    }
}
