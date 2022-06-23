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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;


import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.soap.name.TypeNameStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        SoapToSoapDontIgnoreTest.class,
        SoapToSoapDontIgnoreTest.TestConfiguration.class
    }
)
public class SoapToSoapDontIgnoreTest {

   
 
    @EndpointInject("mock:end")
    protected MockEndpoint mock;

    @Autowired
    protected ProducerTemplate template;

    private static SoapDataFormat soapjaxbModel;
    private static SoapDataFormat soapjaxbModelDontIgnoreUnmarshalled;
    private static Map<String, String> namespacePrefixMap;

    @BeforeAll
    public static void setup() {
        namespacePrefixMap = new HashMap<>();
        namespacePrefixMap.put("http://schemas.xmlsoap.org/soap/envelope/", "soap");
        namespacePrefixMap.put("http://www.w3.org/2001/XMLSchema", "xsd");
        namespacePrefixMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespacePrefixMap.put("http://www.example.com/contact", "cont");
        namespacePrefixMap.put("http://www.example.com/soapheaders", "custom");
        soapjaxbModel = new SoapDataFormat("com.example.contact:com.example.soapheaders");
        soapjaxbModel.setNamespacePrefix(namespacePrefixMap);
        soapjaxbModel.setPrettyPrint(true);
        soapjaxbModel.setIgnoreUnmarshalledHeaders(false);
        soapjaxbModel.setIgnoreJAXBElement(false);
        soapjaxbModel.setElementNameStrategy(new TypeNameStrategy());
        soapjaxbModelDontIgnoreUnmarshalled = new SoapDataFormat(
                "com.example.contact:com.example.soapheaders");
        soapjaxbModelDontIgnoreUnmarshalled.setNamespacePrefix(namespacePrefixMap);
        soapjaxbModelDontIgnoreUnmarshalled.setPrettyPrint(true);
        soapjaxbModelDontIgnoreUnmarshalled.setIgnoreUnmarshalledHeaders(false);
        soapjaxbModelDontIgnoreUnmarshalled.setElementNameStrategy(new TypeNameStrategy());
    }

    @AfterAll
    public static void teardown() {
        soapjaxbModel = null;
        namespacePrefixMap = null;
    }

    @Test
    public void testSoapMarshal() throws Exception {
        mock.setExpectedMessageCount(1);

        template.sendBody("direct:start", createRequest());

        mock.assertIsSatisfied();
        Exchange result = mock.assertExchangeReceived(0);

        byte[] body = (byte[]) result.getIn().getBody(byte[].class);
        InputStream stream = new ByteArrayInputStream(body);
        SOAPMessage request = MessageFactory.newInstance().createMessage(null, stream);
        assertTrue(null != request.getSOAPHeader()
                && request.getSOAPHeader().extractAllHeaderElements().hasNext(),
                "Expected headers");
    }

    private InputStream createRequest() throws Exception {
        InputStream stream = this.getClass().getResourceAsStream("SoapMarshalHeadersTest.xml");
        return stream;
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
                
                public void configure() {
                    from("direct:start").unmarshal(soapjaxbModel).marshal(soapjaxbModelDontIgnoreUnmarshalled)
                            .to("mock:end");
                }
            };
        }
    }
}
