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
package org.apache.camel.springboot.cli.connector;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

/**
 * Matches when the Spring WebSocket client can be used: spring-websocket, and a Jakarta WebSocket implementation (such
 * as Tomcat's), not only the Jakarta WebSocket API, which some applications have without an implementation. Not when
 * <tt>camel.cli.websocket.client=jdk</tt> asks for the JDK client.
 */
class OnSpringWebSocketClientCondition extends SpringBootCondition {

    static final String CLIENT_CLASS = "org.springframework.web.socket.client.standard.StandardWebSocketClient";
    static final String PROVIDER_CLASS = "jakarta.websocket.ContainerProvider";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if ("jdk".equalsIgnoreCase(context.getEnvironment().getProperty("camel.cli.websocket.client"))) {
            return ConditionOutcome.noMatch("camel.cli.websocket.client=jdk");
        }
        String missing = missing(context.getClassLoader());
        return missing == null
                ? ConditionOutcome.match("Spring WebSocket client and a Jakarta WebSocket implementation found")
                : ConditionOutcome.noMatch(missing);
    }

    /**
     * Whether the Spring WebSocket client can be used.
     */
    static boolean isAvailable(ClassLoader classLoader) {
        return missing(classLoader) == null;
    }

    private static String missing(ClassLoader classLoader) {
        if (!ClassUtils.isPresent(CLIENT_CLASS, classLoader)) {
            return "spring-websocket not found";
        }
        if (!ClassUtils.isPresent(PROVIDER_CLASS, classLoader)) {
            return "Jakarta WebSocket API not found";
        }
        // as ContainerProvider.getWebSocketContainer() looks it up, without creating it
        try {
            Class<?> provider = ClassUtils.forName(PROVIDER_CLASS, classLoader);
            if (ServiceLoader.load(provider, classLoader).stream().findFirst().isEmpty()) {
                return "no Jakarta WebSocket implementation (" + PROVIDER_CLASS + " service) found";
            }
        } catch (ClassNotFoundException | ServiceConfigurationError | LinkageError e) {
            return "no usable Jakarta WebSocket implementation: " + e;
        }
        return null;
    }
}
