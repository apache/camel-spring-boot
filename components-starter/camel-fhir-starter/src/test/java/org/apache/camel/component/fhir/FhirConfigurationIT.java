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
package org.apache.camel.component.fhir;

import java.util.List;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.fhir.internal.FhirApiCollection;
import org.apache.camel.component.fhir.internal.FhirCreateApiMethod;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link FhirConfiguration} APIs.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FhirConfigurationIT.class,
                FhirConfigurationIT.TestConfiguration.class,
                CustomFhirConfiguration.class,
                FhirServer.class
        }
)
public class FhirConfigurationIT extends AbstractFhirTestSupport {

    private static final String PATH_PREFIX = FhirApiCollection.getCollection().getApiName(FhirCreateApiMethod.class).getName();

    private static final String TEST_URI = "fhir://" + PATH_PREFIX + "/resource?inBody=resourceAsString&log=true&"
                                           + "encoding=JSON&summary=TEXT&compress=true&username=art&password=tatum&sessionCookie=mycookie%3DChips%20Ahoy"
                                           + "&accessToken=token&serverUrl=http://localhost:8080/hapi-fhir-jpaserver-example/baseR4&fhirVersion=R4";

    @Autowired
    private IClientInterceptor mockClientInterceptor;

    @Autowired
    private FhirConfiguration componentConfiguration;

    @Bean
    protected CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                // add FhirComponent to Camel context but don't set up componentConfiguration
                final FhirComponent component = new FhirComponent(context);
                component.setConfiguration(componentConfiguration);
                final IGenericClient client = component.createClient(componentConfiguration);
                componentConfiguration.setClient(client);
                client.registerInterceptor(mockClientInterceptor);
                context.addComponent("fhir", component);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }
        };
    }

    @Test
    public void testConfiguration() throws Exception {
        FhirEndpoint endpoint = camelContext.getEndpoint(TEST_URI, FhirEndpoint.class);
        GenericClient client = (GenericClient) endpoint.getClient();
        FhirConfiguration configuration = endpoint.getConfiguration();
        assertEquals(this.componentConfiguration, configuration);
        assertEquals("http://localhost:8080/hapi-fhir-jpaserver-example/baseR4", client.getUrlBase());
        assertEquals(EncodingEnum.JSON, client.getEncoding());
        assertEquals(SummaryEnum.TEXT, client.getSummary());
        List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();
        assertEquals(6, interceptors.size());

        assertTrue(interceptors.contains(this.mockClientInterceptor), "User defined IClientInterceptor not found");

        long counter = PluginHelper.getBeanIntrospection(camelContext).getInvokedCounter();
        assertEquals(0, counter, "Should not use reflection");
    }

    @Override
    public void cleanFhirServerState() {
        // do nothing
    }

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct://CONFIGURATION").to(TEST_URI);
                }
            };
        }
    }
}
