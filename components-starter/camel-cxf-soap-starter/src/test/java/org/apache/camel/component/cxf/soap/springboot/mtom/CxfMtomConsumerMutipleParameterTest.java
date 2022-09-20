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

import java.awt.image.BufferedImage;
import java.util.List;

import java.awt.Image;
import javax.imageio.ImageIO;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfMtomConsumerMutipleParameterTest.class,
                           CxfMtomConsumerMutipleParameterTest.TestConfiguration.class, 
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfMtomConsumerMutipleParameterTest {

    protected String MTOM_ENDPOINT_ADDRESS = "/CxfMtomConsumerTest/jaxws-mtom/hello";
    protected String MTOM_ENDPOINT_URI = "cxf://" + MTOM_ENDPOINT_ADDRESS
                                                      + "?serviceClass=org.apache.camel.component.cxf.soap.springboot.mtom.ImageService&properties.mtom-enabled=true";
    private static final Logger LOG = LoggerFactory.getLogger(CxfMtomConsumerMutipleParameterTest.class);

    static int port = CXFTestSupport.getPort1();
    
    

    
    private ImageService getPort() {
              
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost:" + port + "/services" + MTOM_ENDPOINT_ADDRESS);
        clientBean.setServiceClass(ImageService.class);
        proxyFactory.getFeatures().add(new LoggingFeature());
        ImageService imageService = (ImageService) proxyFactory.create();
        return imageService;
    }

    protected Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }

        
    @Test
    public void testInvokingServiceBare() throws Exception {
        if (MtomTestHelper.isAwtHeadless(null, LOG)) {
            return;
        }

        Image image = getImage("/java.jpg");
        
        ImageService port = getPort();

        SOAPBinding binding = (SOAPBinding) ((BindingProvider) port).getBinding();
        binding.setMTOMEnabled(true);

        String ret = port.uploadImage(image, "RequestFromCXF");

        assertEquals(ret,
                     "RequestFromCXF hello", "Should get the right response");

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
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(MTOM_ENDPOINT_URI).process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                            AttachmentMessage in = exchange.getIn(AttachmentMessage.class);
                            assertNull(in.getAttachments(), "We should not get any attachements here.");
                            assertEquals("application/xop+xml", in.getHeader("Content-Type"), "Get a wrong Content-Type header");
                            // Get the parameter list
                            List<?> parameter = in.getBody(List.class);
                            // Get the operation name
                            String name = (String)parameter.get(1);
                            assertNotNull(name, "The name should not be null");
                            assertEquals(name,
                                    "RequestFromCXF", "Should get the right request");
                            BufferedImage image = (BufferedImage) parameter.get(0);
                            assertNotNull(image, "We should get the image here");
                            exchange.getMessage().setBody(name + " hello");

                        }
                    });
                }
            };
        }
    }

}
