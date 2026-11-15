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
package org.apache.camel.springboot.mcp.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that camel.mcp-server.allowed-origins replaces the default loopback
 * allow-list and that requests without Origin remain supported.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SpringBootMcpServerOriginConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.main.banner-mode=off",
                "spring.ai.mcp.server.protocol=STREAMABLE",
                "camel.mcp-server.tags=origin-tests",
                "camel.mcp-server.allowed-origins=https://trusted.example",
                "camel.springboot.main-run-controller=false"
        })
public class SpringBootMcpServerOriginConfigurationTest {

    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("origin-user:origin-secret".getBytes(StandardCharsets.UTF_8));

    @LocalServerPort
    private int port;

    @Test
    void configuredRemoteOriginReplacesLoopbackDefaults() throws Exception {
        HttpResponse<String> trusted = initialize("https://trusted.example");

        assertThat(trusted.statusCode())
                .as("Configured remote Origin should be accepted: " + trusted.body())
                .isEqualTo(200);

        String trustedSession = trusted.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        assertThat(delete(trustedSession, "https://trusted.example")).isEqualTo(200);

        HttpResponse<String> loopback = initialize("http://localhost:5173");

        assertThat(loopback.statusCode())
                .as("Loopback defaults must be replaced when a custom list is configured")
                .isEqualTo(403);

        HttpResponse<String> attacker = initialize("https://attacker.example");

        assertThat(attacker.statusCode()).isEqualTo(403);

        // Process-based MCP clients that omit Origin remain supported.
        HttpResponse<String> noOrigin = initialize(null);

        assertThat(noOrigin.statusCode())
                .as("Requests without Origin should remain accepted: " + noOrigin.body())
                .isEqualTo(200);

        String noOriginSession = noOrigin.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        assertThat(delete(noOriginSession, null)).isEqualTo(200);
    }

    private HttpResponse<String> initialize(String origin) throws Exception {
        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-06-18",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "camel-origin-configuration-test",
                      "version": "1.0"
                    }
                  }
                }
                """;

        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", AUTHORIZATION)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json");

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.POST(HttpRequest.BodyPublishers.ofString(body));

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private int delete(String sessionId, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", AUTHORIZATION)
                .header("Mcp-Session-Id", sessionId);

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.DELETE();

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public InMemoryUserDetailsManager originTestUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("origin-user")
                            .password("{noop}origin-secret")
                            .roles("MCP_USER")
                            .build());
        }

        @Bean
        public SecurityFilterChain originTestSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/mcp"))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/mcp").authenticated()
                            .anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
