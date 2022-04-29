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



import javax.mail.Message;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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
        MailRecipientsPipeIssueTest.class,
        MailRecipientsPipeIssueTest.TestConfiguration.class
    }
)
public class MailRecipientsPipeIssueTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testMultiRecipients() throws Exception {
        Mailbox.clearAll();

        template.sendBody("direct:a", "Camel does really rock");

        Mailbox inbox = Mailbox.get("camel|pipes@riders.org");
        Message msg = inbox.get(0);
        assertEquals("you@apache.org", msg.getFrom()[0].toString());
        assertEquals("camel|pipes@riders.org", msg.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("easy@riders.org", msg.getRecipients(Message.RecipientType.TO)[1].toString());

        inbox = Mailbox.get("easy@riders.org");
        msg = inbox.get(0);
        assertEquals("you@apache.org", msg.getFrom()[0].toString());
        assertEquals("camel|pipes@riders.org", msg.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("easy@riders.org", msg.getRecipients(Message.RecipientType.TO)[1].toString());
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
                    String recipients = "&to=camel|pipes@riders.org;easy@riders.org";

                    from("direct:a").to("smtp://you@mymailserver.com?password=secret&from=you@apache.org" + recipients);
                }
            };
        }
    }
    
   

}
