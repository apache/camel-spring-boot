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

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Controls whether Camel replaces Spring AI's default Streamable HTTP transport.
 * By default, replacement is enabled only when Camel MCP tags are configured.
 * The explicit transport-enabled property overrides that default.
 */
public final class CamelMcpServerTransportCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean explicitlyEnabled = Binder.get(context.getEnvironment())
                .bind("camel.mcp-server.transport-enabled", Boolean.class)
                .orElse(null);
        if (explicitlyEnabled != null) {
            return explicitlyEnabled;
        }

        return StringUtils.hasText(context.getEnvironment().getProperty("camel.mcp-server.tags"));
    }
}
