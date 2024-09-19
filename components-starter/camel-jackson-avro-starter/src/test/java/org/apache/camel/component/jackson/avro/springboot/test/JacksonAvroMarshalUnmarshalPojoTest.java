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

import com.fasterxml.jackson.dataformat.avro.AvroSchema;

import org.apache.avro.NameValidator;
import org.apache.avro.Schema;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, JacksonAvroMarshalUnmarshalPojoTest.class,
        JacksonAvroMarshalUnmarshalPojoTest.TestConfiguration.class })
public class JacksonAvroMarshalUnmarshalPojoTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:serialized")
    MockEndpoint mock1;

    @EndpointInject("mock:pojo")
    MockEndpoint mock2;

    @Bean("schema-resolver")
    private SchemaResolver getSchemaResolver() {
        String schemaJson = "{\n" + "\"type\": \"record\",\n" + "\"name\": \"Pojo\",\n" + "\"fields\": [\n"
                + " {\"name\": \"text\", \"type\": \"string\"}\n" + "]}";
        Schema raw = new Schema.Parser(NameValidator.STRICT_VALIDATOR).parse(schemaJson);
        AvroSchema schema = new AvroSchema(raw);
        SchemaResolver resolver = ex -> schema;

        return resolver;
    }

    @Test
    public void testMarshalUnmarshalPojo() throws Exception {

        mock1.expectedMessageCount(1);
        mock1.message(0).body().isInstanceOf(byte[].class);

        Pojo pojo = new Pojo("Hello");
        template.sendBody("direct:pojo", pojo);

        byte[] serialized = mock1.getReceivedExchanges().get(0).getIn().getBody(byte[].class);
        assertNotNull(serialized);
        assertEquals(6, serialized.length);

        mock2.expectedMessageCount(1);
        mock2.message(0).body().isInstanceOf(Pojo.class);

        template.sendBody("direct:serialized", serialized);
        mock2.assertIsSatisfied();

        Pojo back = mock2.getReceivedExchanges().get(0).getIn().getBody(Pojo.class);

        assertEquals(pojo.getText(), back.getText());
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
                    from("direct:serialized").unmarshal().avro(AvroLibrary.Jackson, Pojo.class).to("mock:pojo");
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
