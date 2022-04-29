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




import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        FromFileSendMailTest.class,
        FromFileSendMailTest.TestConfiguration.class
    }
)
public class FromFileSendMailTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint result;
    
    @Test
    public void testSendFileAsMail() throws Exception {
        Mailbox.clearAll();

        
        result.expectedMessageCount(1);
        result.message(0).body().isInstanceOf(GenericFile.class);

        template.sendBodyAndHeader("file://target/mailtext", "Hi how are you", Exchange.FILE_NAME, "mail.txt");

        result.assertIsSatisfied();

        Mailbox mailbox = Mailbox.get("james@localhost");
        assertEquals(1, mailbox.size());
        Object body = mailbox.get(0).getContent();
        assertEquals("Hi how are you", body);
        Object subject = mailbox.get(0).getSubject();
        assertEquals("Hello World", subject);
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
                    from("file://target/mailtext?initialDelay=100&delay=100")
                            .setHeader("Subject", constant("Hello World"))
                            .setHeader("To", constant("james@localhost"))
                            .setHeader("From", constant("claus@localhost"))
                            .to("smtp://localhost?password=secret&username=claus&initialDelay=100&delay=100", "mock:result");
                }
            };
        }
    }
    
   

}
