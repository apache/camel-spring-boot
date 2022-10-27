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
package org.apache.camel.dataformat.swift.mt;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.model.mt.mt5xx.MT515;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SwiftMtDataFormatTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SwiftMtDataFormatTest.xml"
    }
)
class SwiftMtDataFormatTest {

    @EndpointInject("mock:unmarshal")
    MockEndpoint mockEndpointUnmarshal;
    @Autowired
    @Produce("direct:unmarshal")
    ProducerTemplate templateUnmarshal;
    @EndpointInject("mock:marshal")
    MockEndpoint mockEndpointMarshal;
    @Autowired
    @Produce("direct:marshal")
    ProducerTemplate templateMarshal;
    @EndpointInject("mock:marshalJson")
    MockEndpoint mockEndpointMarshalJson;
    @Autowired
    @Produce("direct:marshalJson")
    ProducerTemplate templateMarshalJson;

    @Test
    void testUnmarshal() throws Exception {
        mockEndpointUnmarshal.expectedMessageCount(1);

        Object result = templateUnmarshal.requestBody(Files.readAllBytes(Paths.get("src/test/resources/mt/message1.txt")));
        assertNotNull(result);
        assertInstanceOf(MT515.class, result);
        mockEndpointUnmarshal.assertIsSatisfied();
    }

    @Test
    void testMarshal() throws Exception {
        mockEndpointMarshal.expectedMessageCount(1);

        MT103 message = MT103.parse(Files.readString(Paths.get("src/test/resources/mt/message2.txt")));

        Object result = templateMarshal.requestBody(message);
        assertNotNull(result);
        assertInstanceOf(InputStream.class, result);
        MT103 actual = MT103.parse((InputStream) result);
        assertEquals(message.message(), actual.message());
        mockEndpointMarshal.assertIsSatisfied();
    }

    @Test
    void testMarshalJson() throws Exception {
        mockEndpointMarshalJson.expectedMessageCount(1);

        MT103 message = MT103.parse(Files.readString(Paths.get("src/test/resources/mt/message2.txt")));

        Object result = templateMarshalJson.requestBody(message);
        assertNotNull(result);
        assertInstanceOf(InputStream.class, result);

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(Files.readString(Paths.get("src/test/resources/mt/message2.json"))),
            mapper.readTree((InputStream) result));
        mockEndpointMarshalJson.assertIsSatisfied();
    }
}
