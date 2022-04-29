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



import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import java.io.InputStream;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.DefaultJavaMailSender;
import org.apache.camel.component.mail.JavaMailSender;
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
        NestedMimeMessageConsumeTest.class,
        NestedMimeMessageConsumeTest.TestConfiguration.class
    }
)
public class NestedMimeMessageConsumeTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint resultEndpoint;
    
    @Test
    public void testNestedMultipart() throws Exception {
        Mailbox.clearAll();

        
        resultEndpoint.expectedMinimumMessageCount(1);

        prepareMailbox("james3");

        resultEndpoint.assertIsSatisfied();

        Exchange exchange = resultEndpoint.getReceivedExchanges().get(0);
        String text = exchange.getIn().getBody(String.class);
        assertThat(text, containsString("Test with bold face, pictures and attachments"));
        assertEquals("text/plain; charset=us-ascii", exchange.getIn().getHeader("Content-Type"));

        Set<String> attachmentNames = exchange.getIn(AttachmentMessage.class).getAttachmentNames();
        assertNotNull(attachmentNames, "attachments got lost");
        assertEquals(2, attachmentNames.size());
        for (String s : attachmentNames) {
            Attachment att = exchange.getIn(AttachmentMessage.class).getAttachmentObject(s);
            DataHandler dh = att.getDataHandler();
            Object content = dh.getContent();
            assertNotNull(content, "Content should not be empty");
            assertThat(dh.getName(), anyOf(equalTo("image001.png"), equalTo("test.txt")));
        }
    }

    private void prepareMailbox(String user) throws MessagingException {
        // connect to mailbox
        JavaMailSender sender = new DefaultJavaMailSender();
        Store store = sender.getSession().getStore("pop3");
        store.connect("localhost", 25, user, "secret");
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        folder.expunge();

        InputStream is = getClass().getResourceAsStream("/nested-multipart.elm");
        Message hurzMsg = new MimeMessage(sender.getSession(), is);
        Message[] messages = new Message[] { hurzMsg };

        // insert one signed message
        folder.appendMessages(messages);
        folder.close(true);
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
                    from("pop3://james3@localhost?initialDelay=100&delay=100").removeHeader("to").to("smtp://james4@localhost");
                    from("pop3://james4@localhost?initialDelay=200&delay=100").convertBodyTo(String.class).to("mock:result");
                }
            };
        }
    }
    
   

}
