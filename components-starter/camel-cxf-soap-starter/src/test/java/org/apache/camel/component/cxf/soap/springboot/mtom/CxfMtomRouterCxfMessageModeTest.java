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
package org.apache.camel.component.cxf.soap.springboot.mtom;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.cxf.mtom_feature.Hello;
import org.apache.camel.cxf.mtom_feature.HelloService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfMtomRouterCxfMessageModeTest.class,
                           CxfMtomRouterPayloadModeTest.TestConfiguration.class,
                           CxfMtomRouterCxfMessageModeTest.EndpointConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfMtomRouterCxfMessageModeTest extends CxfMtomRouterPayloadModeTest {
    
       
    

    @Override
    protected Hello getPort() {
        URL wsdl = getClass().getResource("/mtom.wsdl");
        assertNotNull(wsdl, "WSDL is null");

        HelloService service = new HelloService(wsdl, HelloService.SERVICE);
        assertNotNull(service, "Service is null");
        Hello hello = service.getHelloPort();
        ((BindingProvider) hello).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services" + "/CxfMtomRouterCxfMessageModeTest/jaxws-mtom/hello");
        return hello;
    }
    
    @Configuration
    class EndpointConfiguration {
        @Bean
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("/" + "CxfMtomRouterCxfMessageModeTest" 
                + "/jaxws-mtom/hello");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            properties.put("mtom-enabled", "true");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/" 
                + "CxfMtomRouterCxfMessageModeTest" + "/jaxws-mtom/backend");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            properties.put("mtom-enabled", "true");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
    }
}
