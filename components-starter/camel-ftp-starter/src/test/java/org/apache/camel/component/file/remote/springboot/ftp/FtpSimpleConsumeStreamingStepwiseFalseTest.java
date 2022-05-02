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
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FtpSimpleConsumeStreamingStepwiseFalseTest.class,
                FtpSimpleConsumeStreamingStepwiseFalseTest.TestConfiguration.class
        }
)
public class FtpSimpleConsumeStreamingStepwiseFalseTest extends BaseFtp {

    @EndpointInject("mock:result")
    MockEndpoint mock;

    boolean isStepwise() {
        return false;
    }

    @Test
    public void testFtpSimpleConsumeAbsolute() throws Exception {
        String expected = "Hello World";

        String path = ftpFile("tmp/mytemp").toString();
        template.sendBodyAndHeader("file:" + path, expected, Exchange.FILE_NAME, "hello.txt");

        configureMock();

        assertMockEndpointsSatisfied();
        assertMore(mock);
    }

    void configureMock() {
        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(Exchange.FILE_NAME, "hello.txt");
    }

    void assertMore(MockEndpoint mock) {
        GenericFile<?> remoteFile = (GenericFile<?>) mock.getExchanges().get(0).getIn().getBody();
        assertTrue(remoteFile.getBody() instanceof InputStream);
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
                    from("ftp://localhost:" + getPort() 
                            + "/tmp/mytemp?username=admin&password=admin&delay=10000&disconnect=true&streamDownload=true&stepwise="
                            + isStepwise())
                            .to("mock:result");
                }
            };
        }
    }
}
