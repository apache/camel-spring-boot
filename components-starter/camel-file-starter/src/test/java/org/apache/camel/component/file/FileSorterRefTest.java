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

import java.util.Comparator;

/**
 * Unit test for the file sorter ref option
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileSorterRefTest.class,
                FileSorterRefTest.TestConfiguration.class
        }
)
public class FileSorterRefTest extends BaseFile {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Test
    public void testSortFiles() throws Exception {
        template.sendBodyAndHeader(fileUri(), "Hello Paris", Exchange.FILE_NAME, "paris.txt");

        template.sendBodyAndHeader(fileUri(), "Hello London", Exchange.FILE_NAME, "london.txt");

        template.sendBodyAndHeader(fileUri(), "Hello Copenhagen", Exchange.FILE_NAME, "copenhagen.txt");

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(fileUri("?initialDelay=0&delay=10&sorter=#mySorter")).convertBodyTo(String.class).to("mock:result");
            }
        });

        resultEndpoint.expectedBodiesReceived("Hello Copenhagen", "Hello London", "Hello Paris");
        assertMockEndpointsSatisfied();
    }

    // START SNIPPET: e1
    public class MyFileSorter<T> implements Comparator<GenericFile<T>> {
        @Override
        public int compare(GenericFile<T> o1, GenericFile<T> o2) {
            return o1.getFileName().compareToIgnoreCase(o2.getFileName());
        }
    }
    // END SNIPPET: e1

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean(value = "mySorter")
        public Comparator mySorter() {
            return new MyFileSorter();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUri("in?initialDelay=0&delay=10&readLock=changed&readLockCheckInterval=100&readLockMarkerFile=false"))
                            .to(fileUri("out"),
                                    "mock:result");
                }
            };
        }
    }
}
