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
package org.apache.camel.component.validator;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                ValidatorResourceResolverFactoryTest.class
        }
)
public class ValidatorResourceResolverFactoryTest extends ContextTestSupport {

    private static Context jndiContext;

    @Test
    public void testConfigurationOnEndpoint() throws Exception {
        // ensure that validator from test method "testConfigurationOnComponent"
        // is unbind
        jndiContext.unbind("validator");

        String directStart = "direct:start";
        String endpointUri
                = "validator:org/apache/camel/component/validator/xsds/person.xsd?resourceResolverFactory=#resourceResolverFactory";

        execute(directStart, endpointUri);
    }

    @Test
    public void testConfigurationOnComponent() throws Exception {
        // set resource resolver factory on component
        ValidatorComponent validatorComponent = new ValidatorComponent();
        validatorComponent.setResourceResolverFactory(new ResourceResolverFactoryImpl());
        jndiContext.bind("validator", validatorComponent);

        String directStart = "direct:startComponent";
        String endpointUri = "customValidator:org/apache/camel/component/validator/xsds/person.xsd";
        execute(directStart, endpointUri);

    }

    @Bean
    public ValidatorComponent customValidator() {
        ValidatorComponent validatorComponent = new ValidatorComponent();
        validatorComponent.setResourceResolverFactory(new ResourceResolverFactoryImpl());

        return validatorComponent;
    }

    void execute(String directStart, String endpointUri) throws InterruptedException {
        MockEndpoint endEndpoint = context.getEndpoint("mock:end", MockEndpoint.class);
        endEndpoint.reset();
        endEndpoint.expectedMessageCount(1);

        final String body
                = "<p:person user=\"james\" xmlns:p=\"org.person\" xmlns:h=\"org.health.check.person\" xmlns:c=\"org.health.check.common\">\n" //
                  + "  <p:firstName>James</p:firstName>\n" //
                  + "  <p:lastName>Strachan</p:lastName>\n" //
                  + "  <p:city>London</p:city>\n" //
                  + "  <h:health>\n"//
                  + "      <h:lastCheck>2011-12-23</h:lastCheck>\n" //
                  + "      <h:status>OK</h:status>\n" //
                  + "      <c:commonElement>" //
                  + "          <c:element1/>" //
                  + "          <c:element2/>" //
                  + "      </c:commonElement>" //
                  + "  </h:health>\n" //
                  + "</p:person>";

        template.sendBody(directStart, body);

        // wait until endpoint is resolved
        await().atMost(1, TimeUnit.SECONDS).until(() -> context.getEndpoint(endpointUri, ValidatorEndpoint.class) != null);

        MockEndpoint.assertIsSatisfied(endEndpoint);

        ValidatorEndpoint validatorEndpoint = context.getEndpoint(endpointUri, ValidatorEndpoint.class);
        assertNotNull(validatorEndpoint);
        CustomResourceResolver resolver = (CustomResourceResolver) validatorEndpoint.getResourceResolver();

        Set<String> uris = resolver.getResolvedResourceUris();
        checkResourceUri(uris, "../type2.xsd");
        checkResourceUri(uris, "health/health.xsd");
        checkResourceUri(uris, "type1.xsd");
        checkResourceUri(uris, "common/common.xsd");
    }

    void checkResourceUri(Set<String> uris, String resourceUri) {
        assertTrue(uris.contains(resourceUri), "Missing resource uri " + resourceUri + " in resolved resource URI set");
    }

//    @Override
//    protected Registry createRegistry() throws Exception {
//        jndiContext = createJndiContext();
//        jndiContext.bind("resourceResolverFactory", new ResourceResolverFactoryImpl());
//        return new DefaultRegistry(new JndiBeanRepository(jndiContext));
//
//    }

    public static Context createInitialContext() throws Exception {
        try (InputStream in = ValidatorResourceResolverFactoryTest.class.getClassLoader().getResourceAsStream("jndi-example.properties");) {
            assertNotNull(in, "Cannot find jndi-example.properties on the classpath!");
            Properties properties = new Properties();
            properties.load(in);
            return new InitialContext(new Hashtable<>(properties));
        }
    }

    @Bean
    public ResourceResolverFactoryImpl resourceResolverFactory() {
        return new ResourceResolverFactoryImpl();
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        jndiContext = createInitialContext();
        jndiContext.bind("resourceResolverFactory", new ResourceResolverFactoryImpl());
    }

    @Bean
    protected RouteBuilder routeBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .setHeader("xsd_file", new ConstantExpression("org/apache/camel/component/validator/xsds/person.xsd"))
                        .recipientList(new SimpleExpression(
                                "validator:${header.xsd_file}?resourceResolverFactory=#resourceResolverFactory"))
                        .to("mock:end");
            }
        };
    }

    @Bean
    protected RouteBuilder routeBuilderComponent() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:startComponent")
                        .setHeader("xsd_file", new ConstantExpression("org/apache/camel/component/validator/xsds/person.xsd"))
                        .recipientList(new SimpleExpression("customValidator:${header.xsd_file}")).to("mock:end");
            }
        };
    }

    static class ResourceResolverFactoryImpl implements ValidatorResourceResolverFactory {

        @Override
        public LSResourceResolver createResourceResolver(CamelContext camelContext, String rootResourceUri) {
            return new CustomResourceResolver(camelContext, rootResourceUri);
        }

    }

    /** Custom resource resolver which collects all resolved resource URIs. */
    static class CustomResourceResolver extends DefaultLSResourceResolver {

        private final Set<String> resolvedRsourceUris = new HashSet<>();

        CustomResourceResolver(CamelContext camelContext, String resourceUri) {
            super(camelContext, resourceUri);
        }

        public Set<String> getResolvedResourceUris() {
            return resolvedRsourceUris;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            LSInput result = super.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
            resolvedRsourceUris.add(systemId);
            return result;
        }

    }

}
