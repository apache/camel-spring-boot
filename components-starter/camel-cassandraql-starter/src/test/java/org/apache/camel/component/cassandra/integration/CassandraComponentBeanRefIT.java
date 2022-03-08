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
package org.apache.camel.component.cassandra.integration;




import com.datastax.oss.driver.api.core.CqlSession;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cassandra.CassandraEndpoint;
import org.apache.camel.component.cassandra.springboot.BaseCassandra;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CassandraComponentBeanRefIT.class,
        CassandraComponentBeanRefIT.TestConfiguration.class
    }
)
public class CassandraComponentBeanRefIT extends BaseCassandra {

    public static final String CQL = "insert into camel_user(login, first_name, last_name) values (?, ?, ?)";
    public static final String SESSION_URI = "cql:bean:cassandraSession?cql=" + CQL;

    @Bean("cassandraSession")
    protected CqlSession createSession() {
              
        return getSession();
    }
    
    
   

    @Test
    public void testSession() {
        
        
        CassandraEndpoint endpoint = context.getEndpoint(SESSION_URI, CassandraEndpoint.class);
        assertNotNull(endpoint, "No endpoint found for uri: " + SESSION_URI);

        assertEquals(KEYSPACE_NAME, endpoint.getKeyspace());
        assertEquals(CQL, endpoint.getCql());
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
                public void configure() {
                    from("direct:inputSession").to(SESSION_URI);
                }
            };
        }
    }
}
