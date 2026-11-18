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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Spring Security is optional for applications using this starter. Its APIs are accessed
 * reflectively, and reflective accessors are cached per Authentication implementation.
 * <p>
 * Spring authorities with the ROLE_ prefix are exposed without that prefix. SCOPE_ authorities
 * are mapped to OAuth scopes, and FACTOR_ authorities are not treated as application roles.
 * When the principal is a Spring OAuth2 token authentication, including JWT and opaque bearer-token
 * authentication, its subject and token attributes are also propagated to the Camel tool exchange.
 * Only Spring Security authorities are used to derive Camel roles and scopes; raw token claims are
 * retained as OAuth claims and never promoted to authorization roles.
 * <p>
 * Applications that provide their own {@code WebMvcStreamableServerTransportProvider}
 * can preserve caller identity by installing
 * {@code .contextExtractor(SpringAiMcpTransportContext::extract)} on its builder.
 */
public final class SpringAiMcpTransportContext {

    /** Key used to store Camel's authenticated caller context in the MCP transport context. */
    public static final String CALL_CONTEXT_KEY =
            SpringAiMcpTransportContext.class.getName() + ".toolCallContext";

    private static final String AUTHENTICATION_CLASS =
            "org.springframework.security.core.Authentication";
    private static final String GRANTED_AUTHORITY_CLASS =
            "org.springframework.security.core.GrantedAuthority";
    private static final String OAUTH2_TOKEN_AUTHENTICATION_CLASS =
            "org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken";

