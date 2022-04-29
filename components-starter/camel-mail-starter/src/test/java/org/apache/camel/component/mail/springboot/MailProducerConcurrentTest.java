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



import static org.apache.camel.test.junit5.TestSupport.body;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
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
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jvnet.mock_javamail.Mailbox;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MailProducerConcurrentTest.class,
        MailProducerConcurrentTest.TestConfiguration.class
    }
)
public class MailProducerConcurrentTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testNoConcurrentProducers() throws Exception {
        doSendMessages(1, 1);
    }

    @Test
    public void testConcurrentProducers() throws Exception {
        doSendMessages(10, 5);
    }

    private void doSendMessages(int files, int poolSize) throws Exception {
        Mailbox.clearAll();

        NotifyBuilder builder = new NotifyBuilder(context).whenDone(files).create();

        mock.expectedMessageCount(files);
        mock.expectsNoDuplicates(body());

        final CountDownLatch latch = new CountDownLatch(files);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < files; i++) {
            final int index = i;
            executor.submit(new Callable<Object>() {
                public Object call() {
                    template.sendBodyAndHeader("direct:start", "Message " + index, "To", "someone@localhost");
                    latch.countDown();
                    return null;
                }
            });
        }

        // wait first for all the exchanges above to be thoroughly sent asynchronously
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        mock.assertIsSatisfied();
        assertTrue(builder.matchesWaitTime());

        Mailbox box = Mailbox.get("someone@localhost");
        assertEquals(files, box.size());

        // as we use concurrent producers the mails can arrive out of order
        Set<Object> bodies = new HashSet<>();
        for (int i = 0; i < files; i++) {
            bodies.add(box.get(i).getContent());
        }

        assertEquals(files, bodies.size(), "There should be " + files + " unique mails");
        executor.shutdownNow();
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
                    from("direct:start").to("smtp://camel@localhost", "mock:result");
                }
            };
        }
    }
    
   

}
