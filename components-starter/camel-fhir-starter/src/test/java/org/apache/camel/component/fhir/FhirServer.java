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
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.camel.test.infra.fhir.services.FhirService;
import org.apache.camel.test.infra.fhir.services.FhirServiceFactory;
import org.springframework.context.annotation.Bean;

/**
 * Starts the FHIR server
 */
public class FhirServer {

    static FhirService service;

    static {
        // We don't want a new FHIR server for every test class
        // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
        service = FhirServiceFactory.createService();
        service.initialize();
    }

    @Bean
    FhirContext fhirContext(){
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.DSTU3);
        // Set proxy so that FHIR resource URLs returned by the server are using the correct host and port
        fhirContext.getRestfulClientFactory().setProxy(service.getHost(), service.getPort());
        return fhirContext;
    }

    @Bean
    IGenericClient fhirClient(FhirContext fc){
        return  fc.newRestfulGenericClient(service.getServiceBaseURL());
    }
}
