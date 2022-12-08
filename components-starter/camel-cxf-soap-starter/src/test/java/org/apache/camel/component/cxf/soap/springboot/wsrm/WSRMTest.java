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
package org.apache.camel.component.cxf.soap.springboot.wsrm;




import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.component.cxf.wsrm.HelloWorld;
import org.apache.camel.component.cxf.wsrm.HelloWorldImpl;
import org.apache.camel.component.cxf.wsrm.MessageLossSimulator;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        WSRMTest.class,
        WSRMTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WSRMTest {

    private static Logger logger = LoggerFactory.getLogger(WSRMTest.class);
    private String namespace = "http://camel.apache.org/cxf/wsrm";
    private QName serviceName = new QName(namespace, "HelloWorldService");
    private QName endpointName = new QName(namespace, "HelloWorldPort");
    
    static int port = CXFTestSupport.getPort1();
    
    @Test
    public void testWSRM() throws Exception {
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost:" + port 
                              + "/services/wsrm/HelloWorld");
        clientBean.setServiceClass(HelloWorld.class);
        clientBean.setWsdlURL(WSRMTest.class.getResource("/HelloWorld.wsdl").toString());
        proxyFactory.getFeatures().add(new org.apache.cxf.ws.addressing.WSAddressingFeature());
        proxyFactory.getFeatures().add(new LoggingFeature());
        org.apache.cxf.ws.rm.feature.RMFeature rmFeature = new org.apache.cxf.ws.rm.feature.RMFeature();
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(Long.valueOf(4000));
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(Long.valueOf(2000));

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        AcksPolicyType acksPolicy = new AcksPolicyType();
        acksPolicy.setIntraMessageThreshold(0);
        DestinationPolicyType destinationPolicy = new DestinationPolicyType();
        destinationPolicy.setAcksPolicy(acksPolicy);

        rmFeature.setRMAssertion(rmAssertion);
        rmFeature.setDestinationPolicy(destinationPolicy);
        proxyFactory.getFeatures().add(rmFeature);
        proxyFactory.getOutInterceptors().add(new MessageLossSimulator());
        HelloWorld helloWorld = (HelloWorld) proxyFactory.create();
        Client client = ClientProxy.getClient(helloWorld);
        String decoupledEndpoint = "/wsrm/decoupled_endpoint";
        client.getBus().setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY, 
                                       "http://localhost:" + port + "/services");
        HTTPConduit hc = (HTTPConduit)(client.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);
        String result = helloWorld.sayHi("world1!");
        assertEquals("Hello world1!", result, "Get a wrong response");
        result = helloWorld.sayHi("world2!");//second call will trigger MessageLoss and resend
        assertEquals("Hello world2!", result, "Get a wrong response");
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
        
        
        @Bean("helloWorld")
        CxfEndpoint getCxfEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(HelloWorldImpl.class);
            cxfEndpoint.setWsdlURL(WSRMTest.class.getResource("/HelloWorld.wsdl").toString());
            cxfEndpoint.setAddress("/wsrm/HelloWorld");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            cxfEndpoint.getFeatures().add(new org.apache.cxf.ws.addressing.WSAddressingFeature());
            
            org.apache.cxf.ws.rm.feature.RMFeature rmFeature = new org.apache.cxf.ws.rm.feature.RMFeature();
            RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
            baseRetransmissionInterval.setMilliseconds(Long.valueOf(4000));
            RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
            acknowledgementInterval.setMilliseconds(Long.valueOf(2000));

            RMAssertion rmAssertion = new RMAssertion();
            rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
            rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

            AcksPolicyType acksPolicy = new AcksPolicyType();
            acksPolicy.setIntraMessageThreshold(0);
            DestinationPolicyType destinationPolicy = new DestinationPolicyType();
            destinationPolicy.setAcksPolicy(acksPolicy);

            rmFeature.setRMAssertion(rmAssertion);
            rmFeature.setDestinationPolicy(destinationPolicy);
            cxfEndpoint.getFeatures().add(rmFeature);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:helloWorld").process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                            logger.info("***** Entering Processor *******");
                            String name = exchange.getIn().getBody(String.class);
                            exchange.getMessage().setBody("Hello " + name);
                            logger.info("***** Leaving Processor *******");
                        }
                    });
                }
            };
        }
    }
    
    
}
