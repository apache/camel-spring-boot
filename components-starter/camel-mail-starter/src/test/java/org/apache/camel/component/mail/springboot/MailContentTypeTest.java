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

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        MailContentTypeTest.class,
        MailContentTypeTest.TestConfiguration.class
    }
)
public class MailContentTypeTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testSendHtmlMail() throws Exception {
        Mailbox.clearAll();

        template.sendBody("direct:a", "<html><body><h1>Hello</h1>World</body></html>");

        Mailbox box = Mailbox.get("claus@localhost");
        Message msg = box.get(0);

        assertTrue(msg.getContentType().startsWith("text/html"));
        assertEquals("<html><body><h1>Hello</h1>World</body></html>", msg.getContent());
    }

    @Test
    public void testSendPlainMail() throws Exception {
        Mailbox.clearAll();

        template.sendBody("direct:b", "Hello World");

        Mailbox box = Mailbox.get("claus@localhost");
        Message msg = box.get(0);
        assertTrue(msg.getContentType().startsWith("text/plain"));
        assertEquals("Hello World", msg.getContent());
    }

    @Test
    public void testSendMultipartMail() throws Exception {
        Mailbox.clearAll();

        Map<String, Object> headers = new HashMap<>();
        headers.put(MailConstants.MAIL_ALTERNATIVE_BODY, "Hello World");
        
        template.send("direct:c", exchange -> {
            org.apache.camel.Message in = exchange.getIn();
            in.setBody("<html><body><h1>Hello</h1>World</body></html>");
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                in.setHeader(entry.getKey(), entry.getValue());
            }
        });

        Mailbox box = Mailbox.get("claus@localhost");
        Message msg = box.get(0);
        assertTrue(msg.getContentType().startsWith("multipart/alternative"));
        assertEquals("Hello World", ((MimeMultipart) msg.getContent()).getBodyPart(0).getContent());
        assertEquals("<html><body><h1>Hello</h1>World</body></html>",
                ((MimeMultipart) msg.getContent()).getBodyPart(1).getContent());
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
                    from("direct:a").to("smtp://claus@localhost?contentType=text/html");
                    from("direct:b").to("smtp://claus@localhost?contentType=text/plain");
                    from("direct:c").to("smtp://claus@localhost");
                }
            };
        }
    }
    
   

}
