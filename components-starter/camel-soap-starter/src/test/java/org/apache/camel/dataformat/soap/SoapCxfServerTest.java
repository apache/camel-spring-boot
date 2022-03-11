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


import java.util.List;

import com.example.customerservice.Customer;
import com.example.customerservice.CustomerService;
import com.example.customerservice.GetCustomersByName;
import com.example.customerservice.GetCustomersByNameResponse;
import com.example.customerservice.NoSuchCustomer;
import com.example.customerservice.NoSuchCustomerException;

import org.apache.camel.Produce;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.name.ElementNameStrategy;
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SoapCxfServerTest.class,
        SoapCxfServerTest.TestConfiguration.class
    }
)
@ImportResource({"classpath:META-INF/cxf/cxf.xml", 
    "classpath:META-INF/cxf/cxf-extension-camel.xml"})
public class SoapCxfServerTest {

    @Autowired
    Bus bus;
    
    @Produce("direct:camelClient")
    CustomerService customerServiceProxy;

    
    @Bean("serviceImpl") 
    private CustomerServiceImpl getCustomerServiceImpl() {
        return new CustomerServiceImpl();
    }
    
    @Bean("customerServiceEndpoint")
    private EndpointImpl getEndpointImpl(CustomerServiceImpl serviceImpl) {
        EndpointImpl endpoint = new EndpointImpl(bus, serviceImpl);
        endpoint.publish("camel://direct:cxfEndpoint");
        return endpoint;
    }
    
    @Test
    public void testSuccess() throws NoSuchCustomerException {
        GetCustomersByName request = new GetCustomersByName();
        request.setName("test");
        GetCustomersByNameResponse response = customerServiceProxy.getCustomersByName(request);
        assertNotNull(response);
        List<Customer> customers = response.getReturn();
        assertEquals(1, customers.size());
        assertEquals("test", customers.get(0).getName());
    }

    @Test
    public void testFault() {
        GetCustomersByName request = new GetCustomersByName();
        request.setName("none");
        try {
            customerServiceProxy.getCustomersByName(request);
            fail("NoSuchCustomerException expected");
        } catch (NoSuchCustomerException e) {
            NoSuchCustomer info = e.getFaultInfo();
            assertEquals("none", info.getCustomerId());
        }
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
                    ElementNameStrategy elNameStrat = new ServiceInterfaceStrategy(CustomerService.class, true);
                    SoapDataFormat soapDataFormat = new SoapDataFormat(jaxbPackage, elNameStrat);
                    from("direct:camelClient") //
                            .marshal(soapDataFormat) //
                            .to("direct:cxfEndpoint") //
                            .unmarshal(soapDataFormat);
                }
            };
        }
    }
}
