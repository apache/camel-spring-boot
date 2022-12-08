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
package org.apache.camel.component.cxf.soap.springboot.soap.headers;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import org.w3c.dom.Node;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.soap.headers.Constants;
import org.apache.camel.component.cxf.soap.headers.CustomHeaderFilter;
import org.apache.camel.component.cxf.soap.headers.HeaderService;
import org.apache.camel.component.cxf.soap.headers.HeaderTester;
import org.apache.camel.component.cxf.soap.headers.HeaderTesterImpl;
import org.apache.camel.component.cxf.soap.headers.HeaderTesterWithInsertionImpl;
import org.apache.camel.component.cxf.soap.headers.InHeader;
import org.apache.camel.component.cxf.soap.headers.InHeaderResponse;
import org.apache.camel.component.cxf.soap.headers.InoutHeader;
import org.apache.camel.component.cxf.soap.headers.InoutHeaderResponse;
import org.apache.camel.component.cxf.soap.headers.Me;
import org.apache.camel.component.cxf.soap.headers.OutHeader;
import org.apache.camel.component.cxf.soap.headers.OutHeaderResponse;
import org.apache.camel.component.cxf.soap.headers.SOAPHeaderData;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.component.cxf.transport.header.CxfHeaderFilterStrategy;
import org.apache.camel.component.cxf.transport.header.MessageHeaderFilter;
import org.apache.camel.component.cxf.transport.header.SoapMessageHeaderFilter;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.DefaultExchange;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.Header.Direction;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.outofband.header.OutofBandHeader;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfMessageHeadersRelayTest.class,
                           CxfMessageHeadersRelayTest.TestConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfMessageHeadersRelayTest {

    private static QName SERVICENAME = QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}HeaderService");
    
    static int port = CXFTestSupport.getPort1();
        

    private static final Logger LOG = LoggerFactory.getLogger(CxfMessageHeadersRelayTest.class);

    @Autowired
    protected CamelContext context;
    
    @Autowired
    protected ProducerTemplate template;

    private Endpoint relayEndpoint;
    private Endpoint noRelayEndpoint;
    private Endpoint relayEndpointWithInsertion;

    @BeforeEach
    public void setUp() throws Exception {
        relayEndpoint = Endpoint.publish("/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointBackend",
                new HeaderTesterImpl());
        noRelayEndpoint = Endpoint.publish("/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpointBackend",
                new HeaderTesterImpl(false));
        relayEndpointWithInsertion = Endpoint.publish("/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointWithInsertionBackend",
                new HeaderTesterWithInsertionImpl());

    }

    @AfterEach
    public void tearDown() throws Exception {

        if (relayEndpoint != null) {
            relayEndpoint.stop();
            relayEndpoint = null;
        }

        if (noRelayEndpoint != null) {
            noRelayEndpoint.stop();
            noRelayEndpoint = null;
        }

        if (relayEndpointWithInsertion != null) {
            relayEndpointWithInsertion.stop();
            relayEndpointWithInsertion = null;
        }
    }

    protected static void addOutOfBoundHeader(HeaderTester proxy, boolean invalid) throws JAXBException {
        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        BindingProvider bp = null;

        try {
            if (handler instanceof BindingProvider) {
                bp = (BindingProvider) handler;
                Map<String, Object> requestContext = bp.getRequestContext();
                requestContext.put(Header.HEADER_LIST, buildOutOfBandHeaderList(invalid));
            }
        } catch (JAXBException ex) {
            throw ex;
        }

    }

    @Test
    public void testInHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
        InHeader me = new InHeader();
        me.setRequestType("CXF user");
        InHeaderResponse response = proxy.inHeader(me, Constants.IN_HEADER_DATA);
        assertEquals("pass", response.getResponseType(), "Expected in band header to propagate but it didn't");
    }

    @Test
    public void testOutHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");

        OutHeader me = new OutHeader();
        me.setRequestType("CXF user");
        Holder<OutHeaderResponse> result = new Holder<>(new OutHeaderResponse());
        Holder<SOAPHeaderData> header = new Holder<>(new SOAPHeaderData());
        proxy.outHeader(me, result, header);
        assertEquals("pass", result.value.getResponseType(), "Expected in band header to propagate but it didn't");
        assertTrue(Constants.equals(Constants.OUT_HEADER_DATA, header.value),
                "Expected in band response header to propagate but it either didn't "
                                                                              + " or its contents do not match");
    }

    @Test
    public void testInOutHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
        InoutHeader me = new InoutHeader();
        me.setRequestType("CXF user");
        Holder<SOAPHeaderData> header = new Holder<>(Constants.IN_OUT_REQUEST_HEADER_DATA);
        InoutHeaderResponse result = proxy.inoutHeader(me, header);
        assertEquals("pass", result.getResponseType(), "Expected in band header to propagate but it didn't");
        assertTrue(Constants.equals(Constants.IN_OUT_RESPONSE_HEADER_DATA, header.value),
                "Expected in band response header to propagate but it either didn't "
                                                                                          + " or its contents do not match");
    }

    @Test
    public void testInOutOfBandHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
        addOutOfBoundHeader(proxy, false);
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");

        Me response = proxy.inOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the out of band header to propagate but it didn't");
    }

    @Test
    public void testInoutOutOfBandHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
        addOutOfBoundHeader(proxy, false);
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Me response = proxy.inoutOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the out of band header to propagate but it didn't");
        validateReturnedOutOfBandHeader(proxy);
    }

    @Test
    public void testInoutOutOfBandHeaderCXFClientRelayWithHeaderInsertion() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelayWithInsertion();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointWithInsertion");
        addOutOfBoundHeader(proxy, false);
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Me response = proxy.inoutOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the out of band header to propagate but it didn't");

        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        BindingProvider bp = null;
        if (!(handler instanceof BindingProvider)) {
            fail("Unable to cast dynamic proxy InocationHandler to BindingProvider type");
        }

        bp = (BindingProvider) handler;
        Map<String, Object> responseContext = bp.getResponseContext();
        validateReturnedOutOfBandHeaderWithInsertion(responseContext, true);
    }

    @Test
    public void testOutOutOfBandHeaderCXFClientRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Me response = proxy.outOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the out of band header to propagate but it didn't");
        validateReturnedOutOfBandHeader(proxy);
    }

    @Test
    public void testInOutOfBandHeaderCXFClientNoRelay() throws Exception {

        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");

        addOutOfBoundHeader(proxy, false);
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Me response = proxy.inOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the in out of band header *not* to propagate but it did");

    }

    @Test
    public void testOutOutOfBandHeaderCXFClientNoRelay() throws Exception {

        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Thread.sleep(5000);
        Me response = proxy.outOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the out out of band header *not* to propagate but it did");
        validateReturnedOutOfBandHeader(proxy, false);
    }

    @Test
    public void testInoutOutOfBandHeaderCXFClientNoRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
        addOutOfBoundHeader(proxy, false);
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");
        Me response = proxy.inoutOutOfBandHeader(me);
        assertEquals("pass", response.getFirstName(), "Expected the in out of band header to *not* propagate but it did");
        validateReturnedOutOfBandHeader(proxy, false);
    }

    @Test
    public void testInHeaderCXFClientNoRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
        InHeader me = new InHeader();
        me.setRequestType("CXF user");
        InHeaderResponse response = null;
        try {
            response = proxy.inHeader(me, Constants.IN_HEADER_DATA);
        } catch (Exception e) {
            // do nothing
        }
        assertEquals("pass", response.getResponseType(), "Expected in in band header *not* to propagate but it did");
    }

    @Test
    public void testOutHeaderCXFClientNoRelay() throws Exception {
        Thread.sleep(5000);

        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
        OutHeader me = new OutHeader();
        me.setRequestType("CXF user");
        Holder<OutHeaderResponse> result = new Holder<>(new OutHeaderResponse());
        Holder<SOAPHeaderData> header = new Holder<>(new SOAPHeaderData());
        try {
            proxy.outHeader(me, result, header);
        } catch (Exception e) {
            // do nothing
        }
        assertEquals("pass", result.value.getResponseType(),
                "Ultimate remote HeaderTester.outHeader() destination was not reached");
        assertNull(header.value, "Expected in band response header *not* to propagate but it did");
    }

    @Test
    public void testInoutHeaderCXFClientNoRelay() throws Exception {
        HeaderService s = new HeaderService(
                getClass().getClassLoader().getResource("soap_header.wsdl"),
                HeaderService.SERVICE);
        HeaderTester proxy = s.getSoapPortNoRelay();
        ((BindingProvider) proxy).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
        InoutHeader me = new InoutHeader();
        me.setRequestType("CXF user");
        Holder<SOAPHeaderData> header = new Holder<>(Constants.IN_OUT_REQUEST_HEADER_DATA);
        InoutHeaderResponse result = null;
        try {
            result = proxy.inoutHeader(me, header);
        } catch (Exception e) {
            // do nothing
        }
        assertEquals("pass", result.getResponseType(), "Expected in band out header *not* to propagate but it did");
        assertNull(header.value, "Expected in band response header *not* to propagate but did");
    }

    @Test

    public void testInoutHeaderCXFClientNoServiceClassNoRelay() throws Exception {
        // TODO: Fix this test later
        QName qname = QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SOAPHeaderInfo");
        String uri = "cxf:bean:routerNoRelayNoServiceClassEndpoint?headerFilterStrategy=#dropAllMessageHeadersStrategy"
            + "&address=http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayNoServiceClassEndpoint";
        String requestHeader = "<ns2:SOAPHeaderInfo xmlns:ns2=\"http://apache.org/camel/"
                               + "component/cxf/soap/headers\"><originator>CxfSoapHeaderRoutePropagationTest.testInOutHeader Requestor"
                               + "</originator><message>Invoking CxfSoapHeaderRoutePropagationTest.testInOutHeader() Request"
                               + "</message></ns2:SOAPHeaderInfo>";
        String requestBody = "<ns2:inoutHeader xmlns:ns2=\"http://apache.org/camel/component/cxf/soap/headers\">"
                             + "<requestType>CXF user</requestType></ns2:inoutHeader>";
        List<Source> elements = new ArrayList<>();
        elements.add(new DOMSource(StaxUtils.read(new StringReader(requestBody)).getDocumentElement()));
        final List<SoapHeader> headers = new ArrayList<>();
        headers.add(new SoapHeader(
                qname,
                StaxUtils.read(new StringReader(requestHeader)).getDocumentElement()));
        final CxfPayload<SoapHeader> cxfPayload = new CxfPayload<>(headers, elements, null);

        Exchange exchange = template.request(uri, new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(cxfPayload);
                exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "inoutHeader");
                exchange.getIn().setHeader(Header.HEADER_LIST, headers);
            }

        });

        CxfPayload<?> out = exchange.getMessage().getBody(CxfPayload.class);
        assertEquals(1, out.getBodySources().size());

        assertTrue(out.getBodySources().get(0) instanceof DOMSource);

        assertEquals(0, out.getHeaders().size());

        String responseExp = "<ns2:inoutHeaderResponse xmlns:ns2=\"http://apache.org/camel/"
                             + "component/cxf/soap/headers\"><responseType>pass</responseType>"
                             + "</ns2:inoutHeaderResponse>";
        String response = StaxUtils.toString(out.getBody().get(0));
        //REVISIT use a more reliable comparison to tolerate some namespaces being added to the root element
        assertTrue(response.startsWith(responseExp.substring(0, 87))
                && response.endsWith(responseExp.substring(88, responseExp.length())), response);
    }

    @Test
    public void testMessageHeadersRelaysSpringContext() throws Exception {
        CxfEndpoint endpoint = context.getEndpoint(
                "cxf:bean:serviceExtraRelays?headerFilterStrategy=#customMessageFilterStrategy", CxfEndpoint.class);
        CxfHeaderFilterStrategy strategy = (CxfHeaderFilterStrategy) endpoint.getHeaderFilterStrategy();
        List<MessageHeaderFilter> filters = strategy.getMessageHeaderFilters();
        assertEquals(2, filters.size(), "Expected number of filters");
        Map<String, MessageHeaderFilter> messageHeaderFilterMap = strategy.getMessageHeaderFiltersMap();
        for (String ns : new CustomHeaderFilter().getActivationNamespaces()) {
            assertEquals(CustomHeaderFilter.class, messageHeaderFilterMap.get(ns).getClass(),
                    "Expected a filter class for namespace: " + ns);
        }
    }

    @Test
    public void testInOutOfBandHeaderCamelTemplateDirect() throws Exception {
        doTestInOutOfBandHeaderCamelTemplate("direct:directProducer");
    }

    @Test
    public void testOutOutOfBandHeaderCamelTemplateDirect() throws Exception {
        doTestOutOutOfBandHeaderCamelTemplate("direct:directProducer");
    }

    @Test
    public void testInOutOutOfBandHeaderCamelTemplateDirect() throws Exception {
        doTestInOutOutOfBandHeaderCamelTemplate("direct:directProducer");
    }

    @Test
    public void testInOutOfBandHeaderCamelTemplateRelay() throws Exception {
        doTestInOutOfBandHeaderCamelTemplate("direct:relayProducer");
    }

    @Test
    public void testOutOutOfBandHeaderCamelTemplateRelay() throws Exception {
        doTestOutOutOfBandHeaderCamelTemplate("direct:relayProducer");
    }

    @Test
    public void testInOutOutOfBandHeaderCamelTemplateRelay() throws Exception {
        doTestInOutOutOfBandHeaderCamelTemplate("direct:relayProducer");
    }

    protected void doTestInOutOfBandHeaderCamelTemplate(String producerUri) throws Exception {
        // START SNIPPET: sending
        Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
        final List<Object> params = new ArrayList<>();
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");

        params.add(me);
        senderExchange.getIn().setBody(params);
        senderExchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "inOutOfBandHeader");

        List<Header> headers = buildOutOfBandHeaderList(false);
        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put(Header.HEADER_LIST, headers);
        senderExchange.getIn().setHeader(Client.REQUEST_CONTEXT, requestContext);

        Exchange exchange = template.send(producerUri, senderExchange);

        org.apache.camel.Message out = exchange.getMessage();
        MessageContentsList result = (MessageContentsList) out.getBody();
        Map<String, Object> responseContext = CastUtils.cast((Map<?, ?>) out.getHeader(Client.RESPONSE_CONTEXT));
        assertNotNull(responseContext);
        assertTrue(result.get(0) != null && ((Me) result.get(0)).getFirstName().equals("pass"),
                "Expected the out of band header to propagate but it didn't");

    }

    protected void doTestOutOutOfBandHeaderCamelTemplate(String producerUri) throws Exception {
        // START SNIPPET: sending
        Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
        final List<Object> params = new ArrayList<>();
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");

        params.add(me);
        senderExchange.getIn().setBody(params);
        senderExchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "outOutOfBandHeader");

        Exchange exchange = template.send(producerUri, senderExchange);

        org.apache.camel.Message out = exchange.getMessage();
        MessageContentsList result = (MessageContentsList) out.getBody();
        assertTrue(result.get(0) != null && ((Me) result.get(0)).getFirstName().equals("pass"),
                "Expected the out of band header to propagate but it didn't");
        Map<String, Object> responseContext = CastUtils.cast((Map<?, ?>) out.getHeader(Client.RESPONSE_CONTEXT));
        assertNotNull(responseContext);
        validateReturnedOutOfBandHeader(responseContext);
    }

    public void doTestInOutOutOfBandHeaderCamelTemplate(String producerUri) throws Exception {
        // START SNIPPET: sending
        Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
        final List<Object> params = new ArrayList<>();
        Me me = new Me();
        me.setFirstName("john");
        me.setLastName("Doh");

        params.add(me);
        senderExchange.getIn().setBody(params);
        senderExchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "inoutOutOfBandHeader");

        List<Header> inHeaders = buildOutOfBandHeaderList(false);
        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put(Header.HEADER_LIST, inHeaders);
        senderExchange.getIn().setHeader(Client.REQUEST_CONTEXT, requestContext);

        Exchange exchange = template.send(producerUri, senderExchange);

        org.apache.camel.Message out = exchange.getMessage();
        MessageContentsList result = (MessageContentsList) out.getBody();
        assertTrue(result.get(0) != null && ((Me) result.get(0)).getFirstName().equals("pass"),
                "Expected the out of band header to propagate but it didn't");
        Map<String, Object> responseContext = CastUtils.cast((Map<?, ?>) out.getHeader(Client.RESPONSE_CONTEXT));
        assertNotNull(responseContext);
        validateReturnedOutOfBandHeader(responseContext);
    }

    protected static void validateReturnedOutOfBandHeader(HeaderTester proxy) {
        validateReturnedOutOfBandHeader(proxy, true);
    }

    protected static void validateReturnedOutOfBandHeader(HeaderTester proxy, boolean expect) {
        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        BindingProvider bp = null;
        if (!(handler instanceof BindingProvider)) {
            fail("Unable to cast dynamic proxy InocationHandler to BindingProvider type");
        }

        bp = (BindingProvider) handler;
        Map<String, Object> responseContext = bp.getResponseContext();
        validateReturnedOutOfBandHeader(responseContext, expect);
    }

    protected static void validateReturnedOutOfBandHeader(Map<String, Object> responseContext) {
        validateReturnedOutOfBandHeader(responseContext, true);
    }

    protected static void validateReturnedOutOfBandHeader(Map<String, Object> responseContext, boolean expect) {
        OutofBandHeader hdrToTest = null;
        List<Header> oobHdr = CastUtils.cast((List<?>) responseContext.get(Header.HEADER_LIST));
        if (!expect) {
            if (oobHdr == null || oobHdr != null && oobHdr.size() == 0) {
                return;
            }
            fail("Should have got *no* out-of-band headers, but some were found");
        }
        if (oobHdr == null) {
            fail("Should have got List of out-of-band headers");
        }

        assertEquals(1, oobHdr.size(), "HeaderHolder list expected to conain 1 object received " + oobHdr.size());

        for (Header hdr1 : oobHdr) {
            if (hdr1.getObject() instanceof Node) {
                try {
                    JAXBElement<?> job = (JAXBElement<?>) JAXBContext
                            .newInstance(org.apache.cxf.outofband.header.ObjectFactory.class)
                            .createUnmarshaller().unmarshal((Node) hdr1.getObject());
                    hdrToTest = (OutofBandHeader) job.getValue();
                } catch (JAXBException ex) {
                    LOG.warn("JAXB error: {}", ex.getMessage(), ex);
                }
            }
        }

        assertNotNull(hdrToTest, "out-of-band header should not be null");
        assertEquals("testOobReturnHeaderName", hdrToTest.getName(),
                "Expected out-of-band Header name testOobReturnHeaderName recevied :"
                                                                     + hdrToTest.getName());
        assertEquals("testOobReturnHeaderValue", hdrToTest.getValue(),
                "Expected out-of-band Header value testOobReturnHeaderValue recevied :"
                                                                       + hdrToTest.getValue());
        assertEquals("testReturnHdrAttribute", hdrToTest.getHdrAttribute(),
                "Expected out-of-band Header attribute testReturnHdrAttribute recevied :" + hdrToTest.getHdrAttribute());
    }

    protected static List<Header> buildOutOfBandHeaderList(boolean invalid) throws JAXBException {
        OutofBandHeader ob = new OutofBandHeader();
        ob.setName("testOobHeader");
        ob.setValue("testOobHeaderValue");
        ob.setHdrAttribute(invalid ? "dontProcess" : "testHdrAttribute");

        SoapHeader hdr = new SoapHeader(
                new QName(Constants.TEST_HDR_NS, Constants.TEST_HDR_REQUEST_ELEM),
                ob,
                new JAXBDataBinding(ob.getClass()));

        hdr.setMustUnderstand(invalid);

        List<Header> headers = new ArrayList<>();
        headers.add(hdr);
        return headers;
    }

    protected static void validateReturnedOutOfBandHeaderWithInsertion(Map<String, Object> responseContext, boolean expect) {
        List<OutofBandHeader> hdrToTest = new ArrayList<>();
        List<Header> oobHdr = CastUtils.cast((List<?>) responseContext.get(Header.HEADER_LIST));
        if (!expect) {
            if (oobHdr == null || oobHdr != null && oobHdr.size() == 0) {
                return;
            }
            fail("Should have got *no* out-of-band headers, but some were found");
        }
        if (oobHdr == null) {
            fail("Should have got List of out-of-band headers");
        }

        assertEquals(2, oobHdr.size(), "HeaderHolder list expected to conain 2 object received " + oobHdr.size());

        for (Header hdr1 : oobHdr) {
            if (hdr1.getObject() instanceof Node) {
                try {
                    JAXBElement<?> job = (JAXBElement<?>) JAXBContext
                            .newInstance(org.apache.cxf.outofband.header.ObjectFactory.class)
                            .createUnmarshaller().unmarshal((Node) hdr1.getObject());
                    hdrToTest.add((OutofBandHeader) job.getValue());
                } catch (JAXBException ex) {
                    LOG.warn("JAXB error: {}", ex.getMessage(), ex);
                }
            }
        }

        assertTrue(hdrToTest.size() > 0, "out-of-band header should not be null");
        assertEquals("testOobReturnHeaderName", hdrToTest.get(0).getName(),
                "Expected out-of-band Header name testOobReturnHeaderName recevied :"
                                                                            + hdrToTest.get(0).getName());
        assertEquals("testOobReturnHeaderValue", hdrToTest.get(0).getValue(),
                "Expected out-of-band Header value testOobReturnHeaderValue recevied :"
                                                                              + hdrToTest.get(0).getValue());
        assertEquals("testReturnHdrAttribute", hdrToTest.get(0).getHdrAttribute(),
                "Expected out-of-band Header attribute testReturnHdrAttribute recevied :"
                                                                                   + hdrToTest.get(0).getHdrAttribute());

        assertEquals("New_testOobHeader", hdrToTest.get(1).getName(),
                "Expected out-of-band Header name New_testOobHeader recevied :"
                                                                      + hdrToTest.get(1).getName());
        assertEquals("New_testOobHeaderValue", hdrToTest.get(1).getValue(),
                "Expected out-of-band Header value New_testOobHeaderValue recevied :"
                                                                            + hdrToTest.get(1).getValue());
        assertEquals("testHdrAttribute", hdrToTest.get(1).getHdrAttribute(),
                "Expected out-of-band Header attribute testHdrAttribute recevied :"
                                                                             + hdrToTest.get(1).getHdrAttribute());
    }

    public static class InsertRequestOutHeaderProcessor implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            List<SoapHeader> soapHeaders = CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));

            // Insert a new header
            String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><outofbandHeader "
                         + "xmlns=\"http://cxf.apache.org/outofband/Header\" hdrAttribute=\"testHdrAttribute\" "
                         + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" soap:mustUnderstand=\"1\">"
                         + "<name>New_testOobHeader</name><value>New_testOobHeaderValue</value></outofbandHeader>";

            SoapHeader newHeader = new SoapHeader(
                    soapHeaders.get(0).getName(),
                    StaxUtils.read(new StringReader(xml)).getDocumentElement());
            // make sure direction is IN since it is a request message.
            newHeader.setDirection(Direction.DIRECTION_IN);
            //newHeader.setMustUnderstand(false);
            soapHeaders.add(newHeader);

        }

    }

    // START SNIPPET: InsertResponseOutHeaderProcessor

    public static class InsertResponseOutHeaderProcessor implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            // You should be able to get the header if exchange is routed from camel-cxf endpoint
            List<SoapHeader> soapHeaders = CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));
            if (soapHeaders == null) {
                // we just create a new soap headers in case the header is null
                soapHeaders = new ArrayList<>();
            }

            // Insert a new header
            String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><outofbandHeader "
                         + "xmlns=\"http://cxf.apache.org/outofband/Header\" hdrAttribute=\"testHdrAttribute\" "
                         + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" soap:mustUnderstand=\"1\">"
                         + "<name>New_testOobHeader</name><value>New_testOobHeaderValue</value></outofbandHeader>";
            SoapHeader newHeader = new SoapHeader(
                    soapHeaders.get(0).getName(),
                    StaxUtils.read(new StringReader(xml)).getDocumentElement());
            // make sure direction is OUT since it is a response message.
            newHeader.setDirection(Direction.DIRECTION_OUT);
            //newHeader.setMustUnderstand(false);
            soapHeaders.add(newHeader);

        }

    }

    // END SNIPPET: InsertResponseOutHeaderProcessor
    
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() throws InterruptedException {
            ServletWebServerFactory webServerFactory = new UndertowServletWebServerFactory(port);
            return webServerFactory;
        }

        
        @Bean
        CxfEndpoint routerRelayEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            return cxfEndpoint;
        }

        @Bean
        CxfEndpoint serviceRelayEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointBackend");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint routerRelayEndpointWithInsertion() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointWithInsertion");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortRelayWithInsertion"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            cxfEndpoint.getFeatures().add(new LoggingFeature());
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceRelayEndpointWithInsertion() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpointWithInsertionBackend");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortRelayWithInsertion"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            cxfEndpoint.getFeatures().add(new LoggingFeature());
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint routerNoRelayEndpoint(HeaderFilterStrategy dropAllMessageHeadersStrategy) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpoint");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortNoRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setHeaderFilterStrategy(dropAllMessageHeadersStrategy);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceNoRelayEndpoint(HeaderFilterStrategy dropAllMessageHeadersStrategy) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpointBackend");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortNoRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setHeaderFilterStrategy(dropAllMessageHeadersStrategy);
            return cxfEndpoint;
        }
        
        
        @Bean
        @Scope("prototype")
        CxfEndpoint routerNoRelayNoServiceClassEndpoint(HeaderFilterStrategy dropAllMessageHeadersStrategy) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayNoServiceClassEndpoint");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortNoRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            properties.put("allowStreaming", "false");
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setHeaderFilterStrategy(dropAllMessageHeadersStrategy);
            return cxfEndpoint;
        }

        @Bean
        CxfEndpoint serviceNoRelayNoServiceClassEndpoint(HeaderFilterStrategy dropAllMessageHeadersStrategy) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/CxfMessageHeadersRelayTest/HeaderService/routerNoRelayEndpointBackend");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortNoRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setHeaderFilterStrategy(dropAllMessageHeadersStrategy);
            return cxfEndpoint;
        }
        
        @Bean
        @Scope("prototype")
        CxfEndpoint serviceExtraRelays(HeaderFilterStrategy customMessageFilterStrategy) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("/CxfMessageHeadersRelayTest/HeaderService/serviceExtraRelays");
            cxfEndpoint.setWsdlURL("soap_header.wsdl");
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.soap.headers.HeaderTester.class);
            cxfEndpoint.setEndpointNameAsQName(
                QName.valueOf("{http://apache.org/camel/component/cxf/soap/headers}SoapPortCustomRelay"));
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            cxfEndpoint.setHeaderFilterStrategy(customMessageFilterStrategy);
            return cxfEndpoint;
        }
        
        @Bean
        HeaderFilterStrategy dropAllMessageHeadersStrategy() {
            CxfHeaderFilterStrategy headerFilterStrategy = new CxfHeaderFilterStrategy();
            headerFilterStrategy.setRelayHeaders(false);
            return headerFilterStrategy;
        }
        
        @Bean
        HeaderFilterStrategy customMessageFilterStrategy() {
            CxfHeaderFilterStrategy headerFilterStrategy = new CxfHeaderFilterStrategy();
            List<MessageHeaderFilter> headerFilterList = new ArrayList<MessageHeaderFilter>();
            headerFilterList.add(new SoapMessageHeaderFilter());
            headerFilterList.add(new CustomHeaderFilter());
            headerFilterStrategy.setMessageHeaderFilters(headerFilterList);
            return headerFilterStrategy;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    
                    from("cxf:bean:routerRelayEndpoint")
                        .to("cxf:bean:serviceRelayEndpoint");
                    from("cxf:bean:routerRelayEndpointWithInsertion")
                        .process(new InsertRequestOutHeaderProcessor())
                        .to("cxf:bean:serviceRelayEndpointWithInsertion")
                        .process(new InsertResponseOutHeaderProcessor());
                    from("direct:directProducer")
                        .to("cxf:bean:serviceRelayEndpoint");
                    from("direct:relayProducer")
                        .to("cxf://"
                            + "http://localhost:" + port 
                            + "/services/CxfMessageHeadersRelayTest/HeaderService/routerRelayEndpoint" +
                            "?serviceClass=\"org.apache.camel.component.cxf.soap.headers.HeaderTester\"");
                    from("cxf:bean:routerNoRelayEndpoint")
                        .to("cxf:bean:serviceNoRelayEndpoint");
                    from("cxf:bean:routerNoRelayNoServiceClassEndpoint")
                        .to("cxf:bean:serviceNoRelayNoServiceClassEndpoint");
                    from("cxf:bean:serviceExtraRelays")
                        .to("mock:result");
                }
            };
        }
    }

}
