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

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Captures the authenticated HTTP caller when Spring AI creates the MCP transport context,
 * and makes that caller available to Camel tool execution.
 * <p>
 * The identity is taken from the servlet request principal established by the application's
 * security infrastructure. It is not derived from MCP tool arguments or client-supplied
 * identity headers.
 * <p>
 * Spring Security is optional for applications using this starter. When its Authentication
 * API is present, granted authorities are copied into the runtime-neutral MCP call context.
 * <p>
 * Applications that provide their own {@code WebMvcStreamableServerTransportProvider}
 * can preserve caller identity by installing
 * {@code .contextExtractor(SpringAiMcpTransportContext::extract)} on its builder.
 */
public final class SpringAiMcpTransportContext {

    private static final String CALL_CONTEXT_KEY =
            SpringAiMcpTransportContext.class.getName() + ".toolCallContext";

    private SpringAiMcpTransportContext() {
    }

    /**
     * Extracts the authenticated caller from the current HTTP request.
     *
     * @param request the Spring MVC request received by the MCP transport
     * @return an MCP transport context containing the caller, or the empty context
     */
    public static McpTransportContext extract(ServerRequest request) {
        Principal principal = request.principal().orElse(null);
        if (principal == null) {
            return McpTransportContext.EMPTY;
        }

        McpToolCallContext callContext = new McpToolCallContext(
                principal, principal.getName(), principalRoles(principal));

        return McpTransportContext.create(Map.of(CALL_CONTEXT_KEY, callContext));
    }

    /**
     * Retrieves the caller context attached to the MCP request by the transport extractor.
     *
     * @param exchange the MCP SDK server exchange
     * @return the captured caller context, or the empty context
     */
    static McpToolCallContext from(McpSyncServerExchange exchange) {
        if (exchange == null || exchange.transportContext() == null) {
            return McpToolCallContext.EMPTY;
        }

        Object value = exchange.transportContext().get(CALL_CONTEXT_KEY);
        return value instanceof McpToolCallContext context
                ? context
                : McpToolCallContext.EMPTY;
    }

    /**
     * Reads Spring Security Authentication authorities without introducing a mandatory
     * Spring Security dependency into the Camel MCP server starter.
     */
    private static List<String> principalRoles(Principal principal) {
        ClassLoader classLoader = SpringAiMcpTransportContext.class.getClassLoader();

        try {
            Class<?> authenticationType = Class.forName(
                    "org.springframework.security.core.Authentication", false, classLoader);

            if (!authenticationType.isInstance(principal)) {
                return List.of();
            }

            Class<?> grantedAuthorityType = Class.forName(
                    "org.springframework.security.core.GrantedAuthority", false, classLoader);

            Object authorities = authenticationType.getMethod("getAuthorities").invoke(principal);
            if (!(authorities instanceof Iterable<?> values)) {
                return List.of();
            }

            Method getAuthority = grantedAuthorityType.getMethod("getAuthority");
            List<String> roles = new ArrayList<>();

            for (Object authority : values) {
                if (!grantedAuthorityType.isInstance(authority)) {
                    continue;
                }

                Object name = getAuthority.invoke(authority);
                if (name instanceof String role && !role.isBlank() && !roles.contains(role)) {
                    roles.add(role);
                }
            }

            return List.copyOf(roles);
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            // No Spring Security API, or an unsupported principal implementation.
            // An absent role list is safer than deriving roles from untrusted request data.
            return List.of();
        }
    }
}
