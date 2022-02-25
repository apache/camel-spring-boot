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


import static org.apache.camel.component.hl7.HL7.messageConforms;
import static org.apache.camel.component.hl7.HL7.messageConformsTo;


import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.builder.ValidationRuleBuilder;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

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
        MessageValidatorTest.class,
        MessageValidatorTest.TestConfiguration.class
    }
)
public class MessageValidatorTest extends HL7TestSupport {

    
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
    
    @EndpointInject("mock:test6")
    MockEndpoint mock6;
    
    static ValidationContext defaultValidationContext;
    static ValidationContext customValidationContext;
    static HapiContext defaultContext;
    static HapiContext customContext;

    @BeforeAll
    private static void doPreSetup() throws Exception {
        defaultValidationContext = ValidationContextFactory.defaultValidation();
        defaultContext = new DefaultHapiContext(defaultValidationContext);
        // we validate separately, not during parsing or rendering
        defaultContext.getParserConfiguration().setValidating(false);

        ValidationRuleBuilder builder = new ValidationRuleBuilder() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void configure() {
                forVersion(Version.V24)
                        .message("ADT", "A01")
                        .terser("PID-8", not(empty()));
            }
        };
        customValidationContext = ValidationContextFactory.fromBuilder(builder);
        customContext = new DefaultHapiContext(customValidationContext);
        // we validate separately, not during parsing or rendering
        customContext.getParserConfiguration().setValidating(false);
    }

    @Test
    public void testDefaultHapiContext() throws Exception {
        mock4.expectedMessageCount(1);
        Message msg = createADT01Message();
        template.sendBody("direct:test4", msg);
        mock4.assertIsSatisfied();
    }

    @Test
    public void testCustomHapiContext() throws Exception {
        mock5.expectedMessageCount(0);
        Message msg = createADT01Message();
        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:test5", msg));
        mock5.assertIsSatisfied();
    }

    @Test
    public void testDefaultValidationContext() throws Exception {
        mock1.expectedMessageCount(1);
        Message msg = createADT01Message();
        template.sendBody("direct:test1", msg);
        mock1.assertIsSatisfied();
    }

    @Test
    public void testCustomValidationContext() throws Exception {
        mock2.expectedMessageCount(0);
        Message msg = createADT01Message();
        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:test2", msg));
        mock2.assertIsSatisfied();
    }

    @Test
    public void testDynamicCustomValidationContext() throws Exception {
        mock3.expectedMessageCount(1);
        Message msg = createADT01Message();
        template.sendBodyAndHeader("direct:test3", msg, "validator", defaultValidationContext);
        mock3.assertIsSatisfied();
    }

    @Test
    public void testDynamicDefaultHapiContext() throws Exception {
        mock6.reset();
        mock6.expectedMessageCount(1);
        Message msg = createADT01Message();
        msg.setParser(defaultContext.getPipeParser());
        template.sendBody("direct:test6", msg);
        mock6.assertIsSatisfied();
    }

    @Test
    public void testDynamicCustomHapiContext() throws Exception {
        mock6.reset();
        mock6.expectedMessageCount(0);
        Message msg = createADT01Message();
        msg.setParser(customContext.getPipeParser());
        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:test6", msg));
        mock6.assertIsSatisfied();
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
                    from("direct:test1").validate(messageConformsTo(defaultValidationContext)).to("mock:test1");
                    from("direct:test2").validate(messageConformsTo(customValidationContext)).to("mock:test2");
                    from("direct:test3").validate(messageConformsTo(header("validator"))).to("mock:test3");
                    from("direct:test4").validate(messageConformsTo(defaultContext)).to("mock:test4");
                    from("direct:test5").validate(messageConformsTo(customContext)).to("mock:test5");
                    from("direct:test6").validate(messageConforms()).to("mock:test6");
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
        pid.getPatientIdentifierList(0).getID().setValue("123456");

        return adt;
    }
}
