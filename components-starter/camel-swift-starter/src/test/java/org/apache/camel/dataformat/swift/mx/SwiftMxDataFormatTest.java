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
package org.apache.camel.dataformat.swift.mx;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prowidesoftware.swift.model.mx.MxCamt04800103;
import com.prowidesoftware.swift.model.mx.MxPacs00800107;
import com.prowidesoftware.swift.model.mx.sys.MxXsys01100102;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SwiftMxDataFormatTest.class,
        SwiftMxDataFormatTest.Config.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SwiftMxDataFormatTest.xml"
    }
)
class SwiftMxDataFormatTest {

    @EndpointInject("mock:unmarshal")
    MockEndpoint mockEndpointUnmarshal;
    @Autowired
    @Produce("direct:unmarshal")
    ProducerTemplate templateUnmarshal;
    @EndpointInject("mock:unmarshalFull")
    MockEndpoint mockEndpointUnmarshalFull;
    @Autowired
    @Produce("direct:unmarshalFull")
    ProducerTemplate templateUnmarshalFull;
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
    @EndpointInject("mock:marshalFull")
    MockEndpoint mockEndpointMarshalFull;
    @Autowired
    @Produce("direct:marshalFull")
    ProducerTemplate templateMarshalFull;

    @Test
    void testUnmarshal() throws Exception {
        mockEndpointUnmarshal.expectedMessageCount(1);

        Object result = templateUnmarshal.requestBody(Files.readAllBytes(Paths.get("src/test/resources/mx/message1.xml")));
        assertNotNull(result);
        assertInstanceOf(MxCamt04800103.class, result);
        mockEndpointUnmarshal.assertIsSatisfied();
    }

    @Test
    void testUnmarshalFull() throws Exception {
        mockEndpointUnmarshalFull.expectedMessageCount(1);

        Object result = templateUnmarshalFull.requestBody(Files.readAllBytes(Paths.get("src/test/resources/mx/message3.xml")));
        assertNotNull(result);
        assertInstanceOf(MxXsys01100102.class, result);
        mockEndpointUnmarshalFull.assertIsSatisfied();
    }

    @Test
    void testMarshal() throws Exception {
        mockEndpointMarshal.expectedMessageCount(1);

        MxPacs00800107 message = MxPacs00800107.parse(Files.readString(Paths.get("src/test/resources/mx/message2.xml")));
        Object result = templateMarshal.requestBody(message);
        assertNotNull(result);
        assertInstanceOf(InputStream.class, result);
        MxPacs00800107 actual = MxPacs00800107.parse(IOUtils.toString((InputStream) result, StandardCharsets.UTF_8));
        assertEquals(message.message(), actual.message());
        mockEndpointMarshal.assertIsSatisfied();
    }

    @Test
    void testMarshalJson() throws Exception {
        mockEndpointMarshalJson.expectedMessageCount(1);

        MxPacs00800107 message = MxPacs00800107.parse(Files.readString(Paths.get("src/test/resources/mx/message2.xml")));
        Object result = templateMarshalJson.requestBody(message);
        assertNotNull(result);
        assertInstanceOf(InputStream.class, result);

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(Files.readString(Paths.get("src/test/resources/mx/message2.json"))),
            mapper.readTree((InputStream) result));
        mockEndpointMarshalJson.assertIsSatisfied();
    }

    @Test
    void testMarshalFull() throws Exception {
        mockEndpointMarshalFull.expectedMessageCount(1);

        MxPacs00800107 message = MxPacs00800107.parse(Files.readString(Paths.get("src/test/resources/mx/message2.xml")));
        Object result = templateMarshalFull.requestBody(message);
        assertNotNull(result);
        assertInstanceOf(InputStream.class, result);

        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) result, StandardCharsets.UTF_8));
        String line = reader.readLine();
        assertFalse(line.contains("<?xml"), String.format("Should not start with the xml header, the first line was %s", line));
        mockEndpointMarshalFull.assertIsSatisfied();
    }

    @Configuration
    public static class Config {

        @Bean
        public ReadConfiguration readConfig(){
            return new ReadConfiguration();
        }

        @Bean
        public WriteConfiguration writeConfig(){
            WriteConfiguration configuration = new WriteConfiguration();
            configuration.setIncludeXMLDeclaration(false);
            return configuration;
        }
    }
}
