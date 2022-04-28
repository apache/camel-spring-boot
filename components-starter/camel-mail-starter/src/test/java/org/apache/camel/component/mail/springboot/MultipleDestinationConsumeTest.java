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



import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.ObjectHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.CastUtils;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MultipleDestinationConsumeTest.class,
        MultipleDestinationConsumeTest.TestConfiguration.class
    }
)
public class MultipleDestinationConsumeTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint resultEndpoint;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    private String body = "hello world!";
    private Session mailSession;

    @Test
    public void testSendAndReceiveMails() throws Exception {
        Mailbox.clearAll();

        
        resultEndpoint.expectedMinimumMessageCount(1);

        MimeMessage message = new MimeMessage(mailSession);
        message.setText(body);

        message.setRecipients(Message.RecipientType.TO,
                new Address[] {
                        new InternetAddress("james@localhost"),
                        new InternetAddress("bar@localhost") });

        Transport.send(message);

        // lets test the receive worked
        resultEndpoint.assertIsSatisfied(100000);

        Exchange exchange = resultEndpoint.getReceivedExchanges().get(0);

        org.apache.camel.Message in = exchange.getIn();
        assertNotNull(in.getHeaders(), "Should have headers");

        MailMessage msg = (MailMessage) exchange.getIn();
        Message inMessage = msg != null ? msg.getMessage() : null;
        assertNotNull(inMessage, "In message has no JavaMail message!");

        String text = in.getBody(String.class);
        assertEquals(body, text, "mail body");

        // need to use iterator as some mail impl returns String[] and others a single String with comma as separator
        // so we let Camel create an iterator so we can use the same code for the test
        Object to = in.getHeader("TO");
        Iterator<String> it = CastUtils.cast(ObjectHelper.createIterator(to));
        int i = 0;
        while (it.hasNext()) {
            if (i == 0) {
                assertEquals("james@localhost", it.next().trim());
            } else {
                assertEquals("bar@localhost", it.next().trim());
            }
            i++;
        }

        Enumeration<Header> iter = CastUtils.cast(inMessage.getAllHeaders());
        while (iter.hasMoreElements()) {
            Header header = iter.nextElement();
            String[] value = message.getHeader(header.getName());
            log.debug("Header: " + header.getName() + " has value: " + org.apache.camel.util.ObjectHelper.asString(value));
        }
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "localhost");
        mailSession = Session.getInstance(properties, null);

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
                    from("pop3://james@localhost?password=foo&initialDelay=100&delay=100").to("mock:result");
                }
            };
        }
    }
    
   

}
