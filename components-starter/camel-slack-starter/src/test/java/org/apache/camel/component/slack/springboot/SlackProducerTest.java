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
package org.apache.camel.component.slack.springboot;


import java.util.Collections;

import com.slack.api.model.Message;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.slack.SlackComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SlackProducerTest.class,
        SlackProducerTest.TestConfiguration.class
    }
)
public class SlackProducerTest {
    
    

    protected static final int UNDERTOW_PORT = AvailablePortFinder.getNextAvailable();
    
    @Autowired
    CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:errors")
    MockEndpoint errors;

    @EndpointInject("direct:test")
    DirectEndpoint test;

    
   
      
    
    @Test
    public void testSlackMessage() throws Exception {
        errors.reset();
        
        errors.expectedMessageCount(0);

        template.sendBody(test, "Hello from Camel!");

        errors.assertIsSatisfied();
    }

    @Test
    public void testSlackAPIModelMessage() throws Exception {
        errors.reset();
        errors.expectedMessageCount(0);

        Message message = new Message();
        message.setBlocks(Collections.singletonList(SectionBlock
                .builder()
                .text(MarkdownTextObject
                        .builder()
                        .text("*Hello from Camel!*")
                        .build())
                .build()));

        template.sendBody(test, message);

        errors.assertIsSatisfied();
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
                    SlackComponent slack = new SlackComponent();
                    slack.setWebhookUrl("http://localhost:" + UNDERTOW_PORT + "/slack/webhook");
                    context.addComponent("slack", slack);

                    onException(Exception.class).handled(true).to(errors);

                    final String slackUser = System.getProperty("SLACK_USER", "CamelTest");
                    from("undertow:http://localhost:" + UNDERTOW_PORT + "/slack/webhook").setBody(constant("{\"ok\": true}"));

                    from(test).to(String.format("slack:#general?iconEmoji=:camel:&username=%s", slackUser));
                }
            };
        }
    }
}
