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
package org.apache.camel.component.hl7.springboot.test;


import static org.apache.camel.component.hl7.HL7.hl7terser;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.Hl7Terser;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.PID;

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
        TerserExpressionTest.class,
        TerserExpressionTest.TestConfiguration.class
    }
)
public class TerserExpressionTest extends HL7TestSupport {

    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:test1")
    MockEndpoint mock1;
    
    @EndpointInject("mock:test2")
    MockEndpoint mock2;
    
    @EndpointInject("mock:test3")
    MockEndpoint mock3;
    
    @EndpointInject("mock:test4")
    MockEndpoint mock4;
    
    @EndpointInject("mock:test5")
    MockEndpoint mock5;

    
    private static final String PATIENT_ID = "123456";

    @Test
    public void testTerserExpression() throws Exception {
        mock1.expectedMessageCount(1);
        mock1.expectedBodiesReceived(PATIENT_ID);
        template.sendBody("direct:test1", createADT01Message());
        mock1.assertIsSatisfied();
    }

    @Test
    public void testTerserPredicateValue() throws Exception {
        
        mock2.expectedMessageCount(1);
        template.sendBody("direct:test2", createADT01Message());
        mock2.assertIsSatisfied();
    }

    @Test
    public void testTerserPredicateNull() throws Exception {
        
        mock3.expectedMessageCount(1);
        template.sendBody("direct:test3", createADT01Message());
        mock3.assertIsSatisfied();
    }

    @Test
    public void testTerserInvalidExpression() throws Exception {
        final Message adt01Message = createADT01Message();

        assertThrows(CamelExecutionException.class,
                () -> {
                    template.sendBody("direct:test4", adt01Message);
                });
    }

    @Test
    public void testTerserInvalidMessage() throws Exception {
        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:test4", "text instead of message"));
    }

    @Test
    public void testTerserAnnotatedMethod() throws Exception {
        
        mock5.expectedMessageCount(1);
        mock5.expectedBodiesReceived(PATIENT_ID);
        template.sendBody("direct:test5", createADT01Message());
        mock5.assertIsSatisfied();
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {
        public class TerserBean {
            public String patientId(@Hl7Terser(value = "PID-3-1") String patientId) {
                return patientId;
            }
        }

        @Bean
        public RouteBuilder routeBuilder() {
            final TerserBean terserBean = new TerserBean();

            return new RouteBuilder() {
                public void configure() throws Exception {
                    from("direct:test1").transform(hl7terser("PID-3-1")).to("mock:test1");
                    from("direct:test2").filter(hl7terser("PID-3-1").isEqualTo(PATIENT_ID)).to("mock:test2");
                    from("direct:test3").filter(hl7terser("PID-4-1").isNull()).to("mock:test3");
                    from("direct:test4").filter(hl7terser("blorg gablorg").isNull()).to("mock:test3");
                    from("direct:test5").bean(terserBean).to("mock:test5");
                }
            };
        }
    }
    
    private static Message createADT01Message() throws Exception {
        ADT_A01 adt = new ADT_A01();
        adt.initQuickstart("ADT", "A01", "P");

        // Populate the PID Segment
        PID pid = adt.getPID();
        pid.getPatientName(0).getFamilyName().getSurname().setValue("Doe");
        pid.getPatientName(0).getGivenName().setValue("John");
        pid.getPatientIdentifierList(0).getID().setValue(PATIENT_ID);

        return adt;
    }

    
}
