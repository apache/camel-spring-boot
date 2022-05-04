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
package org.apache.camel.component.mllp.springboot;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpServerCharsetTest.class,
        MllpTcpServerCharsetTest.TestConfiguration.class
    }
)
public class MllpTcpServerCharsetTest {
    
    static final String TEST_MESSAGE
        = "MSH|^~\\&|REQUESTING|ICE|INHOUSE|RTH00|20161206193919||ORM^O01|00001|D|2.3||||||ISO_IR 100|" + '\r'
      + "PID|1||ICE999999^^^ICE^ICE||Testpatient^Testy^^^Mr||19740401|M|||123 Barrel Drive^^^^SW18 4RT|||||2||||||||||||||"
      + '\r'
      + "NTE|1||Free text for entering clinical details|" + '\r'
      + "PV1|1||^^^^^^^^Admin Location|||||||||||||||NHS|" + '\r'
      + "ORC|NW|213||175|REQ||||20080808093202|ahsl^^Administrator||G999999^TestDoctor^GPtests^^^^^^NAT|^^^^^^^^Admin Location | 819600|200808080932||RTH00||ahsl^^Administrator||"
      + '\r'
      + "OBR|1|213||CCOR^Serum Cortisol ^ JRH06|||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||"
      + '\r'
      + "OBR|2|213||GCU^Serum Copper ^ JRH06 |||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||"
      + '\r'
      + "OBR|3|213||THYG^Serum Thyroglobulin ^JRH06|||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||"
      + '\r'
      + '\n';
    
    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();

    @EndpointInject("mock://target")
    MockEndpoint target;

    

    @Autowired
    ProducerTemplate template;
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpTcpServerCharsetTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testReceiveMessageWithInvalidMsh18() throws Exception {
        target.reset();
        target.expectedMinimumMessageCount(1);

        mllpClient.connect();

        mllpClient.sendMessageAndWaitForAcknowledgement(TEST_MESSAGE);

        target.assertIsSatisfied(5000);
    }

    @Test
    public void testReceiveMessageWithValidMsh18() throws Exception {
        target.reset();
        target.expectedMinimumMessageCount(1);

        mllpClient.connect();

        mllpClient.sendMessageAndWaitForAcknowledgement(TEST_MESSAGE.replace("ISO_IR 100", "ISO-IR-100"));

        target.assertIsSatisfied(5000);
    }
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                  
                    String routeId = "mllp-sender";
                    mllpClient.setMllpHost("localhost");
                    mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());

                    fromF("mllp://%d?receiveTimeout=1000&readTimeout=500&charsetName=ISO-IR-100", mllpClient.getMllpPort())
                    .log(LoggingLevel.INFO, routeId, "Sending Message")
                    .to("mock://target");
                }
            };
        }
    }
    
   

}
