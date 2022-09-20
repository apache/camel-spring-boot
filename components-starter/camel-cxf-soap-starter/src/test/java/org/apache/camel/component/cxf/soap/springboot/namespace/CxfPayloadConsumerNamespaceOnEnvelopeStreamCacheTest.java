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
package org.apache.camel.component.cxf.soap.springboot.namespace;

import org.w3c.dom.Document;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class,
                           CxfPayloadConsumerNamespaceOnEnvelopeStreamCacheTest.class,
                           CxfPayloadConsumerNamespaceOnEnvelopeStreamCacheTest.TestConfiguration.class,
                           CxfAutoConfiguration.class,
                           CxfPayloadConsumerNamespaceOnEnvelopeTest.EndpointConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfPayloadConsumerNamespaceOnEnvelopeStreamCacheTest
    extends CxfPayloadConsumerNamespaceOnEnvelopeTest {
    
        
   

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
                    from("direct:router")
                        // Use message mode to send the raw message
                        .to("cxf:bean:serviceEndpoint")
                        // Convert to String to make testing the result easier
                        .convertBodyTo(String.class);
                    // The consumer is running in payload mode
                    from("cxf:bean:routerEndpoint")
                        // Use stream caching to trigger the issue
                        .streamCaching()
                        // Parse to DOM to make sure it's still valid XML
                        .convertBodyTo(Document.class).setBody().constant(RESPONSE_PAYLOAD);
                }
            };
        }
    }

}
