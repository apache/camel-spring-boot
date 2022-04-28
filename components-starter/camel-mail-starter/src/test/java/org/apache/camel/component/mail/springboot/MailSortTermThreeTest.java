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



import java.util.Date;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import com.sun.mail.imap.SortTerm;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.DefaultJavaMailSender;
import org.apache.camel.component.mail.JavaMailSender;
import org.apache.camel.component.mail.SearchTermBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
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
        MailSortTermThreeTest.class,
        MailSortTermThreeTest.TestConfiguration.class
    }
)
public class MailSortTermThreeTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:resultDescendingImap")
    MockEndpoint mockDescImap;
    
    @Bean("sortAscendingDate")
    private SortTerm[] getAsc() {
        return new SortTerm[] { SortTerm.DATE };
    }

    @Bean("sortDescendingDate")
    private SortTerm[] getDesc() {
        return new SortTerm[] { SortTerm.REVERSE, SortTerm.DATE };
    }

    @Bean("searchTerm")
    private SearchTerm getSearchTerm() {
        return new SearchTermBuilder().subject("Camel").build();
    }

    
    @BeforeEach
    public void setUp() throws Exception {
        prepareMailbox();
    
    }

    @Test
    public void testSortTerm() throws Exception {
        Mailbox mailbox = Mailbox.get("bill@localhost");
        assertEquals(3, mailbox.size());

        // This one has search term set
        
        mockDescImap.expectedBodiesReceived("Even later date", "Later date", "Earlier date");

        context.getRouteController().startAllRoutes();

        mockDescImap.assertIsSatisfied();
    }

    private void prepareMailbox() throws Exception {
        // connect to mailbox
        Mailbox.clearAll();
        JavaMailSender sender = new DefaultJavaMailSender();
        Store store = sender.getSession().getStore("pop3");
        store.connect("localhost", 25, "bill", "secret");
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        folder.expunge();

        // inserts 3 messages, one with earlier, one with later sent date and
        // one with invalid subject (not returned in search)
        Message[] messages = new Message[3];
        messages[0] = new MimeMessage(sender.getSession());
        messages[0].setText("Earlier date");
        messages[0].setHeader("Message-ID", "0");
        messages[0].setSentDate(new Date(10000));
        messages[0].setSubject("Camel");

        messages[1] = new MimeMessage(sender.getSession());
        messages[1].setText("Later date");
        messages[1].setHeader("Message-ID", "1");
        messages[1].setSentDate(new Date(20000));
        messages[1].setSubject("Camel");

        messages[2] = new MimeMessage(sender.getSession());
        messages[2].setText("Even later date");
        messages[2].setHeader("Message-ID", "2");
        messages[2].setSentDate(new Date(30000));
        messages[2].setSubject("Invalid");

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
                    context.setAutoStartup(false);

                    from("imap://bill@localhost?password=secret&sortTerm=#sortDescendingDate&initialDelay=100&delay=100")
                            .to("mock:resultDescendingImap");
                }
            };
        }
    }
    
   

}
