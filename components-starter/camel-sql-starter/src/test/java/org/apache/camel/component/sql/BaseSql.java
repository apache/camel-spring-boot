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