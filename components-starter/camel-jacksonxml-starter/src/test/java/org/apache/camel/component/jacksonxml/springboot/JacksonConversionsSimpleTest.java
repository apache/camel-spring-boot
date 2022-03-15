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
package org.apache.camel.component.jacksonxml.springboot;


import java.util.HashMap;
import java.util.Map;


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.jacksonxml.JacksonXMLConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JacksonConversionsSimpleTest.class,
    }
)

public class JacksonConversionsSimpleTest {

   
    @Autowired
    private CamelContext context;
    
 
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, "true");
            }
            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }

        };
    }
    
    @Test
    public void shouldNotConvertMapToString() {
        Exchange exchange = new DefaultExchange(context);

        Map<String, String> body = new HashMap<>();
        Object convertedObject = context.getTypeConverter().convertTo(String.class, exchange, body);
        // will do a toString which is an empty map
        assertEquals(body.toString(), convertedObject);
    }

    @Test
    public void shouldNotConvertMapToNumber() {
        Exchange exchange = new DefaultExchange(context);

        Object convertedObject = context.getTypeConverter().convertTo(Long.class, exchange, new HashMap<String, String>());
        assertNull(convertedObject);
    }

    @Test
    public void shouldNotConvertMapToPrimitive() {
        Exchange exchange = new DefaultExchange(context);
        Object convertedObject = context.getTypeConverter().convertTo(long.class, exchange, new HashMap<String, String>());

        assertNull(convertedObject);
    }
       
    
}
