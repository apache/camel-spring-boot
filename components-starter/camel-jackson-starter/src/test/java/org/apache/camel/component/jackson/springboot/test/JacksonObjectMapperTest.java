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
package org.apache.camel.component.jackson.springboot.test;


import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;



import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JacksonObjectMapperTest.class,
        JacksonObjectMapperTest.TestConfiguration.class
    }
)

public class JacksonObjectMapperTest extends JacksonMarshalTest {

   
    

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    ObjectMapper mapper = new ObjectMapper();
                    JacksonDataFormat format = new JacksonDataFormat();
                    format.setObjectMapper(mapper);

                    from("direct:in").marshal(format);
                    from("direct:back").unmarshal(format).to("mock:reverse");

                    JacksonDataFormat prettyPrintDataFormat = new JacksonDataFormat();
                    prettyPrintDataFormat.setPrettyPrint(true);

                    from("direct:inPretty").marshal(prettyPrintDataFormat);
                    from("direct:backPretty").unmarshal(prettyPrintDataFormat).to("mock:reverse");

                    JacksonDataFormat formatPojo = new JacksonDataFormat(TestPojo.class);

                    from("direct:inPojo").marshal(formatPojo);
                    from("direct:backPojo").unmarshal(formatPojo).to("mock:reversePojo");
                }
            };
        }
        
      
    }
    
    
    
    
}
