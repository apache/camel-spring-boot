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




import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.HelloService;
import org.apache.camel.component.cxf.HelloServiceImpl;
import org.apache.camel.component.cxf.common.CXFTestSupport;
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
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        LoggingInterceptorInMessageModeTest.class,
        LoggingInterceptorInMessageModeTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class LoggingInterceptorInMessageModeTest {
    
    
    protected static final String ROUTER_ADDRESS = "/LoggingInterceptorInMessageModeTest/router";
    protected static final String SERVICE_ADDRESS
            = "/LoggingInterceptorInMessageModeTest/helloworld";

    static Server server;

    
    static int port = CXFTestSupport.getPort1();

        
    @BeforeEach
    public void startService() {
      //start a service
        ServerFactoryBean svrBean = new ServerFactoryBean();

        svrBean.setAddress(SERVICE_ADDRESS);
        svrBean.setServiceClass(HelloService.class);
        svrBean.setServiceBean(new HelloServiceImpl());

        server = svrBean.create();
    }
    
    @AfterEach
    public void stopService() {
        server.stop();
        server.destroy();
    }
    
    
    @Autowired
    protected CamelContext context;
    
    @Test
    public void testInvokingServiceFromCXFClient() throws Exception {

        LoggingOutInterceptor logInterceptor = null;

        for (Interceptor<?> interceptor : context.getEndpoint("cxf:bean:serviceEndpoint", CxfSpringEndpoint.class)
                .getOutInterceptors()) {
            if (interceptor instanceof LoggingOutInterceptor) {
                logInterceptor = LoggingOutInterceptor.class.cast(interceptor);
                break;
            }
        }

        assertNotNull(logInterceptor);
        // StringPrintWriter writer = new StringPrintWriter();
        // Unfortunately, LoggingOutInterceptor does not have a setter for writer so
        // we can't capture the output to verify.
        // logInterceptor.setPrintWriter(writer);

        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost:" + port
                              + "/services" + ROUTER_ADDRESS);
        clientBean.setServiceClass(HelloService.class);

        HelloService client = (HelloService) proxyFactory.create();

        String result = client.echo("hello world");
        assertEquals("echo hello world", result, "we should get the right answer from router");

    }

    @SuppressWarnings("unused")
    private static final class StringPrintWriter extends PrintWriter {
        private StringPrintWriter() {
            super(new StringWriter());
        }

        private StringPrintWriter(int initialSize) {
            super(new StringWriter(initialSize));
        }

        private String getString() {
            flush();
            return ((StringWriter) out).toString();
        }
    }
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint").to("cxf:bean:serviceEndpoint");
                }
            };
        }
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
        
        @Bean
        public CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress(ROUTER_ADDRESS);
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        public CxfEndpoint serviceEndpoint(LoggingOutInterceptor loggingOutInterceptor) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services" + SERVICE_ADDRESS);
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.getOutInterceptors().add(loggingOutInterceptor);
            return cxfEndpoint;
        }
        
        
        @Bean
        public LoggingOutInterceptor loggingOutInterceptor() {
            LoggingOutInterceptor logger = new LoggingOutInterceptor("write");
            return logger;
        }
    }
    
    
}
