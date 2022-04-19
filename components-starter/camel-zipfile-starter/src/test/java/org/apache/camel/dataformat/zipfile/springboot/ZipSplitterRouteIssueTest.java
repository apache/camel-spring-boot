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
package org.apache.camel.dataformat.zipfile.springboot;



import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        ZipSplitterRouteIssueTest.class,
        ZipSplitterRouteIssueTest.TestConfiguration.class
    }
)
public class ZipSplitterRouteIssueTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    
    @EndpointInject("mock:entry")
    MockEndpoint mockEntry;
    
    @EndpointInject("mock:errors")
    MockEndpoint mockErrors;
    
    
    @BeforeEach
    public void setUp() throws Exception {
        TestSupport.deleteDirectory("target/zip");
              
    }

    @Test
    public void testSplitter() throws Exception {
        mockEntry.reset();
        mockEntry.expectedMessageCount(2);

        template.sendBody("direct:decompressFiles", new File("src/test/resources/data.zip"));

        mockEntry.assertIsSatisfied();
    }

    @Test
    public void testSplitterWithWrongFile() throws Exception {
        mockEntry.reset();
        mockEntry.expectedMessageCount(0);
        mockErrors.expectedMessageCount(1);

        //Send a file which is not exit
        template.sendBody("direct:decompressFiles", new File("src/test/resources/data"));

        mockEntry.assertIsSatisfied();
        mockErrors.assertIsSatisfied();
    }

    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(deadLetterChannel("mock:errors"));

                    from("direct:decompressFiles")
                            .split(new ZipSplitter()).streaming().shareUnitOfWork()
                            .to("log:entry")
                            .to("mock:entry");
                }
            };
        }
    }
    
    

}
