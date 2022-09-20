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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.mtom.HelloImpl;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.cxf.mtom_feature.Hello;
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

import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfMtomDisabledProducerPayloadModeTest.class,
        CxfMtomDisabledProducerPayloadModeTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfMtomDisabledProducerPayloadModeTest {

    protected final QName SERVICE_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloService");
    protected final QName PORT_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloPort");
    private static final Logger LOG = LoggerFactory.getLogger(CxfMtomDisabledProducerPayloadModeTest.class);

    @Autowired
    protected CamelContext context;
    private Endpoint endpoint;
    
    static int port = CXFTestSupport.getPort1();
    
    
    

    @BeforeEach
    public void setUp() throws Exception {
        endpoint = Endpoint.publish("/" + getClass().getSimpleName()
                                    + "/jaxws-mtom/hello",
                getServiceImpl());
        SOAPBinding binding = (SOAPBinding) endpoint.getBinding();
        binding.setMTOMEnabled(isMtomEnabled());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    @Test
    public void testProducer() throws Exception {
        if (MtomTestHelper.isAwtHeadless(null, LOG)) {
            return;
        }

        Exchange exchange = context.createProducerTemplate().send("direct:testEndpoint", new Processor() {

            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                List<Source> elements = new ArrayList<>();
                elements.add(new DOMSource(
                        StaxUtils.read(new StringReader(MtomTestHelper.MTOM_DISABLED_REQ_MESSAGE)).getDocumentElement()));
                CxfPayload<SoapHeader> body = new CxfPayload<>(
                        new ArrayList<SoapHeader>(),
                        elements, null);
                exchange.getIn().setBody(body);
                exchange.getIn(AttachmentMessage.class).addAttachment(MtomTestHelper.REQ_PHOTO_CID,
                        new DataHandler(new ByteArrayDataSource(MtomTestHelper.REQ_PHOTO_DATA, "application/octet-stream")));

                exchange.getIn(AttachmentMessage.class).addAttachment(MtomTestHelper.REQ_IMAGE_CID,
                        new DataHandler(new ByteArrayDataSource(MtomTestHelper.requestJpeg, "image/jpeg")));

            }

        });

        // process response - verify response attachments

        CxfPayload<?> out = exchange.getMessage().getBody(CxfPayload.class);
        assertEquals(1, out.getBody().size());

        DataHandler dr = exchange.getMessage(AttachmentMessage.class).getAttachment(MtomTestHelper.RESP_PHOTO_CID);
        assertEquals("application/octet-stream", dr.getContentType());
        assertArrayEquals(MtomTestHelper.RESP_PHOTO_DATA, IOUtils.readBytesFromStream(dr.getInputStream()));

        dr = exchange.getMessage(AttachmentMessage.class).getAttachment(MtomTestHelper.RESP_IMAGE_CID);
        assertEquals("image/jpeg", dr.getContentType());

        BufferedImage image = ImageIO.read(dr.getInputStream());
        assertEquals(560, image.getWidth());
        assertEquals(300, image.getHeight());

    }

    public static class MyHelloImpl extends HelloImpl implements Hello {

        @Resource
        WebServiceContext ctx;

        @Override
        public void detail(Holder<byte[]> photo, Holder<Image> image) {

            // verify request attachments
            Map<String, DataHandler> map
                    = CastUtils.cast((Map<?, ?>) ctx.getMessageContext().get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS));
            assertEquals(2, map.size());

            DataHandler dh = map.get(MtomTestHelper.REQ_PHOTO_CID);
            assertEquals("application/octet-stream", dh.getContentType());
            byte[] bytes = null;
            try {
                bytes = IOUtils.readBytesFromStream(dh.getInputStream());
            } catch (IOException e) {
                LOG.warn("I/O error reading bytes from stream: {}", e.getMessage(), e);
            }
            assertArrayEquals(MtomTestHelper.REQ_PHOTO_DATA, bytes);

            dh = map.get(MtomTestHelper.REQ_IMAGE_CID);
            assertEquals("image/jpeg", dh.getContentType());

            BufferedImage bufferedImage = null;
            try {
                bufferedImage = ImageIO.read(dh.getInputStream());

            } catch (IOException e) {
                LOG.warn("I/O error reading bytes from stream: {}", e.getMessage(), e);
            }
            assertNotNull(bufferedImage);
            assertEquals(41, bufferedImage.getWidth());
            assertEquals(39, bufferedImage.getHeight());

            // add output attachments
            map = CastUtils.cast((Map<?, ?>) ctx.getMessageContext().get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS));

            try {
                DataSource ds = new AttachmentDataSource("image/jpeg", getClass().getResourceAsStream("/Splash.jpg"));
                map.put(MtomTestHelper.RESP_IMAGE_CID, new DataHandler(ds));

            } catch (Exception e) {
                LOG.warn("I/O error: {}", e.getMessage(), e);
            }

            try {
                DataSource ds = new AttachmentDataSource(
                        "application/octet-stream",
                        new ByteArrayInputStream(MtomTestHelper.RESP_PHOTO_DATA));
                map.put(MtomTestHelper.RESP_PHOTO_CID, new DataHandler(ds));

            } catch (Exception e) {
                LOG.warn("I/O error: {}", e.getMessage(), e);
            }

        }
    }

    
    protected boolean isMtomEnabled() {
        return false;
    }

    protected Object getServiceImpl() {
        return new MyHelloImpl();
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
                + "CxfMtomDisabledProducerPayloadModeTest" + "/jaxws-mtom/hello");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
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
