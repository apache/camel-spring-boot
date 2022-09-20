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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfMtomConsumerPayloadModeTest.class,
                           CxfMtomConsumerPayloadModeTest.TestConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfMtomConsumerPayloadModeTest {
    
    private final QName SERVICE_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloService");
    private final QName PORT_QNAME = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloPort");
    
    
    
    private static final Logger LOG = LoggerFactory.getLogger(CxfMtomConsumerPayloadModeTest.class);

    static int port = CXFTestSupport.getPort1();
    
    @Autowired
    private ProducerTemplate template;

    @Test
    public void testConsumer() throws Exception {
        if (MtomTestHelper.isAwtHeadless(null, LOG)) {
            return;
        }

        Exchange exchange = template.send("cxf:bean:consumerEndpoint", new Processor() {

            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                List<Source> elements = new ArrayList<>();
                elements.add(new DOMSource(StaxUtils.read(new StringReader(getRequestMessage())).getDocumentElement()));
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
        assertEquals(exchange.getIn(AttachmentMessage.class).getAttachments().size(), 2);
    }

    protected String getRequestMessage() {
        return MtomTestHelper.REQ_MESSAGE;
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
        public CxfEndpoint consumerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("/" + getClass().getSimpleName()+ "/jaxws-mtom/hello");
            cxfEndpoint.setWsdlURL("mtom.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
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
                    from("cxf:bean:consumerEndpoint").process(new Processor() {
                        
                        @SuppressWarnings("unchecked")
                        public void process(Exchange exchange) throws Exception {
                            CxfPayload<SoapHeader> in = exchange.getIn().getBody(CxfPayload.class);

                            // verify request
                            assertEquals(1, in.getBody().size());

                            Map<String, String> ns = new HashMap<>();
                            ns.put("ns", MtomTestHelper.SERVICE_TYPES_NS);
                            ns.put("xop", MtomTestHelper.XOP_NS);

                            XPathUtils xu = new XPathUtils(ns);
                            Element body = new XmlConverter().toDOMElement(in.getBody().get(0));
                            Element ele = (Element) xu.getValue("//ns:Detail/ns:photo/xop:Include", body,
                                    XPathConstants.NODE);
                            String photoId = ele.getAttribute("href").substring(4); // skip "cid:"
                            assertEquals(MtomTestHelper.REQ_PHOTO_CID, photoId);

                            ele = (Element) xu.getValue("//ns:Detail/ns:image/xop:Include", body,
                                    XPathConstants.NODE);
                            String imageId = ele.getAttribute("href").substring(4); // skip "cid:"
                            assertEquals(MtomTestHelper.REQ_IMAGE_CID, imageId);

                            DataHandler dr = exchange.getIn(AttachmentMessage.class).getAttachment(photoId);
                            assertEquals("application/octet-stream", dr.getContentType());
                            assertArrayEquals(MtomTestHelper.REQ_PHOTO_DATA, IOUtils.readBytesFromStream(dr.getInputStream()));

                            dr = exchange.getIn(AttachmentMessage.class).getAttachment(imageId);
                            assertEquals("image/jpeg", dr.getContentType());
                            assertArrayEquals(MtomTestHelper.requestJpeg, IOUtils.readBytesFromStream(dr.getInputStream()));

                            // create response
                            List<Source> elements = new ArrayList<>();
                            elements.add(new DOMSource(StaxUtils.read(new StringReader(MtomTestHelper.RESP_MESSAGE)).getDocumentElement()));
                            CxfPayload<SoapHeader> sbody = new CxfPayload<>(
                                    new ArrayList<SoapHeader>(),
                                    elements, null);
                            exchange.getIn().setBody(sbody);
                            exchange.getIn(AttachmentMessage.class).addAttachment(MtomTestHelper.RESP_PHOTO_CID,
                                    new DataHandler(new ByteArrayDataSource(MtomTestHelper.RESP_PHOTO_DATA, "application/octet-stream")));

                            exchange.getIn(AttachmentMessage.class).addAttachment(MtomTestHelper.RESP_IMAGE_CID,
                                    new DataHandler(new ByteArrayDataSource(MtomTestHelper.responseJpeg, "image/jpeg")));

                        }
                    });
                }
            };
        }
    }

}
