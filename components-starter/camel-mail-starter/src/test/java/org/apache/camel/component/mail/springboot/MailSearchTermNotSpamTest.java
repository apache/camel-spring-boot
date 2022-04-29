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



import javax.mail.search.SearchTerm;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.SearchTermBuilder;
import org.apache.camel.component.mail.SearchTermBuilder.Op;
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
        MailSearchTermNotSpamTest.class,
        MailSearchTermNotSpamTest.TestConfiguration.class
    }
)
public class MailSearchTermNotSpamTest extends MailSearchTermTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    
    @Bean("myTerm")
    private SearchTerm getSearchTerm() {
        return createSearchTerm();
    }
    
    @Override
    protected SearchTerm createSearchTerm() {
        // we just want the unseen mails which is not spam
        SearchTermBuilder build = new SearchTermBuilder();
        build.unseen().body(Op.not, "Spam").subject(Op.not, "Spam");

        return build.build();
    }

    @Override
    @Test
    public void testSearchTerm() throws Exception {
        Mailbox mailbox = Mailbox.get("bill@localhost");
        assertEquals(6, mailbox.size());

        
        mock.expectedBodiesReceivedInAnyOrder("I like riding the Camel", "Ordering Camel in Action",
                "Ordering ActiveMQ in Action", "We meet at 7pm the usual place");

        mock.assertIsSatisfied();
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
                    from("pop3://bill@localhost?password=secret&searchTerm=#myTerm&initialDelay=100&delay=100").to("mock:result");
                }
            };
        }
    }
    
}
