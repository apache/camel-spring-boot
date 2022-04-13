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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Unit test for the file filter option
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileConsumerFileFilterTest.class,
                FileConsumerFileFilterTest.TestConfiguration.class
        }
)
public class FileConsumerFileFilterTest extends BaseFile {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    private String fileUrl = fileUri("?initialDelay=0&delay=10&filter=#myFilter");

    @AfterEach
    public void reset() {
        resultEndpoint.reset();
    }

    @Test
    public void testFilterFiles() throws Exception {
        resultEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader(fileUri(), "This is a file to be filtered",
                Exchange.FILE_NAME,
                "skipme.txt");

        resultEndpoint.setResultWaitTime(100);
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFilterFilesWithARegularFile() throws Exception {
        resultEndpoint.expectedBodiesReceived("Hello World");

        template.sendBodyAndHeader(fileUri(), "This is a file to be filtered",
                Exchange.FILE_NAME,
                "skipme.txt");

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME,
                "hello.txt");

        resultEndpoint.assertIsSatisfied();
    }

    // START SNIPPET: e1
    public class MyFileFilter<T> implements GenericFileFilter<T> {
        @Override
        public boolean accept(GenericFile<T> file) {
            // we want all directories
            if (file.isDirectory()) {
                return true;
            }
            // we dont accept any files starting with skip in the name
            return !file.getFileName().startsWith("skip");
        }
    }
    // END SNIPPET: e1


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        @Bean(value = "myFilter")
        public GenericFileFilter myFilter() {
            return new MyFileFilter();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUrl).convertBodyTo(String.class).to("mock:result");
                }
            };
        }
    }
}
