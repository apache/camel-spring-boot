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
package org.apache.camel.component.cxf.soap.springboot.undertowhandler;




import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.non_wrapper.Person;
import org.apache.camel.non_wrapper.types.GetPerson;
import org.apache.camel.non_wrapper.types.GetPersonResponse;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.server.handlers.RequestLimitingHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.util.HttpString;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        RequestLimitingHandlerTest.class,
        RequestLimitingHandlerTest.TestConfiguration.class,
        CxfAutoConfiguration.class
        
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class RequestLimitingHandlerTest {
    
    
    static int port = CXFTestSupport.getPort1();
    
    @Test
    public void testClient() throws Exception {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + port + "/services/"
                           + getClass().getSimpleName() + "/CamelContext/RouterPort");
        factory.setServiceClass(Person.class);
        Person person = factory.create(Person.class);
        CountDownLatch latch = new CountDownLatch(50); 

        ExecutorService executor = Executors.newFixedThreadPool(200); 

        for (int i = 0; i < 50; i++) {
            executor.execute(new SendRequest(person, latch)); 
        }
        latch.await();
    }
    
    class SendRequest implements Runnable {
        Person person;
        CountDownLatch latch;
        SendRequest(Person person, CountDownLatch latch) {
            this.person = person;
            this.latch = latch;
        }
        @Override
        public void run() {
            try {
                GetPerson payload = new GetPerson();
                payload.setPersonId("1234");

                GetPersonResponse reply = person.getPerson(payload);
                assertEquals("1234", reply.getPersonId(), "Get the wrong person id.");

            } catch (Exception ex) {
                //some requests are expected to fail and receive 503 error
                //cause Server side limit the concurrent request
                assertTrue(ex.getCause().getMessage().contains("503: Service Unavailable"));
            } finally {
                latch.countDown();
            }

        }

    }

            

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(port);

            // Customize DeploymentInfo
            factory.addDeploymentInfoCustomizers(new UndertowDeploymentInfoCustomizer() {
                @Override
                public void customize(DeploymentInfo deploymentInfo) {
                    // Enable RequestLimitingHandler
                    deploymentInfo.addOuterHandlerChainWrapper(new HandlerWrapper() {
                        @Override
                        public HttpHandler wrap(HttpHandler handler) {
                            
                            return new DisallowedMethodsHandler(new RequestLimitingHandler(3, 1, handler), 
                                                                new HttpString("GET"));
                        }
                    });
                }
            });

            return factory;
        }
        
        @Bean
        public CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("/RequestLimitingHandlerTest/CamelContext/RouterPort");
            cxfEndpoint.setServiceClass(org.apache.camel.non_wrapper.Person.class);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        
        
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint").streamCaching().process(new Processor() {

                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // just try to turn the payload to the parameter we want
                            // to use
                            Thread.sleep(1000);
                            GetPerson request = exchange.getIn().getBody(GetPerson.class);
                            GetPersonResponse reply = new GetPersonResponse();
                            reply.setPersonId(request.getPersonId());
                            exchange.getMessage().setBody(reply);
                        }

                    });
                }
            };
        }
    }
    
    
}