    private static final ClassValue<AuthenticationAccessors> ACCESSORS = new ClassValue<>() {
        @Override
        protected AuthenticationAccessors computeValue(Class<?> type) {
            ClassLoader loader = type.getClassLoader();
            if (loader == null) {
                loader = SpringAiMcpTransportContext.class.getClassLoader();
            }

            try {
                Class<?> authenticationType = Class.forName(AUTHENTICATION_CLASS, false, loader);
                if (!authenticationType.isAssignableFrom(type)) {
                    return AuthenticationAccessors.NONE;
                }

                Class<?> authorityType = Class.forName(GRANTED_AUTHORITY_CLASS, false, loader);
                Method getAuthorities = authenticationType.getMethod("getAuthorities");
                Method getAuthority = authorityType.getMethod("getAuthority");

                Method getTokenAttributes = null;
                try {
                    Class<?> oauth2TokenAuthenticationType =
                            Class.forName(OAUTH2_TOKEN_AUTHENTICATION_CLASS, false, loader);
                    if (oauth2TokenAuthenticationType.isAssignableFrom(type)) {
                        getTokenAttributes = oauth2TokenAuthenticationType.getMethod("getTokenAttributes");
                    }
                } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                    // OAuth2 Resource Server is optional.
                }

                return new AuthenticationAccessors(getAuthorities, getAuthority, getTokenAttributes);
            } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
                return AuthenticationAccessors.NONE;
            }
        }
    };

    private SpringAiMcpTransportContext() {
    }

    /**
     * Extracts the authenticated caller from the current HTTP request.
     *
     * @param request the Spring MVC request received by the MCP transport
     * @return an MCP transport context containing the caller, or the empty context
     */
    public static McpTransportContext extract(ServerRequest request) {
        return extract(request, Map.of());
    }

    /**
     * Extracts the authenticated caller and composes Camel's caller context with application-owned entries.
     * The Camel caller-context key is reserved and cannot be replaced by an application entry.
     *
     * @param request the Spring MVC request received by the MCP transport
     * @param additionalEntries application-owned transport-context entries; may be null
     * @return the composed transport context, or the empty context when it has no entries
     */
    public static McpTransportContext extract(ServerRequest request, Map<String, ?> additionalEntries) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (additionalEntries != null) {
            additionalEntries.forEach((key, value) -> {
                if (key != null && value != null && !CALL_CONTEXT_KEY.equals(key)) {
                    context.put(key, value);
                }
            });
        }

        Principal principal = request.principal().orElse(null);
        if (principal != null) {
            context.put(CALL_CONTEXT_KEY, toCallContext(principal));
        }

        return context.isEmpty() ? McpTransportContext.EMPTY : McpTransportContext.create(context);
    }

    /**
     * Retrieves the caller context attached to the MCP request by the transport extractor.
     *
     * @param exchange the MCP SDK server exchange
     * @return the captured caller context, or the empty context
     */
    static McpToolCallContext from(McpSyncServerExchange exchange) {
        return from(exchange != null ? exchange.transportContext() : null);
    }

    /**
     * Retrieves the caller context attached to the MCP request by the transport extractor, as the stateless MCP server
     * passes it to the handlers.
     *
     * @param transportContext the MCP transport context of the request
     * @return the captured caller context, or the empty context
     */
    static McpToolCallContext from(McpTransportContext transportContext) {
        if (transportContext == null) {
            return McpToolCallContext.EMPTY;
        }

        Object value = transportContext.get(CALL_CONTEXT_KEY);
        return value instanceof McpToolCallContext context
                ? context
                : McpToolCallContext.EMPTY;
    }

    private static McpToolCallContext toCallContext(Principal principal) {
        AuthenticationAccessors accessors = ACCESSORS.get(principal.getClass());
        Set<String> roles = new LinkedHashSet<>();
        Set<String> scopes = new LinkedHashSet<>();
        Map<String, Object> claims = null;

        if (accessors != AuthenticationAccessors.NONE) {
            readAuthorities(principal, accessors, roles, scopes);
            claims = readJwtClaims(principal, accessors);
        }

        String oauthSubject = null;
        if (claims != null) {
            Object subject = claims.get("sub");
            if (subject instanceof String value && !value.isBlank()) {
                oauthSubject = value;
            }

        }

        return new McpToolCallContext(
                principal,
                principal.getName(),
                List.copyOf(roles),
                oauthSubject,
                scopes.isEmpty() ? null : List.copyOf(scopes),
                claims,
                null);
    }

    private static void readAuthorities(
            Principal principal,
            AuthenticationAccessors accessors,
            Set<String> roles,
            Set<String> scopes) {

        try {
            Object authorities = accessors.getAuthorities().invoke(principal);
            if (!(authorities instanceof Iterable<?> values)) {
                return;
            }

            for (Object authority : values) {
                if (authority == null) {
                    continue;
                }

                Object value = accessors.getAuthority().invoke(authority);
                if (value instanceof String name) {
                    addAuthority(name, roles, scopes);
                }
            }
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            // An unavailable or unsupported authority representation fails closed.
        }
    }

    private static Map<String, Object> readJwtClaims(
            Principal principal, AuthenticationAccessors accessors) {

        if (accessors.getTokenAttributes() == null) {
            return null;
        }

        try {
            Object value = accessors.getTokenAttributes().invoke(principal);
            if (!(value instanceof Map<?, ?> values)) {
                return null;
            }

            Map<String, Object> claims = new LinkedHashMap<>();
            values.forEach((key, claim) -> {
                if (key instanceof String name) {
                    claims.put(name, claim);
                }
            });
            return claims;
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            return null;
        }
    }

    private static void addAuthority(String authority, Set<String> roles, Set<String> scopes) {
        if (authority == null || authority.isBlank() || authority.startsWith("FACTOR_")) {
            return;
        }

        if (authority.startsWith("SCOPE_")) {
            addScope(scopes, authority.substring("SCOPE_".length()));
        } else {
            addRole(roles, authority);
        }
    }

    private static void addRole(Set<String> roles, String role) {
        if (role == null || role.isBlank() || role.startsWith("FACTOR_")) {
            return;
        }

        String normalized = role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        if (!normalized.isBlank()) {
            roles.add(normalized);
        }
    }

    private static void addScope(Set<String> scopes, String scope) {
        if (scope != null && !scope.isBlank()) {
            scopes.add(scope);
        }
    }

    private static final class AuthenticationAccessors {

        private static final AuthenticationAccessors NONE = new AuthenticationAccessors(null, null, null);

        private final Method getAuthorities;
        private final Method getAuthority;
        private final Method getTokenAttributes;

        private AuthenticationAccessors(
                Method getAuthorities, Method getAuthority, Method getTokenAttributes) {
            this.getAuthorities = getAuthorities;
            this.getAuthority = getAuthority;
            this.getTokenAttributes = getTokenAttributes;
        }

        private Method getAuthorities() {
            return getAuthorities;
        }

        private Method getAuthority() {
            return getAuthority;
        }

        private Method getTokenAttributes() {
            return getTokenAttributes;
        }
    }
}
