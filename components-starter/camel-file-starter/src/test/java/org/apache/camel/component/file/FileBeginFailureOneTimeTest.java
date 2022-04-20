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
package org.apache.camel.component.file;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileBeginFailureOneTimeTest.class,
                FileBeginFailureOneTimeTest.TestConfiguration.class
        }
)
public class FileBeginFailureOneTimeTest extends BaseFile {

    private static MyStrategy myStrategy = new MyStrategy();

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Test
    public void testBeginFailureOneTime() throws Exception {
        resultEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, "hello.txt");

        assertMockEndpointsSatisfied();

        assertEquals(2, myStrategy.getInvoked(), "Begin should have been invoked 2 times");
    }

    private static class MyStrategy implements GenericFileProcessStrategy<File> {

        private volatile int invoked;

        @Override
        public void prepareOnStartup(
                GenericFileOperations<File> fileGenericFileOperations, GenericFileEndpoint<File> fileGenericFileEndpoint)
                throws Exception {
        }

        @Override
        public boolean begin(
                GenericFileOperations<File> fileGenericFileOperations, GenericFileEndpoint<File> fileGenericFileEndpoint,
                Exchange exchange,
                GenericFile<File> fileGenericFile)
                throws Exception {
            invoked++;
            if (invoked <= 1) {
                throw new IllegalArgumentException("Damn I cannot do this");
            }
            return true;
        }

        @Override
        public void abort(
                GenericFileOperations<File> fileGenericFileOperations, GenericFileEndpoint<File> fileGenericFileEndpoint,
                Exchange exchange,
                GenericFile<File> fileGenericFile)
                throws Exception {
            // noop
        }

        @Override
        public void commit(
                GenericFileOperations<File> fileGenericFileOperations, GenericFileEndpoint<File> fileGenericFileEndpoint,
                Exchange exchange,
                GenericFile<File> fileGenericFile)
                throws Exception {
        }

        @Override
        public void rollback(
                GenericFileOperations<File> fileGenericFileOperations, GenericFileEndpoint<File> fileGenericFileEndpoint,
                Exchange exchange,
                GenericFile<File> fileGenericFile)
                throws Exception {
        }

        public int getInvoked() {
            return invoked;
        }
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean(value = "myStrategy")
        public GenericFileProcessStrategy myStrategy() {
            return myStrategy;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUri("?initialDelay=0&delay=10&processStrategy=#myStrategy")).convertBodyTo(String.class)
                            .to("mock:result");
                }
            };
        }
    }
}
