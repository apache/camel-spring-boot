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
package org.apache.camel.component.mail.springboot;



import javax.mail.internet.MimeUtility;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jvnet.mock_javamail.Mailbox;


@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MailMimeDecodeHeadersTest.class,
        MailMimeDecodeHeadersTest.TestConfiguration.class
    }
)
public class MailMimeDecodeHeadersTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:decoded")
    MockEndpoint mockDecoded;
    
    @EndpointInject("mock:plain")
    MockEndpoint mockPlain;
    
    private String nonAsciiSubject = "\uD83D\uDC2A rocks!";
    private String encodedNonAsciiSubject = "=?UTF-8?Q?=F0=9F=90=AA_rocks!?=";

    private String longSubject;
    {
        StringBuilder sb = new StringBuilder("Camel rocks!");

        int mimeFoldingLimit = 76;
        while (sb.length() <= mimeFoldingLimit) {
            sb.insert(7, "o");
        }
        longSubject = sb.toString();
    }
    private String foldedLongSubject = MimeUtility.fold(9, longSubject);

    @Test
    public void testLongMailSubject() throws Exception {
        Mailbox.clearAll();
        mockDecoded.reset();
        mockPlain.reset();
        // The email subject is >76 chars and will get MIME folded.
        template.sendBody("direct:longSubject", "");

        // When mimeDecodeHeaders=true is used, expect the received subject to be MIME unfolded.
        
        mockDecoded.expectedMessageCount(1);
        mockDecoded.expectedHeaderReceived("subject", longSubject);
        mockDecoded.setResultWaitTime(10000);
        mockDecoded.assertIsSatisfied();

        // When mimeDecodeHeaders=false or missing, expect the received subject to be MIME folded.
        
        mockPlain.expectedMessageCount(1);
        mockPlain.expectedHeaderReceived("subject", foldedLongSubject);
        mockPlain.setResultWaitTime(10000);
        mockPlain.assertIsSatisfied();
    }

    @Test
    public void testNonAsciiMailSubject() throws Exception {
        Mailbox.clearAll();
        mockDecoded.reset();
        mockPlain.reset();
        // The email subject contains non-ascii characters and will be encoded.
        template.sendBody("direct:nonAsciiSubject", "");

        // When mimeDecodeHeaders=true is used, expect the received subject to be MIME encoded.
        
        mockDecoded.expectedMessageCount(1);
        mockDecoded.expectedHeaderReceived("subject", nonAsciiSubject);
        mockDecoded.assertIsSatisfied();

        // When mimeDecodeHeaders=false or missing, expect the received subject to be MIME encoded.

        mockPlain.expectedMessageCount(1);
        mockPlain.expectedHeaderReceived("subject", encodedNonAsciiSubject);
        mockPlain.assertIsSatisfied();
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
                    from("direct:longSubject")
                            .setHeader("subject", constant(longSubject))
                            .to("smtp://plain@localhost", "smtp://decoded@localhost");

                    from("direct:nonAsciiSubject")
                            .setHeader("subject", constant(nonAsciiSubject))
                            .to("smtp://plain@localhost", "smtp://decoded@localhost");

                    from("pop3://localhost?username=plain&password=secret&initialDelay=100&delay=100")
                            .to("mock:plain");

                    from("pop3://localhost?username=decoded&password=secret&initialDelay=100&delay=100&mimeDecodeHeaders=true")
                            .to("mock:decoded");
                }
            };
        }
    }
    
   

}
