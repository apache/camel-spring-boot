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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.mtom.HelloImpl;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.cxf.mtom_feature.Hello;
import org.apache.camel.cxf.mtom_feature.HelloService;
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
        CxfMtomRouterPayloadModeTest.class,
        CxfMtomRouterPayloadModeTest.TestConfiguration.class,
        CxfMtomRouterPayloadModeTest.EndpointConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfMtomRouterPayloadModeTest {

    protected final QName SERVICE_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloService");
    protected final QName PORT_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloPort");
    private static final Logger LOG = LoggerFactory.getLogger(CxfMtomRouterPayloadModeTest.class);

    @Autowired
    protected CamelContext context;
    private Endpoint endpoint;
    
    static int port = CXFTestSupport.getPort1();
    
    

    @BeforeEach
    public void setUp() throws Exception {
        endpoint = Endpoint.publish("/"
                                    + getClass().getSimpleName() + "/jaxws-mtom/backend",
                getImpl());
        SOAPBinding binding = (SOAPBinding) endpoint.getBinding();
        binding.setMTOMEnabled(true);

    }

    @AfterEach
    public void tearDown() throws Exception {
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    @Test
    public void testInvokingServiceFromCXFClient() throws Exception {
        if (MtomTestHelper.isAwtHeadless(null, LOG)) {
            return;
        }

        Holder<byte[]> photo = new Holder<>(MtomTestHelper.REQ_PHOTO_DATA);
        Holder<Image> image = new Holder<>(getImage("/java.jpg"));

        Hello port = getPort();

        SOAPBinding binding = (SOAPBinding) ((BindingProvider) port).getBinding();
        binding.setMTOMEnabled(true);

        port.detail(photo, image);

        assertArrayEquals(MtomTestHelper.RESP_PHOTO_DATA, photo.value);
        assertNotNull(image.value);
        if (image.value instanceof BufferedImage) {
            assertEquals(560, ((BufferedImage) image.value).getWidth());
            assertEquals(300, ((BufferedImage) image.value).getHeight());
        }

    }

    protected Hello getPort() {
        URL wsdl = getClass().getResource("/mtom.wsdl");
        assertNotNull(wsdl, "WSDL is null");

        HelloService service = new HelloService(wsdl, HelloService.SERVICE);
        assertNotNull(service, "Service is null");
        Hello hello = service.getHelloPort();
        ((BindingProvider) hello).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMtomRouterPayloadModeTest/jaxws-mtom/hello");
        return hello;
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
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint").
                    to("cxf:bean:serviceEndpoint");
                }
            };
        }
    }
    
    @Configuration
    class EndpointConfiguration {
        @Bean
        public CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("/" + "CxfMtomRouterPayloadModeTest"
                + "/jaxws-mtom/hello");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            properties.put("mtom-enabled", "true");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        public CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/" 
                + "CxfMtomRouterPayloadModeTest" + "/jaxws-mtom/backend");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            properties.put("mtom-enabled", "true");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
    }
}
