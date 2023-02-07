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

    @Bean
    FhirContext fhirContext(){
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.R4);
        fhirContext.getRestfulClientFactory().setSocketTimeout(20 * 1000);
        // Set proxy so that FHIR resource URLs returned by the server are using the correct host and port
        fhirContext.getRestfulClientFactory().setProxy(AbstractFhirTestSupport.service.getHost(), AbstractFhirTestSupport.service.getPort());
        return fhirContext;
    }

    @Bean
    IGenericClient fhirClient(FhirContext fc){
        return  fc.newRestfulGenericClient(AbstractFhirTestSupport.service.getServiceBaseURL());
    }
}
