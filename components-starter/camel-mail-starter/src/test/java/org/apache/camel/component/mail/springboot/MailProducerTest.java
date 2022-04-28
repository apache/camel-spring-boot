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



import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.DefaultAuthenticator;
import org.apache.camel.component.mail.MailConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        MailProducerTest.class,
        MailProducerTest.TestConfiguration.class
    }
)
public class MailProducerTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testProducer() throws Exception {
        Mailbox.clearAll();
        mock.reset();
        mock.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:start", "Message ", "To", "someone@localhost");
        mock.assertIsSatisfied();
        // need to check the message header
        Exchange exchange = mock.getExchanges().get(0);
        assertNotNull(exchange.getIn().getHeader(MailConstants.MAIL_MESSAGE_ID), "The message id should not be null");

        Mailbox box = Mailbox.get("someone@localhost");
        assertEquals(1, box.size());
    }

    @Test
    public void testProducerBodyIsMimeMessage() throws Exception {
        Mailbox.clearAll();
        mock.reset();
        mock.expectedMessageCount(1);

        Address from = new InternetAddress("fromCamelTest@localhost");
        Address to = new InternetAddress("recipient2@localhost");
        Session session = Session.getDefaultInstance(System.getProperties(), new DefaultAuthenticator("camel", "localhost"));
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(from);
        mimeMessage.addRecipient(RecipientType.TO, to);
        mimeMessage.setSubject("This is the subject.");
        mimeMessage.setText("This is the message");

        template.sendBodyAndHeader("direct:start", mimeMessage, "To", "someone@localhost");
        mock.assertIsSatisfied();
        // need to check the message header
        Exchange exchange = mock.getExchanges().get(0);
        assertNotNull(exchange.getIn().getHeader(MailConstants.MAIL_MESSAGE_ID), "The message id should not be null");

        Mailbox box = Mailbox.get("someone@localhost");
        assertEquals(0, box.size());

        // Check if the mimeMessagea has override body and headers
        Mailbox box2 = Mailbox.get("recipient2@localhost");
        assertEquals(1, box2.size());
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
                    from("direct:start").to("smtp://camel@localhost", "mock:result");
                }
            };
        }
    }
    
   

}
