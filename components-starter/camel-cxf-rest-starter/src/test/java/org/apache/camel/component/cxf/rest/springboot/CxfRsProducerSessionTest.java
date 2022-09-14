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
package org.apache.camel.component.cxf.rest.springboot;


import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfRsProducerSessionTest.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
              "camel.springboot.routes-include-pattern=file:src/test/resources/routes/CxfRsSpringProducerSessionRoute.xml"}
)
    
@ImportResource({
                 "classpath:routes/CxfRsSpringProducerSession.xml"
})
public class CxfRsProducerSessionTest {

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return new UndertowServletWebServerFactory();
    }
    
    @Test
    public void testNoSessionProxy() {
        String response = sendMessage("direct://proxy", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("New New World", response);
        response = sendMessage("direct://proxy", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("New New World", response);
    }

    @Test
    public void testExchangeSessionProxy() {
        String response = sendMessage("direct://proxyexchange", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("Old New World", response);
        response = sendMessage("direct://proxyexchange", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("Old New World", response);
    }

    @Test
    public void testInstanceSession() {
        String response = sendMessage("direct://proxyinstance", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("Old New World", response);
        response = sendMessage("direct://proxyinstance", "World", Boolean.FALSE).getMessage().getBody(String.class);
        assertEquals("Old Old World", response);
        // we do the instance tests for proxy and http in one test because order
        // matters here
        response = sendMessage("direct://httpinstance", "World", Boolean.TRUE).getMessage().getBody(String.class);
        assertEquals("Old Old World", response);
    }

    @Test
    public void testNoSessionHttp() {
        String response = sendMessage("direct://http", "World", Boolean.TRUE).getMessage().getBody(String.class);
        assertEquals("New New World", response);
        response = sendMessage("direct://http", "World", Boolean.TRUE).getMessage().getBody(String.class);
        assertEquals("New New World", response);
    }

    @Test
    public void testExchangeSessionHttp() {
        String response = sendMessage("direct://httpexchange", "World", Boolean.TRUE).getMessage().getBody(String.class);
        assertEquals("Old New World", response);
        response = sendMessage("direct://httpexchange", "World", Boolean.TRUE).getMessage().getBody(String.class);
        assertEquals("Old New World", response);
    }

    @Autowired
    ProducerTemplate template;
    
    private Exchange sendMessage(String endpoint, String body, Boolean httpApi) {
        Exchange exchange = template.send(endpoint, new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                Message inMessage = exchange.getIn();
                inMessage.setHeader(CxfConstants.OPERATION_NAME, "echo");
                inMessage.setHeader(Exchange.HTTP_METHOD, "POST");
                inMessage.setHeader(Exchange.HTTP_PATH, "/echoservice/echo");
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, httpApi);
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, String.class);
                inMessage.setBody(body);
            }
        });
        return exchange;
    }
}
