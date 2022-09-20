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




import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.mtom.HelloImpl;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfMtomPOJOProducerTest.class,
        CxfMtomPOJOProducerTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfMtomPOJOProducerTest {

    protected final QName SERVICE_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloService");
    protected final QName PORT_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloPort");
    private static final Logger LOG = LoggerFactory.getLogger(CxfMtomPOJOProducerTest.class);

    @Autowired
    protected CamelContext context;
    private Endpoint endpoint;
    
    static int port = CXFTestSupport.getPort1();
    

    @BeforeEach
    public void setUp() throws Exception {
        endpoint = Endpoint.publish("/CxfMtomPOJOProducerTest/jaxws-mtom/hello", getImpl());
        SOAPBinding binding = (SOAPBinding) endpoint.getBinding();
        binding.setMTOMEnabled(true);

    }

    @AfterEach
    public void tearDown() throws Exception {
        if (endpoint != null) {
            endpoint.stop();
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testInvokingServiceFromCxfProducer() throws Exception {
        if (MtomTestHelper.isAwtHeadless(null, LOG)) {
            return;
        }

        final Holder<byte[]> photo = new Holder<>(MtomTestHelper.REQ_PHOTO_DATA);
        final Holder<Image> image = new Holder<>(getImage("/java.jpg"));

        Exchange exchange = context.createProducerTemplate().send("direct://testEndpoint", new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new Object[] { photo, image });

            }

        });

        assertEquals(2, exchange.getMessage(AttachmentMessage.class).getAttachments().size(),
                "The attachement size should be 2");

        Object[] result = exchange.getMessage().getBody(Object[].class);
        Holder<byte[]> photo1 = (Holder<byte[]>) result[1];
        assertArrayEquals(MtomTestHelper.RESP_PHOTO_DATA, photo1.value);
        Holder<Image> image1 = (Holder<Image>) result[2];
        assertNotNull(image1.value);
        if (image.value instanceof BufferedImage) {
            assertEquals(560, ((BufferedImage) image1.value).getWidth());
            assertEquals(300, ((BufferedImage) image1.value).getHeight());
        }

    }

    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }

    protected Object getImpl() {
        return new HelloImpl();
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
        public CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/" 
                + "CxfMtomPOJOProducerTest" + "/jaxws-mtom/hello");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            cxfEndpoint.setServiceClass(org.apache.camel.cxf.mtom_feature.Hello.class);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("mtom-enabled", true);
            properties.put("loggingFeatureEnabled", false);
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:testEndpoint").
                    to("cxf:bean:serviceEndpoint?defaultOperationName=Detail");
                }
            };
        }
    }
    
    
}
