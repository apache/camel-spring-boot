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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerSseWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStatelessWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Serves the same {@code ai-tool} routes over the stdio transport instead of streamable HTTP.
 * <p>
 * The transport provider is fed with in-memory pipes rather than the real {@code System.in} / {@code System.out}, so
 * the JSON-RPC conversation a stdio MCP client would have with the application can be driven from the test without
 * spawning a process. Supplying the bean also replaces the one the Spring AI auto-configuration would create for
 * {@code spring.ai.mcp.server.stdio=true} (it is annotated with {@code @ConditionalOnMissingBean}); everything below
 * that bean — the MCP server, this starter's engine and the Camel bridge — is the production wiring.
 */
// a stdio application does not depend on spring-ai-starter-mcp-server-webmvc at all; the test classpath has it, so
// its transports are excluded here to leave the stdio one as the only MCP server transport
@EnableAutoConfiguration(exclude = {
        McpServerSseWebMvcAutoConfiguration.class,
        McpServerStreamableHttpWebMvcAutoConfiguration.class,
        McpServerStatelessWebMvcAutoConfiguration.class })
@CamelSpringBootTest
@SpringBootTest(
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                classes = { SpringBootMcpServerStdioTest.TestConfiguration.class },
                properties = {
                        "spring.main.banner-mode=off",
                        "camel.mcp-server.tags=stdio-conformance",
                        "camel.springboot.main-run-controller=false" })
public class SpringBootMcpServerStdioTest {

    private static final PipedOutputStream TO_SERVER;
    private static final PipedInputStream SERVER_IN;
    private static final PipedOutputStream SERVER_OUT;
    private static final BufferedReader FROM_SERVER;

    static {
        try {
            TO_SERVER = new PipedOutputStream();
            SERVER_IN = new PipedInputStream(TO_SERVER);
            SERVER_OUT = new PipedOutputStream();
            FROM_SERVER = new BufferedReader(new InputStreamReader(new PipedInputStream(SERVER_OUT), UTF_8));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void toolsAreServedOverStdio() throws Exception {
        send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\","
             + "\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}");
        assertThat(responseWith("\"id\":1")).contains("\"protocolVersion\"");

        send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

        // only the selected tag is exposed
        send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        assertThat(responseWith("\"id\":2"))
                .contains("say_hello")
                .doesNotContain("hidden_tool");

        send("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
             + "\"params\":{\"name\":\"say_hello\",\"arguments\":{\"name\":\"World\"}}}");
        assertThat(responseWith("\"id\":3")).contains("Hello World");
    }

    private static void send(String message) throws IOException {
        TO_SERVER.write((message + "\n").getBytes(UTF_8));
        TO_SERVER.flush();
    }

    /**
     * Reads server output until the response carrying the given id shows up, skipping the notifications the server
     * emits on its own (such as {@code notifications/tools/list_changed}).
     */
    private static String responseWith(String id) throws IOException {
        for (int i = 0; i < 20; i++) {
            String line = FROM_SERVER.readLine();
            if (line != null && line.contains(id)) {
                return line;
            }
        }
        throw new AssertionError("No response containing " + id + " was received over stdio");
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        McpServerTransportProviderBase stdioServerTransport() {
            return new StdioServerTransportProvider(McpJsonDefaults.getMapper(), SERVER_IN, SERVER_OUT);
        }

        @Bean
        RouteBuilder stdioRoutes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:say_hello?tags=stdio-conformance&description=Say hello"
                         + "&parameter.name=string&parameter.name.description=Who to greet"
                         + "&parameter.name.required=true")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-tool:hidden_tool?description=Untagged tool, must not be exposed")
                            .setBody(constant("hidden"));
                }
            };
        }
    }
}
