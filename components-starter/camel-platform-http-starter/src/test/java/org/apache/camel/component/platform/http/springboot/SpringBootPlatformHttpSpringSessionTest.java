package org.apache.camel.component.platform.http.springboot;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.hazelcast.PrincipalNameExtractor;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.test.annotation.DirtiesContext;

@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpSpringSessionTest.class, SpringBootPlatformHttpSpringSessionTest.TestConfiguration.class,
        SpringBootPlatformHttpSessionTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpSpringSessionTest extends SpringBootPlatformHttpSessionTest {

    @Override
    protected String getSessionKey() {
        return "SESSION";
    }

    @Configuration
    @EnableHazelcastHttpSession(maxInactiveIntervalInSeconds = 150)
    public static class TestConfiguration {

        @Bean(destroyMethod = "shutdown")
        public HazelcastInstance hazelcastInstance() {
            Config config = new Config();
            NetworkConfig networkConfig = config.getNetworkConfig();
            networkConfig.setPort(0);
            networkConfig.getJoin().getAutoDetectionConfig().setEnabled(false);
            AttributeConfig attributeConfig = new AttributeConfig()
                    .setName(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                    .setExtractorClassName(PrincipalNameExtractor.class.getName());
            config.getMapConfig(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME)
                    .addAttributeConfig(attributeConfig)
                    .addIndexConfig(
                            new IndexConfig(IndexType.HASH, HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));
            return Hazelcast.newHazelcastInstance(config);
        }
    }
}
