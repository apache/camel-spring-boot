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

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.modelcontextprotocol.common.McpTransportContext;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiMcpTransportContextTest {

    @Test
    void extractsJwtIdentityRolesScopesAndClaims() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("alice")
                .claim("scope", "mcp:read mcp:write")
                .claim("scp", List.of("mcp:read", "mcp:delete"))
                .claim("roles", List.of("MCP_USER"))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_MCP_ADMIN"),
                        new SimpleGrantedAuthority("SCOPE_mcp:custom"),
                        new SimpleGrantedAuthority("FACTOR_BEARER"),
                        new SimpleGrantedAuthority("OPS")));

        McpToolCallContext context = extract(authentication);

        assertThat(context.securityPrincipal()).isSameAs(authentication);
        assertThat(context.principalName()).isEqualTo("alice");
        assertThat(context.oauthSubject()).isEqualTo("alice");
        assertThat(context.principalRoles()).containsExactly("MCP_ADMIN", "OPS", "MCP_USER");
        assertThat(context.oauthScopes())
                .containsExactly("mcp:custom", "mcp:read", "mcp:write", "mcp:delete");
        assertThat(context.oauthClaims())
                .containsEntry("sub", "alice")
                .containsKey("scope")
                .containsKey("scp");
    }

    @Test
    void extractsRealmAccessRolesWhenRolesClaimIsAbsent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("bob")
                .claim("realm_access", Map.of("roles", List.of("REALM_ADMIN")))
                .build();

        McpToolCallContext context = extract(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(context.principalName()).isEqualTo("bob");
        assertThat(context.principalRoles()).containsExactly("REALM_ADMIN");
    }

    @Test
    void fallsBackToGroupsClaimWhenOtherRoleClaimsAreAbsent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("carol")
                .claim("groups", List.of("support", "operations"))
                .build();

        McpToolCallContext context = extract(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(context.principalRoles()).containsExactly("support", "operations");
    }

    private static McpToolCallContext extract(Principal principal) {
        ServerRequest request = mock(ServerRequest.class);
        when(request.principal()).thenReturn(Optional.of(principal));

        McpTransportContext transportContext = SpringAiMcpTransportContext.extract(request);
        return (McpToolCallContext) transportContext.get(SpringAiMcpTransportContext.CALL_CONTEXT_KEY);
    }
}
