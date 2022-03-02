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
package org.apache.camel.component.jsonpath.springboot.test;


import java.io.File;
import java.io.FileInputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SpringJsonPathCBRTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringJsonPathCBTTest.xml"}

)
public class SpringJsonPathCBRTest {

    @Autowired
    CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:cheap")
    MockEndpoint mockCheap;
    
    @EndpointInject("mock:average")
    MockEndpoint mockAverage;
    
    @EndpointInject("mock:expensive")
    MockEndpoint mockExpensive;

   
    
    

    @Test
    public void testCheap() throws Exception {
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(1);
        mockAverage.expectedMessageCount(0);
        mockExpensive.expectedMessageCount(0);

        template.sendBody("direct:start", new FileInputStream(new File("src/test/resources/cheap.json")));

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testAverage() throws Exception {
        
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(0);
        mockAverage.expectedMessageCount(1);
        mockExpensive.expectedMessageCount(0);
        template.sendBody("direct:start", new FileInputStream(new File("src/test/resources/average.json")));

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testExpensive() throws Exception {
        MockEndpoint.resetMocks(context);
        mockCheap.expectedMessageCount(0);
        mockAverage.expectedMessageCount(0);
        mockExpensive.expectedMessageCount(1);
        template.sendBody("direct:start", new FileInputStream(new File("src/test/resources/expensive.json")));

        MockEndpoint.assertIsSatisfied(context);
    }
}
