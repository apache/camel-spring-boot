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

import com.example.customerservice.GetCustomersByName;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.soap.SoapDataFormat;
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SoapSpringRoundtripTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SoapSpringRoundtripTest-context.xml"}

)
public class SoapSpringRoundtripTest {

    
    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Autowired
    @Produce("direct:start")
    protected ProducerTemplate producer;
    
    @Bean("myNameStrategy") 
    private ServiceInterfaceStrategy getServiceInterfaceStrategy() {
        return new ServiceInterfaceStrategy(com.example.customerservice.CustomerService.class, true);
    }

    @Bean("soap")
    private SoapDataFormat getSoapDataFormat(ServiceInterfaceStrategy serviceInterfaceStrategy) {
        SoapDataFormat soapDataFormat = new SoapDataFormat();
        soapDataFormat.setContextPath("com.example.customerservice");
        soapDataFormat.setElementNameStrategy(serviceInterfaceStrategy);
        soapDataFormat.setSchema("classpath:org/apache/camel/dataformat/soap/CustomerService.xsd,classpath:soap.xsd");
        return soapDataFormat;
    }
  
    @Test
    public void testRoundTrip() throws IOException, InterruptedException {
        resultEndpoint.expectedMessageCount(1);
        GetCustomersByName request = new GetCustomersByName();
        request.setName("Mueller");
        producer.sendBody(request);
        resultEndpoint.assertIsSatisfied();
        Exchange exchange = resultEndpoint.getExchanges().get(0);
        GetCustomersByName received = exchange.getIn().getBody(
                GetCustomersByName.class);
        assertNotNull(received);
        assertEquals("Mueller", received.getName());
    }
}
