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





import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.HelloService;
import org.apache.camel.component.cxf.HelloServiceImpl;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
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


import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfSpringRouterTest.class,
        CxfSpringRouterTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfSpringRouterTest {
    
    protected Server server;
    
    static int port = CXFTestSupport.getPort1();

    @BeforeEach
    public void startService() {
        //start a service
        ServerFactoryBean svrBean = new ServerFactoryBean();

        svrBean.setAddress("/CxfSpringRouterTest/helloworld");
        svrBean.setServiceClass(HelloService.class);
        svrBean.setServiceBean(new HelloServiceImpl());
        server = svrBean.create();
        server.start();
    }

    @AfterEach
    public void shutdownService() {
        if (server != null) {
            server.stop();
        }
    }

    
    
    
    @Bean
    private CxfEndpoint routerEndpoint() {
        CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
        cxfEndpoint.setServiceClass(HelloService.class);
        cxfEndpoint.setAddress("/CxfSpringRouterTest/router");
        return cxfEndpoint;
    }
    
    @Bean
    private CxfEndpoint serviceEndpoint() {
        CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
        cxfEndpoint.setServiceClass(HelloService.class);
        cxfEndpoint.setAddress("http://localhost:" + port 
                               + "/services/CxfSpringRouterTest/helloworld");
        return cxfEndpoint;
    }
    
    protected HelloService getCXFClient() throws Exception {
        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost:" + port 
                              + "/services/CxfSpringRouterTest/router");
        clientBean.setServiceClass(HelloService.class);

        HelloService client = (HelloService) proxyFactory.create();
        return client;
    }

    @Test
    public void testInvokingServiceFromCXFClient() throws Exception {
        HelloService client = getCXFClient();
        String result = client.echo("hello world");
        assertEquals("echo hello world", result, "we should get the right answer from router");

    }

    @Test
    public void testOnwayInvocation() throws Exception {
        HelloService client = getCXFClient();
        int count = client.getInvocationCount();
        client.ping();
        //oneway ping invoked, so invocationCount ++
        assertEquals(client.getInvocationCount(), ++count, "The ping should be invocated");
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
                    from("cxf:bean:routerEndpoint").to("cxf:bean:serviceEndpoint");
                }
            };
        }
    }
    
    
}
