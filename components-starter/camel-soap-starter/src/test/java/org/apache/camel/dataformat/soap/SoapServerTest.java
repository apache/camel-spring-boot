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
package org.apache.camel.dataformat.soap;


import java.io.IOException;
import java.io.InputStream;


import com.example.customerservice.GetCustomersByName;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.name.ElementNameStrategy;
import org.apache.camel.dataformat.soap.name.TypeNameStrategy;
import org.apache.camel.dataformat.soap.springboot.TestUtil;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        SoapServerTest.class,
        SoapServerTest.TestConfiguration.class
    }
)
public class SoapServerTest {

    @Autowired
    CamelContext context;
    
    @Produce("direct:start")
    protected ProducerTemplate producer;

    @Test
    public void testSuccess() throws IOException, InterruptedException {
        sendAndCheckReply("request.xml", "response.xml");
    }

    @Test
    public void testFault() throws IOException, InterruptedException {
        sendAndCheckReply("requestFault.xml", "responseFault.xml");
    }

    private void sendAndCheckReply(String requestResource, String responseResource) throws IOException {
        context.setTracing(true);
        InputStream requestIs = this.getClass().getResourceAsStream(requestResource);
        InputStream responseIs = this.getClass().getResourceAsStream(responseResource);
        Object reply = producer.requestBody(requestIs);
        String replySt = context.getTypeConverter().convertTo(String.class, reply);
        assertEquals(TestUtil.readStream(responseIs), replySt);
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
                
                public void configure() throws Exception {
                    String jaxbPackage = GetCustomersByName.class.getPackage().getName();
                    ElementNameStrategy elNameStrat = new TypeNameStrategy();
                    SoapDataFormat soapDataFormat = new SoapDataFormat(jaxbPackage, elNameStrat);
                    CustomerServiceImpl serverBean = new CustomerServiceImpl();
                    from("direct:start").onException(Exception.class) // 
                            .handled(true) //
                            .marshal(soapDataFormat) //
                            .end() //
                            .unmarshal(soapDataFormat) //
                            .bean(serverBean) //
                            .marshal(soapDataFormat);
                }
            };
        }
    }
}
