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
package org.apache.camel.component.ai.tool.springboot;

import java.util.Map;
import java.util.function.BiConsumer;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Keeps ai-tool MCP hints and returnDirect unset unless they are explicitly configured.
 * <p>
 * The Camel catalog reports a default of false for Boolean options without a declared default, so the generated
 * {@link AiToolComponentConfiguration} initializes these options to false and the generated customizer would publish
 * them on every tool. This clears them on the configuration class after Spring Boot binds it, leaving values set on a
 * component or by a user customizer untouched.
 * <p>
 * Only needed on the 4.22.x branch: from 4.23 the starter copies only the options the application configured
 * (CAMEL-24501), so these options stay unset without it.
 */
@AutoConfiguration
public class AiToolUnsetHintsAutoConfiguration {

    private static final String PREFIX = "camel.component.ai-tool.";

    private static final Map<String, BiConsumer<AiToolComponentConfiguration, Boolean>> OPTIONS = Map.of(
            "read-only-hint", AiToolComponentConfiguration::setReadOnlyHint,
            "destructive-hint", AiToolComponentConfiguration::setDestructiveHint,
            "idempotent-hint", AiToolComponentConfiguration::setIdempotentHint,
            "open-world-hint", AiToolComponentConfiguration::setOpenWorldHint,
            "return-direct", AiToolComponentConfiguration::setReturnDirect);

    @Bean
    static BeanPostProcessor clearUnsetAiToolHints(Environment environment) {
        Binder binder = Binder.get(environment);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof AiToolComponentConfiguration configuration) {
                    OPTIONS.forEach((option, setter) -> {
                        if (!binder.bind(PREFIX + option, Boolean.class).isBound()) {
                            setter.accept(configuration, null);
                        }
                    });
                }
                return bean;
            }
        };
    }
}
