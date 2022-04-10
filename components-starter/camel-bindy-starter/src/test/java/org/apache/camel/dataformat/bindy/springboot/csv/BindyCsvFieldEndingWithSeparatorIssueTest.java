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
package org.apache.camel.dataformat.bindy.springboot.csv;


import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.model.csv.MyCsvRecord;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;


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
        BindyCsvFieldEndingWithSeparatorIssueTest.class,
        BindyCsvFieldEndingWithSeparatorIssueTest.TestConfiguration.class
    }
)
public class BindyCsvFieldEndingWithSeparatorIssueTest {

    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testBindy() throws Exception {
        mock.reset();

        String addressLine1 = "8506 SIX FORKS ROAD,";

        mock.expectedPropertyReceived("addressLine1", addressLine1);

        String csvLine = "\"PROBLEM SOLVER\",\"" + addressLine1
                         + "\",\"SUITE 104\",\"RALEIGH\",\"NC\",\"27615\",\"US\"";
        template.sendBody("direct:fromCsv", csvLine.trim());

        mock.assertIsSatisfied();

        // The algorithm of BindyCvsDataFormat.unquoteTokens(..) does not handle
        // separator at end of a field
        // addressLine1 results in the next field being appended -> '8506 SIX
        // FORKS ROAD,,SUITE 104'
    }

    @Test
    public void testBindyMoreSeparators() throws Exception {
        mock.reset();

        String addressLine1 = "8506 SIX FORKS ROAD, , ,,, ,";

        
        mock.expectedPropertyReceived("addressLine1", addressLine1);

        String csvLine = "\"PROBLEM SOLVER\",\"" + addressLine1
                         + "\",\"SUITE 104\",\"RALEIGH\",\"NC\",\"27615\",\"US\"";
        template.sendBody("direct:fromCsv", csvLine.trim());

        mock.assertIsSatisfied();

    }

    @Test
    public void testBindySeparatorsAround() throws Exception {
        mock.reset();

         
        String addressLine1 = ",8506 SIX FORKS ROAD,";

        mock.expectedPropertyReceived("addressLine1", addressLine1);

        String csvLine = "\"PROBLEM SOLVER\",\"" + addressLine1
                         + "\",\"SUITE 104\",\"RALEIGH\",\"NC\",\"27615\",\"US\"";
        
        template.sendBody("direct:fromCsv", csvLine.trim());

        mock.assertIsSatisfied();

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
                    from("direct:fromCsv").unmarshal().bindy(BindyType.Csv, MyCsvRecord.class)
                            .setProperty("addressLine1", simple("${in.body.addressLine1}"))
                            .setProperty("addressLine2", simple("${in.body.addressLine2}")).log("${in.body}")
                            .to("mock:result");
                }
            };
        }
    }
    
    

}
