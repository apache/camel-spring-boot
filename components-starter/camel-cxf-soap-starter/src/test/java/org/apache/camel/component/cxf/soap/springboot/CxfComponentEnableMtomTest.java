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
package org.apache.camel.component.cxf.soap.springboot;

import java.util.HashMap;
import java.util.Map;


import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.message.Message;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfComponentEnableMtomTest.class,
                           CxfComponentEnableMtomTest.TestConfig.class,
                           CxfAutoConfiguration.class
})
public class CxfComponentEnableMtomTest {

    @Autowired
    private CamelContext camelContext;
    
    
    @Test
    public void testIsMtomEnabledEnabledThroughBeanSetter() throws InterruptedException {
        Endpoint endpoint = camelContext.getEndpoint("cxf:bean:mtomByBeanSetter");

        if (endpoint instanceof CxfEndpoint) {
            CxfEndpoint cxfEndpoint = (CxfEndpoint) endpoint;
            assertTrue(cxfEndpoint.isMtomEnabled(), "Mtom should be enabled");
        } else {
            fail("CXF Endpoint not found");
        }
    }

    @Test
    public void testIsMtomEnabledEnabledThroughBeanProperties() throws InterruptedException {
        Endpoint endpoint = camelContext.getEndpoint("cxf:bean:mtomByBeanProperties");

        if (endpoint instanceof CxfEndpoint) {
            CxfEndpoint cxfEndpoint = (CxfEndpoint) endpoint;
            assertTrue(cxfEndpoint.isMtomEnabled(), "Mtom should be enabled");
        } else {
            fail("CXF Endpoint not found");
        }
    }

    @Test
    public void testIsMtomEnabledEnabledThroughURIProperties() throws InterruptedException {
        Endpoint endpoint = camelContext.getEndpoint("cxf:bean:mtomByURIProperties?properties.mtom-enabled=true");

        if (endpoint instanceof CxfEndpoint) {
            CxfEndpoint cxfEndpoint = (CxfEndpoint) endpoint;
            assertTrue(cxfEndpoint.isMtomEnabled(), "Mtom should be enabled");
        } else {
            fail("CXF Endpoint not found");
        }
    }

    @Test
    public void testIsMtomEnabledEnabledThroughQueryParameters() throws InterruptedException {
        Endpoint endpoint = camelContext.getEndpoint("cxf:bean:mtomByQueryParameters?mtomEnabled=true");

        if (endpoint instanceof CxfEndpoint) {
            CxfEndpoint cxfEndpoint = (CxfEndpoint) endpoint;
            assertTrue(cxfEndpoint.isMtomEnabled(), "Mtom should be enabled");
        } else {
            fail("CXF Endpoint not found");
        }
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    
    static class TestConfig {

        @Bean("mtomByQueryParameters")
        public CxfEndpoint mtomByQueryParameters(CamelContext context) {
            CxfEndpoint endpoint = new CxfSpringEndpoint();
            return endpoint;
        }

        @Bean("mtomByURIProperties")
        public CxfEndpoint mtomByURIProperties() {
            return new CxfSpringEndpoint();
        }

        @Bean("mtomByBeanProperties")
        public CxfEndpoint mtomByBeanProperties() {
            CxfEndpoint endpoint = new CxfSpringEndpoint();
            Map<String, Object> properties = new HashMap<>();
            properties.put(Message.MTOM_ENABLED, true);

            endpoint.setProperties(properties);
            return endpoint;

        }

        @Bean("mtomByBeanSetter")
        public CxfEndpoint mtomByBeanSetter() {
            CxfEndpoint endpoint = new CxfSpringEndpoint();
            endpoint.setMtomEnabled(true);
            return endpoint;

        }
    }

}
