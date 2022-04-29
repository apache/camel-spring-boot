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


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MailDisconnectTest.class,
        MailDisconnectTest.TestConfiguration.class
    }
)
public class MailDisconnectTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testDisconnect() throws Exception {
        mock.expectedMessageCount(5);

        // send 5 mails with some delay so we do multiple polls with disconnect between
        template.sendBodyAndHeader("smtp://jones@localhost", "A Bla bla", "Subject", "Hello A");
        template.sendBodyAndHeader("smtp://jones@localhost", "B Bla bla", "Subject", "Hello B");

        Thread.sleep(500);
        template.sendBodyAndHeader("smtp://jones@localhost", "C Bla bla", "Subject", "Hello C");

        Thread.sleep(500);
        template.sendBodyAndHeader("smtp://jones@localhost", "D Bla bla", "Subject", "Hello D");

        Thread.sleep(500);
        template.sendBodyAndHeader("smtp://jones@localhost", "E Bla bla", "Subject", "Hello E");

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
                    from("pop3://jones@localhost?password=secret&disconnect=true&initialDelay=100&delay=100").to("mock:result");
                }
            };
        }
    }
    
   

}
