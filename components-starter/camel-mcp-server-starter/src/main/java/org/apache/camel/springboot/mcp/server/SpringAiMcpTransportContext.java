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
import java.util.Collection;
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
 * When the principal is a JwtAuthenticationToken, its subject, scopes and claims are also
 * propagated to the Camel tool exchange.
 * <p>
 * Applications that provide their own {@code WebMvcStreamableServerTransportProvider}
 * can preserve caller identity by installing
 * {@code .contextExtractor(SpringAiMcpTransportContext::extract)} on its builder.
 */
public final class SpringAiMcpTransportContext {

    static final String CALL_CONTEXT_KEY =
            SpringAiMcpTransportContext.class.getName() + ".toolCallContext";

    private static final String AUTHENTICATION_CLASS =
            "org.springframework.security.core.Authentication";
    private static final String GRANTED_AUTHORITY_CLASS =
            "org.springframework.security.core.GrantedAuthority";
    private static final String JWT_AUTHENTICATION_CLASS =
            "org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken";

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
                    Class<?> jwtAuthenticationType = Class.forName(JWT_AUTHENTICATION_CLASS, false, loader);
                    if (jwtAuthenticationType.isAssignableFrom(type)) {
                        getTokenAttributes = jwtAuthenticationType.getMethod("getTokenAttributes");
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
        Principal principal = request.principal().orElse(null);
        if (principal == null) {
            return McpTransportContext.EMPTY;
        }

        return McpTransportContext.create(Map.of(CALL_CONTEXT_KEY, toCallContext(principal)));
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

            addScopeClaim(scopes, claims.get("scope"));
            addScopeClaim(scopes, claims.get("scp"));
            addClaimRoles(claims, roles);
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

    private static void addScopeClaim(Set<String> scopes, Object value) {
        if (value instanceof String text) {
            for (String scope : text.trim().split("\\s+")) {
                addScope(scopes, scope);
            }
        } else if (value instanceof Iterable<?> values) {
            for (Object scope : values) {
                if (scope instanceof String name) {
                    addScope(scopes, name);
                }
            }
        }
    }

    private static void addScope(Set<String> scopes, String scope) {
        if (scope != null && !scope.isBlank()) {
            scopes.add(scope);
        }
    }

    /**
     * Mirrors the common claim shapes supported by the Camel Main MCP engine.
     * The first available claim shape wins: roles, realm_access.roles, then groups.
     */
    private static void addClaimRoles(Map<String, Object> claims, Set<String> roles) {
        Object value = claims.get("roles");

        if (!(value instanceof Collection<?>)) {
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map<?, ?> realmMap) {
                value = realmMap.get("roles");
            }
        }

        if (!(value instanceof Collection<?>)) {
            value = claims.get("groups");
        }

        if (value instanceof Collection<?> values) {
            for (Object role : values) {
                if (role instanceof String name) {
                    addRole(roles, name);
                }
            }
        }
    }

    private record AuthenticationAccessors(
            Method getAuthorities, Method getAuthority, Method getTokenAttributes) {
        private static final AuthenticationAccessors NONE = new AuthenticationAccessors(null, null, null);
    }
}
