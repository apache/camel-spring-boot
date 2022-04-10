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
package org.apache.camel.dataformat.bindy.springboot.fixed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;



import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        BindyPaddingAndTrimmingTest.class,
        BindyPaddingAndTrimmingTest.TestConfiguration.class
    }
)
public class BindyPaddingAndTrimmingTest {

    @Autowired
    ProducerTemplate template;
    
    private static final String URI_DIRECT_UNMARSHAL = "direct:unmarshall";
    private static final String URI_MOCK_UNMARSHAL_RESULT = "mock:unmarshal_result";

    @EndpointInject(URI_MOCK_UNMARSHAL_RESULT)
    private MockEndpoint unmarhsalResult;

    
    @Test
    public void testUnmarshal() throws Exception {
        unmarhsalResult.reset();
        unmarhsalResult.expectedMessageCount(1);
        template.sendBody(URI_DIRECT_UNMARSHAL, "foo  \r\n");

        unmarhsalResult.assertIsSatisfied();
        MyBindyModel myBindyModel = unmarhsalResult.getReceivedExchanges().get(0).getIn().getBody(MyBindyModel.class);
        assertEquals("foo  ", myBindyModel.foo);
        assertThat(myBindyModel.bar, is(""));
    }

    @Test
    public void testUnmarshalTooLong() throws Exception {
        unmarhsalResult.reset();
        unmarhsalResult.expectedMessageCount(1);
        template.sendBody(URI_DIRECT_UNMARSHAL, "foo  bar    \r\n");

        unmarhsalResult.assertIsSatisfied();
        MyBindyModel myBindyModel = unmarhsalResult.getReceivedExchanges().get(0).getIn().getBody(MyBindyModel.class);
        assertEquals("foo  ", myBindyModel.foo);

    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(URI_DIRECT_UNMARSHAL)
                            .unmarshal().bindy(BindyType.Fixed, MyBindyModel.class)
                            .to(URI_MOCK_UNMARSHAL_RESULT);
                }
            };
        }
    }
    
    // *************************************************************************
    // DATA MODEL
    // *************************************************************************
    @FixedLengthRecord(length = 10, ignoreMissingChars = true, ignoreTrailingChars = true)
    public static class MyBindyModel {
        @DataField(pos = 0, length = 5)
        String foo;

        @DataField(pos = 5, length = 5)
        String bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }
}
