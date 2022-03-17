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
package org.apache.camel.jaxb;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.FallbackTypeConverter;
import org.apache.camel.converter.jaxb.message.Message;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.impl.converter.CoreTypeConverterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        FallbackTypeConverterObjectFactoryEnabledTest.class,
        FallbackTypeConverterObjectFactoryEnabledTest.TestConfiguration.class
    }
)
public class FallbackTypeConverterObjectFactoryEnabledTest {
    
    @Autowired
    ProducerTemplate template;
    
   
    @EndpointInject("mock:a")
    private MockEndpoint resultEndpoint;

    @Test
    public void testObjectFactoryTrue() throws Exception {
        
        Message in = new Message("Hello World");
        resultEndpoint.expectedBodiesReceived(in);

        template.sendBody("direct:a", in);

        resultEndpoint.assertIsSatisfied();
    }

        
    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:a").convertBodyTo(String.class).to("direct:b");
                    from("direct:b").convertBodyTo(Message.class).to("mock:a");
                }
            };
        }
    }
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                //hack way to remove the SpringTypeConverter as its the first fallback converter
                //but we expect to use the org.apache.camel.converter.jaxb.FallbackTypeConverter
                //from SimpleTypeConverter in this test case.
                List<CoreTypeConverterRegistry.FallbackTypeConverter> list = ((CoreTypeConverterRegistry)context.getTypeConverterRegistry()).
                    getFallbackConverters();
                for (CoreTypeConverterRegistry.FallbackTypeConverter fallback : list) {
                    if (fallback.getFallbackTypeConverter().getClass().getName()
                        .contains("SpringTypeConverter")) {
                        list.remove(fallback);
                    }
                }
                context.getGlobalOptions().put(FallbackTypeConverter.OBJECT_FACTORY, "true");
            }
            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }

        };
    }


}
