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
package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpRestDSLTest.class, PlatformHttpAsyncRequestHandlingTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
@AutoConfigureMockMvc
public class PlatformHttpAsyncRequestHandlingTest extends PlatformHttpBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    CamelContext camelContext;

    private static final String postRouteId = "PlatformHttpAsyncRequestHandlingTest_mypost";

    private static final String getRouteId = "PlatformHttpAsyncRequestHandlingTest_myget";

    @Test
    public void testGetAsync() throws Exception {
        waitUntilRouteIsStarted(1, getGetRouteId());

        MvcResult mvcResult = mockMvc.perform(get("/myget"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("get"));
    }

    @Test
    public void testPostAsync() throws Exception {
        waitUntilRouteIsStarted(1, getPostRouteId());

        MvcResult mvcResult = mockMvc.perform(post("/mypost").content("hello"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("HELLO"));
    }

    // *************************************
    // Config
    // *************************************
    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder servletPlatformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/myget").id(getRouteId)
                            .process(exchange -> {
                                Thread.sleep(2000);
                            })
                            .setBody().constant("get");
                    from("platform-http:/mypost").id(postRouteId).transform().body(String.class, b -> b.toUpperCase());
                }
            };
        }
    }

    @Override
    protected String getPostRouteId() {
        return postRouteId;
    }

    @Override
    protected String getGetRouteId() {
        return getRouteId;
    }
}
