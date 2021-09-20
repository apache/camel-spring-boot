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
package org.apache.camel.component.servlet.springboot.test;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;

import javax.servlet.http.HttpServletRequest;

/**
 * Testing multipart processing with camel servlet
 */
@CamelSpringBootTest
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = ServletMultiPartTest.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServletMultiPartTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CamelContext context;

    @BeforeEach
    public void setup() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("servlet:/test?disableStreamCache=true")
                        .process(exchange -> {
                            HttpServletRequest httpServletRequest = exchange.getIn().getBody(HttpServletRequest.class);
                            exchange.getIn().setBody(httpServletRequest.getPart("file").getName());
                        });
            }
        });
    }

    @Test
    public void testMultipartRequest() throws Exception {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> message = new LinkedMultiValueMap<>();
        message.add("file", "Multipart Test");
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(message, httpHeaders);
        Assertions.assertEquals("file", restTemplate.postForEntity("/camel/test", httpEntity, String.class).getBody());
    }

}

