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




import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.HelloService;
import org.apache.camel.component.cxf.HelloServiceImpl;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        FileToCxfMessageDataFormatTest.class,
        FileToCxfMessageDataFormatTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class FileToCxfMessageDataFormatTest {

    
    private static final Logger LOG = LoggerFactory.getLogger(FileToCxfMessageDataFormatTest.class);
    
    private Server server;
    
    static int port = CXFTestSupport.getPort1();

    @BeforeEach
    public void setUp() throws Exception {
        deleteDirectory("target/filetocxf");

        // set CXF
        ServerFactoryBean factory = new ServerFactoryBean();

        factory.setAddress("/FileToCxfMessageDataFormatTest/router");
        factory.setServiceClass(HelloService.class);
        factory.setServiceBean(new HelloServiceImpl());

        server = factory.create();
        server.start();

    }

    @AfterEach
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
    }

    
    
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Autowired
    ProducerTemplate template;
    
    @Test
    public void testFileToCxfMessageDataFormat() throws Exception {
        
        mock.expectedMessageCount(1);

        template.sendBodyAndHeader("file:target/filetocxf", createBody(), Exchange.FILE_NAME, "payload.xml");

        mock.assertIsSatisfied();

        String out = mock.getReceivedExchanges().get(0).getIn().getBody(String.class);
        assertNotNull(out);
        LOG.info("Reply payload as a String:\n" + out);
        assertTrue(out.contains("echo Camel"), "Should invoke the echo operation");
    }

    private String createBody() throws Exception {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://cxf.component.camel.apache.org/\">\n"
               + "   <soapenv:Header/>\n"
               + "   <soapenv:Body>\n"
               + "      <cxf:echo>\n"
               + "          <cxf:arg0>Camel</cxf:arg0>\n"
               + "      </cxf:echo>\n"
               + "   </soapenv:Body>\n"
               + "</soapenv:Envelope>";
    }
    
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
        
        
        @Bean
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services" + "/FileToCxfMessageDataFormatTest/router");
            cxfEndpoint.setDataFormat(DataFormat.RAW);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("file:target/filetocxf")
                    .to("log:request")
                    .to(ExchangePattern.InOut, "routerEndpoint")
                    .to("log:reply")
                    .to("mock:result");
                }
            };
        }
    }
    
    
}
