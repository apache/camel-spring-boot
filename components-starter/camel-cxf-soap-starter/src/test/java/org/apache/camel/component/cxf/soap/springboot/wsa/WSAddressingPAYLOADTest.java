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
package org.apache.camel.component.cxf.soap.springboot.wsa;




import java.util.List;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        WSAddressingPAYLOADTest.class,
        WSAddressingPAYLOADTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WSAddressingPAYLOADTest {

    private String namespace = "http://apache.org/hello_world_soap_http";
    private QName serviceName = new QName(namespace, "SOAPService");
    private QName endpointName = new QName(namespace, "SoapPort");
    
    @Autowired
    protected ProducerTemplate template;

    private Server serviceEndpoint;
    
    static int port = CXFTestSupport.getPort1();
    
    
    @BeforeEach
    public void setUp() throws Exception {
        JaxWsServerFactoryBean svrBean = new JaxWsServerFactoryBean();
        svrBean.setAddress("/WSAddressingPAYLOADTest/SoapContext/backendService");
        svrBean.setServiceClass(Greeter.class);
        svrBean.setServiceBean(new GreeterImpl());
        svrBean.getFeatures().add(new LoggingFeature());
        serviceEndpoint = svrBean.create();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (serviceEndpoint != null) {
            serviceEndpoint.stop();
        }
    }
    
    @Test
    public void testWSAddressing() throws Exception {
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost:" + port + "/services/WSAddressingPAYLOADTest/SoapContext/SoapPort");
        clientBean.setServiceClass(Greeter.class);
        WSAddressingFeature addressingFeature = new WSAddressingFeature();
        addressingFeature.setUsingAddressingAdvisory(true);
        proxyFactory.getFeatures().add(addressingFeature);
        proxyFactory.getFeatures().add(new LoggingFeature());
        Greeter greeter = (Greeter) proxyFactory.create();
        Client client = ClientProxy.getClient(greeter);
        String decoupledEndpoint = "http://localhost:"
            + TestUtil.getPortNumber("decoupled") + "/wsa/decoupled_endpoint";
        HTTPConduit hc = (HTTPConduit)(client.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);
        String result = greeter.greetMe("world!");
        assertEquals("Hello world!", result, "Get a wrong response");
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
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setWsdlURL(WSAddressingPAYLOADTest.class.getResource("/wsdl/hello_world.wsdl").toString());
            cxfEndpoint.setAddress("/WSAddressingPAYLOADTest/SoapContext/SoapPort");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            cxfEndpoint.getFeatures().add(new org.apache.cxf.ws.addressing.WSAddressingFeature());
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setWsdlURL(WSAddressingPAYLOADTest.class.getResource("/wsdl/hello_world.wsdl").toString());
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/WSAddressingPAYLOADTest/SoapContext/backendService");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint").process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                            List<?> headerList = (List<?>) exchange.getIn().getHeader(Header.HEADER_LIST);
                            assertNotNull(headerList, "We should get the header list.");
                            assertEquals(4, headerList.size(), "Get a wrong size of header list.");
                        }
                    }).to("cxf:bean:serviceEndpoint");
                }
            };
        }
    }
    
    
}
