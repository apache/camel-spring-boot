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



import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailAuthenticator;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jvnet.mock_javamail.Mailbox;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        AuthenticatorTest.class,
        AuthenticatorTest.TestConfiguration.class
    }
)
public class AuthenticatorTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mockResult;
    
    @EndpointInject("mock:exception")
    MockEndpoint mockException;
    
    
  
    @Bean("authPop3")
    private MyAuthenticator getAuthPop3() {
        return new MyAuthenticator("pop3");
    }

    @Bean("authSmtp")
    private MyAuthenticator getAuthSmtp() {
        return new MyAuthenticator("smtp");
    }
    
    @Bean("authImap")
    private MyAuthenticator getAuthImap() {
        return new MyAuthenticator("imap");
    }
    
    
    
    @Test
    public void testSendAndReceiveMails() throws Exception {
        Mailbox.clearAll();
        // first expect correct result because smtp authenticator does not return wrong password       
        callAndCheck(mockResult);
        // second expect exception  because smtp authenticator does return wrong password       
        callAndCheck(mockException);
        // third expect correct result because smtp authenticator does not return wrong password       
        callAndCheck(mockResult);
    }

    private String callAndCheck(MockEndpoint resultEndpoint) throws MessagingException, InterruptedException {
        resultEndpoint.reset();
        resultEndpoint.expectedMinimumMessageCount(1);
        //resultEndpoint.setResultWaitTime(60000);
        String body = "hello world!";
        execute("james3@localhost", body);

        resultEndpoint.assertIsSatisfied();

        Exchange exchange = resultEndpoint.getReceivedExchanges().get(0);
        String text = exchange.getIn().getBody(String.class);
        assertEquals(body, text, "mail body");
        return body;
    }

    private void execute(String mailAddress, String body) throws MessagingException {

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "localhost");
        Session session = Session.getInstance(properties, null);

        MimeMessage message = new MimeMessage(session);
        populateMimeMessageBody(message, body);
        message.setRecipients(Message.RecipientType.TO, mailAddress);

        Transport.send(message);

    }

    protected void populateMimeMessageBody(MimeMessage message, String body) throws MessagingException {
        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText(body);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setText("<html><body>" + body + "</body></html>");

        Multipart alt = new MimeMultipart("alternative");
        alt.addBodyPart(plainPart);
        alt.addBodyPart(htmlPart);

        Multipart mixed = new MimeMultipart("mixed");
        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(alt);
        mixed.addBodyPart(wrap);

        mixed.addBodyPart(plainPart);
        mixed.addBodyPart(htmlPart);

        mixed.addBodyPart(plainPart);
        mixed.addBodyPart(htmlPart);

        message.setContent(mixed);
    }
    
    
    public class MyAuthenticator extends MailAuthenticator {
        private final String protocol;
        private int counter;

        public MyAuthenticator(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            if ("pop3".equals(protocol)) {
                return new PasswordAuthentication("james3", "secret");
            } else if ("smtp".equals(protocol)) {
                if (counter < 2) {
                    // in the processing of a mail message the mail consumer calls this method twice
                    counter++;
                    return new PasswordAuthentication("james4", "secret");
                } else if (counter < 3) {
                    // return in the second call the wrongPassword which will throw an MessagingException, see MyMockTransport
                    counter++;
                    return new PasswordAuthentication("james4", "wrongPassword");
                } else {
                    return new PasswordAuthentication("james4", "secret");
                }
            } else if ("imap".equals(protocol)) {
                return new PasswordAuthentication("james4", "secret");
            } else {
                throw new IllegalStateException("not supported protocol " + protocol);
            }

        }
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
                    onException(MessagingException.class).handled(true).to("mock:exception");

                    from("pop3://localhost?initialDelay=100&delay=100&authenticator=#authPop3").removeHeader("to")
                            .to("smtp://localhost?authenticator=#authSmtp&to=james4@localhost");
                    from("imap://localhost?initialDelay=200&delay=100&authenticator=#authImap").convertBodyTo(String.class)
                            .to("mock:result");
                }
            };
        }
    }
    
   

}
