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
package org.apache.camel.component.hashicorp.vault.springboot;

import org.apache.camel.RuntimeCamelException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A placeholder that matched the vault prefix but could not be resolved must not be left in place, since the literal
 * placeholder text would then become the effective value of whatever it configures.
 */
public class EarlyResolutionFailureTest {

    private static final String SECRET_KEY = "my.secret";

    private static ApplicationEnvironmentPreparedEvent eventWith(boolean ignoreResolutionFailures) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("camel.component.hashicorp-vault.early-resolve-properties", "true");
        props.put("camel.vault.hashicorp.token", "not-a-real-token");
        props.put("camel.vault.hashicorp.host", "127.0.0.1");
        // nothing listens here, so the lookup fails locally without touching the network
        props.put("camel.vault.hashicorp.port", "1");
        props.put("camel.vault.hashicorp.scheme", "http");
        props.put(SECRET_KEY, "{{hashicorp:secret:does/not/exist#value}}");
        if (ignoreResolutionFailures) {
            props.put("camel.vault.ignore-resolution-failures", "true");
        }

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-properties", props));

        return new ApplicationEnvironmentPreparedEvent(
                new DefaultBootstrapContext(), new SpringApplication(), new String[0], environment);
    }

    @Test
    public void unresolvableSecretAbortsStartup() {
        SpringBootHashicorpVaultPropertiesParser parser = new SpringBootHashicorpVaultPropertiesParser();

        RuntimeCamelException thrown = assertThrows(RuntimeCamelException.class,
                () -> parser.onApplicationEvent(eventWith(false)));

        assertTrue(thrown.getMessage().contains(SECRET_KEY),
                "the failure should name the property that could not be resolved, was: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("camel.vault.ignore-resolution-failures"),
                "the failure should point at the opt-back property, was: " + thrown.getMessage());
    }

    @Test
    public void ignoreResolutionFailuresRestoresTheTolerantBehaviour() {
        SpringBootHashicorpVaultPropertiesParser parser = new SpringBootHashicorpVaultPropertiesParser();

        assertDoesNotThrow(() -> parser.onApplicationEvent(eventWith(true)));
    }
}
