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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;



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
        MailNameAndEmailInRecipientTest.class,
        MailNameAndEmailInRecipientTest.TestConfiguration.class
    }
)
public class MailNameAndEmailInRecipientTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:davsclaus")
    MockEndpoint mockClaus;
    
    @EndpointInject("mock:jstrachan")
    MockEndpoint mockJames;
    
    @Test
    public void testSendWithNameAndEmailInRecipient() throws Exception {
        Mailbox.clearAll();

        // START SNIPPET: e1
        Map<String, Object> headers = new HashMap<>();
        headers.put("to", "Claus Ibsen <davsclaus@localhost>");
        headers.put("cc", "James Strachan <jstrachan@localhost>");

        assertMailbox(mockClaus);
        assertMailbox(mockJames);

        template.sendBodyAndHeaders("smtp://localhost", "Hello World", headers);
        // END SNIPPET: e1

        MockEndpoint.assertIsSatisfied(context);
    }

    private void assertMailbox(MockEndpoint mock) {
        
        mock.expectedBodiesReceived("Hello World");
        mock.message(0).header("to").isEqualTo("Claus Ibsen <davsclaus@localhost>");
        mock.message(0).header("cc").isEqualTo("James Strachan <jstrachan@localhost>");
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
                    from("pop3://davsclaus@localhost?initialDelay=100&delay=100").to("mock:davsclaus");

                    from("pop3://jstrachan@localhost?initialDelay=100&delay=100").to("mock:jstrachan");
                }
            };
        }
    }
    
   

}
