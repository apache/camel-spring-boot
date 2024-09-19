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
package org.apache.camel.component.jackson.avro.springboot.test;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;

import org.apache.avro.NameValidator;
import org.apache.avro.Schema;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.AvroLibrary;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, JacksonAvroMarshalUnmarshalJsonNodeTest.class,
        JacksonAvroMarshalUnmarshalJsonNodeTest.TestConfiguration.class })
public class JacksonAvroMarshalUnmarshalJsonNodeTest {

    @Autowired
    ProducerTemplate template;

    @Autowired
    CamelContext context;

    @EndpointInject("mock:serialized")
    MockEndpoint mock1;

    @EndpointInject("mock:pojo")
    MockEndpoint mock2;

    @Bean("schema-resolver")
    private SchemaResolver getSchemaResolver() {
        String schemaJson = "{\n" + "\"type\": \"record\",\n" + "\"name\": \"Pojo\",\n" + "\"fields\": [\n"
                + " {\"name\": \"text\", \"type\": \"string\"}\n" + "]}";
        String listSchemaJson = "{\n" + "  \"type\": \"array\",  \n" + "  \"items\":{\n" + "    \"name\":\"Pojo\",\n"
                + "    \"type\":\"record\",\n" + "    \"fields\":[\n"
                + "      {\"name\":\"text\", \"type\":\"string\"}\n" + "    ]\n" + "  }\n" + "}";

        Schema raw = new Schema.Parser(NameValidator.STRICT_VALIDATOR).parse(schemaJson);
        AvroSchema schema = new AvroSchema(raw);

        Schema rawList = new Schema.Parser(NameValidator.STRICT_VALIDATOR).parse(listSchemaJson);
        AvroSchema schemaList = new AvroSchema(rawList);

        SchemaResolver resolver = ex -> {
            Boolean isList = ex.getMessage().getHeader("list", Boolean.class);
            if (isList != null && isList) {
                return schemaList;
            }
            return schema;
        };
        return resolver;
    }

    @Test
    public void testMarshalUnmarshalJsonNode() throws Exception {
        MockEndpoint.resetMocks(context);
        mock1.expectedMessageCount(1);
        mock1.message(0).body().isInstanceOf(byte[].class);

        Pojo pojo = new Pojo("Hello");
        template.sendBody("direct:pojo", pojo);

        byte[] serialized = mock1.getReceivedExchanges().get(0).getIn().getBody(byte[].class);
        assertNotNull(serialized);
        assertEquals(6, serialized.length);

        mock2.expectedMessageCount(1);
        mock2.message(0).body().isInstanceOf(JsonNode.class);

        template.sendBody("direct:serialized", serialized);
        mock2.assertIsSatisfied();

        JsonNode back = mock2.getReceivedExchanges().get(0).getIn().getBody(JsonNode.class);

        assertEquals(pojo.getText(), back.at("/text").asText());
    }

    @Test
    public void testMarshalUnmarshalJsonNodeList() throws Exception {
        MockEndpoint.resetMocks(context);
        mock1.expectedMessageCount(1);
        mock1.message(0).body().isInstanceOf(byte[].class);

        List<Pojo> pojos = new ArrayList<>();
        pojos.add(new Pojo("Hello"));
        pojos.add(new Pojo("World"));

        template.sendBodyAndHeader("direct:pojo", pojos, "list", true);

        byte[] serialized = mock1.getReceivedExchanges().get(0).getIn().getBody(byte[].class);
        assertNotNull(serialized);
        assertEquals(14, serialized.length);

        mock2.expectedMessageCount(1);
        mock2.message(0).body().isInstanceOf(JsonNode.class);

        template.sendBodyAndHeader("direct:serialized", serialized, "list", true);
        mock2.assertIsSatisfied();

        JsonNode back = mock2.getReceivedExchanges().get(0).getIn().getBody(JsonNode.class);
        assertTrue(back.isArray());
        assertEquals(2, back.size());
        assertEquals("Hello", back.get(0).at("/text").asText());
        assertEquals("World", back.get(1).at("/text").asText());
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
                public void configure() throws Exception {
                    from("direct:serialized").unmarshal().avro(AvroLibrary.Jackson, JsonNode.class).to("mock:pojo");
                    from("direct:pojo").marshal().avro(AvroLibrary.Jackson).to("mock:serialized");
                }
            };
        }
    }

    public static class Pojo {

        private String text;

        public Pojo() {
        }

        public Pojo(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
