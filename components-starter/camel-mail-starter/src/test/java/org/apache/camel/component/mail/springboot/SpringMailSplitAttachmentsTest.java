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



import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.IOUtils;
import org.jvnet.mock_javamail.Mailbox;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SpringMailSplitAttachmentsTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringMailSplitAttachmentsTest.xml"}

)
public class SpringMailSplitAttachmentsTest {

    @Autowired
    private CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:split")
    MockEndpoint mock;
    
    

    private Endpoint endpoint;
    private Exchange exchange;
    

    @Bean("splitAttachments")
    private SplitAttachmentsExpression getSplitAttachmentsExpression() {
        return new SplitAttachmentsExpression();
    }
    
    @BeforeEach
    public void clearMailBox() {
        Mailbox.clearAll();
    }

    @BeforeEach
    public void setup() {
        // create the exchange with the mail message that is multipart with a file and a Hello World text/plain message.
        endpoint = context.getEndpoint("smtp://james@mymailserver.com?password=secret");
        exchange = endpoint.createExchange();
        AttachmentMessage in = exchange.getIn(AttachmentMessage.class);
        in.setBody("Hello World");
        in.addAttachment("logo.jpeg", new DataHandler(new FileDataSource("src/test/data/logo.jpeg")));
        in.addAttachment("log4j2.properties", new DataHandler(new FileDataSource("src/test/resources/log4j2.properties")));
    }

    @Test
    public void testExtractAttachments() throws Exception {
        
        mock.expectedMessageCount(2);

        Producer producer = endpoint.createProducer();
        producer.start();
        producer.process(exchange);

        mock.assertIsSatisfied();

        AttachmentMessage first = mock.getReceivedExchanges().get(0).getIn(AttachmentMessage.class);
        AttachmentMessage second = mock.getReceivedExchanges().get(1).getIn(AttachmentMessage.class);

        // check it's no longer an attachment, but is the message body
        assertEquals(0, first.getAttachments().size());
        assertEquals(0, second.getAttachments().size());

        assertEquals("logo.jpeg", first.getHeader("CamelSplitAttachmentId"));
        assertEquals("log4j2.properties", second.getHeader("CamelSplitAttachmentId"));

        byte[] expected1 = IOUtils.toByteArray(new FileDataSource("src/test/data/logo.jpeg").getInputStream());
        byte[] expected2 = IOUtils.toByteArray(new FileDataSource("src/test/resources/log4j2.properties").getInputStream());

        assertArrayEquals(expected1, first.getBody(byte[].class));
        assertArrayEquals(expected2, second.getBody(byte[].class));
    }
}
