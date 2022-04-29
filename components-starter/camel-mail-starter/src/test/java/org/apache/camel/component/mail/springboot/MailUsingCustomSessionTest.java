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



import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        MailUsingCustomSessionTest.class,
        MailUsingCustomSessionTest.TestConfiguration.class
    }
)
public class MailUsingCustomSessionTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mockEndpoint;
    
    private static Session mailSession;
    
    @Bean("myCustomMailSession")
    private Session getSession() {
        mailSession = Session.getInstance(new Properties());
        return mailSession;
    }

    @BeforeEach
    public void setUp() throws Exception {
        Mailbox.clearAll();
    }

    @Test
    public void testEndpointConfigurationWithCustomSession() {
        // Verify that the mail session bound to the bean registry is identical
        // to the session tied to the endpoint configuration
        assertSame(mailSession, getEndpointMailSession("smtp://james@localhost?session=#myCustomMailSession"));
    }

    @Test
    public void testSendAndReceiveMailsWithCustomSession() throws Exception {
        
        mockEndpoint.expectedBodiesReceived("hello camel!");

        Map<String, Object> headers = new HashMap<>();
        headers.put("subject", "Hello Camel");
        template.sendBodyAndHeaders("smtp://james@localhost?session=#myCustomMailSession", "hello camel!", headers);

        mockEndpoint.assertIsSatisfied();

        Mailbox mailbox = Mailbox.get("james@localhost");
        assertEquals(1, mailbox.size(), "Expected one mail for james@localhost");

        Message message = mailbox.get(0);
        assertEquals("hello camel!", message.getContent());
        assertEquals("camel@localhost", message.getFrom()[0].toString());
    }

    private Session getEndpointMailSession(String uri) {
        MailEndpoint endpoint = context.getEndpoint(uri, MailEndpoint.class);
        return endpoint.getConfiguration().getSession();
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
                    from("pop3://james@localhost?session=#myCustomMailSession&initialDelay=100&delay=100").to("mock:result");
                }
            };
        }
    }
    
   

}
