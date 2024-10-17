package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.concurrent.Executor;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpMultipleExecutorsTest.class, SpringBootPlatformHttpMultipleExecutorsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
@EnableScheduling
public class SpringBootPlatformHttpMultipleExecutorsTest extends PlatformHttpBase {

    private static final String postRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_mypost";

    private static final String getRouteId = "SpringBootPlatformHttpMultipleExecutorsTest_myget";

    private static final String THREAD_PREFIX = "myThread-";

    // *************************************
    // Config
    // *************************************
    @Configuration
    public static class TestConfiguration {

        @Bean(name = "customPoolTaskExecutor")
        public Executor customPoolTaskExecutor() {
            final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(2);
            executor.setQueueCapacity(500);
            executor.setThreadNamePrefix(THREAD_PREFIX);
            executor.initialize();
            return executor;
        }

        @Bean
        public RouteBuilder servletPlatformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/myget").id(postRouteId).setBody().constant("get");
                    from("platform-http:/mypost").id(getRouteId).transform().body(String.class, b -> b.toUpperCase());

                    from("platform-http:/executor").process(exchange -> exchange.getIn().setBody(Thread.currentThread().getName()));
                }
            };
        }
    }

    @Override
    protected String getPostRouteId() {
        return postRouteId;
    }

    @Override
    protected String getGetRouteId() {
        return getRouteId;
    }

    @Autowired
    List<Executor> executors;

    @Test
    public void checkCustomExecutorIsPickedWhenMultipleExecutorsAreDefined() {
        Assertions.assertThat(executors).hasSizeGreaterThan(1);

        Assertions.assertThat(restTemplate.postForEntity("/executor", "test", String.class).getBody())
                        .contains(THREAD_PREFIX);
    }
}
