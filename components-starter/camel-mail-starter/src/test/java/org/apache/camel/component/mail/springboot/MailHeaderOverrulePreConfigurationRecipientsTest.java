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
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jvnet.mock_javamail.Mailbox;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MailHeaderOverrulePreConfigurationRecipientsTest.class,
        MailHeaderOverrulePreConfigurationRecipientsTest.TestConfiguration.class
    }
)
public class MailHeaderOverrulePreConfigurationRecipientsTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testSendWithRecipientsInHeaders() throws Exception {
        Mailbox.clearAll();

        
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived("Hello World");
        mock.expectedHeaderReceived("to", "claus@localhost");
        mock.expectedHeaderReceived("cc", "willem@localhost");
        mock.expectedHeaderReceived("bcc", "hadrian@localhost");

        Map<String, Object> headers = new HashMap<>();
        headers.put("to", "claus@localhost");
        headers.put("cc", "willem@localhost");
        headers.put("bcc", "hadrian@localhost");

        template.sendBodyAndHeaders("smtp://james3@localhost", "Hello World", headers);

        mock.assertIsSatisfied(10000);
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
                    from("pop3://claus@localhost?to=someone@outhere.com&cc=none@world.com&initialDelay=100&delay=100")
                            .to("mock:result");
                }
            };
        }
    }
    
   

}
