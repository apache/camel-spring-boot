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

import java.util.Iterator;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
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
        ZipFileSplitIteratorCorruptTest.class,
        ZipFileSplitIteratorCorruptTest.TestConfiguration.class
    }
)
public class ZipFileSplitIteratorCorruptTest {

    
    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:dead")
    MockEndpoint mockDead;
    
    @EndpointInject("mock:end")
    MockEndpoint mockEnd;
    
    
    @Test
    public void testZipFileUnmarshal() throws Exception {
        mockDead.expectedMessageCount(1);
        mockDead.message(0).exchangeProperty(Exchange.EXCEPTION_CAUGHT)
                .isInstanceOf(IllegalStateException.class);
        mockEnd.expectedMessageCount(0);

        mockDead.assertIsSatisfied();
        mockEnd.assertIsSatisfied();
    
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
                    ZipFileDataFormat zf = new ZipFileDataFormat();
                    zf.setUsingIterator(true);

                    errorHandler(deadLetterChannel("mock:dead"));

                    from("file://src/test/resources?delay=10&fileName=corrupt.zip&noop=true")
                            .unmarshal(zf)
                            .split(bodyAs(Iterator.class)).streaming()
                            .convertBodyTo(String.class)
                            .to("mock:end")
                            .end();
                }
            };
        }
    }
    
   

}
