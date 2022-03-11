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

import javax.xml.namespace.QName;

import com.example.customerservice.CustomerService;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@ImportResource({"classpath:META-INF/cxf/cxf.xml", 
    "classpath:META-INF/cxf/cxf-extension-camel.xml"})
@Configuration
public class CustomerServiceCxfProxy {
    
    @Autowired
    Bus bus;
    
    @Bean
    public CustomerService getCustomerService() {
        JaxWsProxyFactoryBean clientProxyFactoryBean = new JaxWsProxyFactoryBean();
        clientProxyFactoryBean.setBus(bus);
        clientProxyFactoryBean.setAddress("camel://direct:cxfclient");
        QName serviceName = new QName("http://customerservice.example.com/", "CustomerServiceService");
        clientProxyFactoryBean.setServiceName(serviceName);
        QName endpointName = new QName("http://customerservice.example.com/", "CustomerServiceEndpoint");
        clientProxyFactoryBean.setEndpointName(endpointName);
        clientProxyFactoryBean.setServiceClass(com.example.customerservice.CustomerService.class);
        return (CustomerService)clientProxyFactoryBean.create();
    }
    
}
