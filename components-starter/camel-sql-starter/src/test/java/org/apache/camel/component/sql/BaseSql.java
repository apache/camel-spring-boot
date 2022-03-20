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
package org.apache.camel.component.sql;

import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

public class BaseSql {

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate template;

    protected static EmbeddedDatabase initEmptyDb() {
        return initDb(null);
    }

    protected static EmbeddedDatabase initDb() {
        return initDb("sql/createAndPopulateDatabase.sql");
    }

    protected static EmbeddedDatabase initDb(String script) {
        return initDb(EmbeddedDatabaseType.DERBY, script);
    }

    protected static EmbeddedDatabase initDb(EmbeddedDatabaseType type, String script) {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
                .setName(BaseSql.class.getSimpleName())
                .setType(type);
        if(script != null && !"".equals(script)) {
            builder.addScript(script);
        }
        EmbeddedDatabase ed =  builder.build();
        return ed;
    }

    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(this.context);
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public DisposableBean embeddedDatabaseShutdownExecutor(DataSource dataSource) {
            return new DisposableBean() {

                @Override
                public void destroy() throws Exception {
                    ((EmbeddedDatabase)dataSource).shutdown();
                }

            };
        }
    }
}