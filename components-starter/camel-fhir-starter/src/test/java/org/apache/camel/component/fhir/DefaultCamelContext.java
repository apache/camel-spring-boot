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

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import static org.apache.camel.component.fhir.FhirServer.service;

public class DefaultCamelContext {

    @Autowired
    FhirContext fhirContext;

    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                final FhirConfiguration configuration = new FhirConfiguration();
                configuration.setServerUrl(service.getServiceBaseURL());
                configuration.setFhirContext(fhirContext);

                // add FhirComponent to Camel context
                final FhirComponent component = new FhirComponent(context);
                component.setConfiguration(configuration);
                context.addComponent("fhir", component);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }
        };
    }
}
