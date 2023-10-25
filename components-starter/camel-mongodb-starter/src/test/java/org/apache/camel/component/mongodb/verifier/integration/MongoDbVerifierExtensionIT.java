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
package org.apache.camel.component.mongodb.verifier.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Component;
import org.apache.camel.component.extension.ComponentVerifierExtension;
import org.apache.camel.component.mongodb.integration.AbstractMongoDbITSupport;
import org.apache.camel.component.mongodb.processor.idempotent.MongoDbIdempotentRepositoryIT;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                MongoDbVerifierExtensionIT.class,
                AbstractMongoDbITSupport.MongoConfiguration.class
        }
)
public class MongoDbVerifierExtensionIT extends AbstractMongoDbITSupport {
    // We simulate the presence of an authenticated user
    @BeforeEach
    public void createAuthorizationUser() throws IOException {
        super.createAuthorizationUser();
    }

    protected ComponentVerifierExtension getExtension() {
        Component component = context.getComponent(SCHEME);
        ComponentVerifierExtension verifier
                = component.getExtension(ComponentVerifierExtension.class).orElseThrow(IllegalStateException::new);

        return verifier;
    }

    @Test
    public void verifyConnectionOK() throws IOException {
        Properties properties = loadAuthProperties();
        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", service.getConnectionAddress());
        parameters.put("user", properties.getProperty("testusername"));
        parameters.put("password", properties.getProperty("testpassword"));
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.CONNECTIVITY, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.OK, result.getStatus());
    }

    @Test
    public void verifyConnectionKO() throws IOException {
        Properties properties = loadAuthProperties();
        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", "notReachable.host");
        parameters.put("user", properties.getProperty("testusername"));
        parameters.put("password", properties.getProperty("testpassword"));
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.CONNECTIVITY, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.ERROR, result.getStatus());
        assertTrue(result.getErrors().get(0).getDescription().startsWith("Unable to connect"));
    }

    @Test
    public void verifyConnectionMissingParams() throws IOException {
        Properties properties = loadAuthProperties();

        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", service.getConnectionAddress());
        parameters.put("user", properties.getProperty("testusername"));
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.PARAMETERS, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.ERROR, result.getStatus());
        assertTrue(result.getErrors().get(0).getDescription().startsWith("password should be set"));
    }

    @Test
    public void verifyConnectionNotAuthenticated() throws IOException {
        Properties properties = loadAuthProperties();

        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", service.getConnectionAddress());
        parameters.put("user", properties.getProperty("wrongusername"));
        parameters.put("password", properties.getProperty("wrongpassword"));
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.CONNECTIVITY, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.ERROR, result.getStatus());
        assertTrue(result.getErrors().get(0).getDescription().startsWith("Unable to authenticate"));
    }

    @Test
    public void verifyConnectionAdminDBKO() throws IOException {
        Properties properties = loadAuthProperties();

        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", service.getConnectionAddress());
        parameters.put("user", properties.getProperty("testusername"));
        parameters.put("password", properties.getProperty("testpassword"));
        parameters.put("adminDB", "someAdminDB");
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.CONNECTIVITY, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.ERROR, result.getStatus());
        assertTrue(result.getErrors().get(0).getDescription().startsWith("Unable to authenticate"));
    }

    @Test
    public void verifyConnectionPortKO() throws IOException {
        Properties properties = loadAuthProperties();

        //When
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("host", "localhost:12343");
        parameters.put("user", properties.getProperty("testusername"));
        parameters.put("password", properties.getProperty("testpassword"));
        //Given
        ComponentVerifierExtension.Result result
                = getExtension().verify(ComponentVerifierExtension.Scope.CONNECTIVITY, parameters);
        //Then
        assertEquals(ComponentVerifierExtension.Result.Status.ERROR, result.getStatus());
        assertTrue(result.getErrors().get(0).getDescription().startsWith("Unable to connect"));
    }

}
