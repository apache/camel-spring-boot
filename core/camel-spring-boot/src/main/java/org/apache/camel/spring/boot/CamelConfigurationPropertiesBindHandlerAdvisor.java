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
package org.apache.camel.spring.boot;

import java.util.Collection;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;

/**
 * Stops Spring Boot from binding into the internals of third party types, such as a Jackson <tt>ObjectMapper</tt>,
 * held by the options of the Camel configuration classes, unless the application configured a property below them.
 * <p/>
 * Spring Boot binds a nested JavaBean only when a property source has a property below it. A property source that
 * cannot list its property names, such as one that looks properties up in a remote store, cannot tell, so Spring Boot
 * binds into every nested JavaBean in case it has a property below it. For an option of a third party type that walks
 * into the internals of that type, which may fail, and then the application fails to start although it configured
 * nothing there.
 * <p/>
 * The options of the Camel configuration classes are still bound when the application configures them, whether as a
 * value, such as a <tt>#bean:myObjectMapper</tt> reference, or as properties below them in a property source that can
 * list its property names. The options of Camel's own types, such as the nested configuration of a component, are
 * bound as before.
 */
public class CamelConfigurationPropertiesBindHandlerAdvisor implements ConfigurationPropertiesBindHandlerAdvisor {

    private static final ConfigurationPropertyName CAMEL = ConfigurationPropertyName.of("camel");

    @Override
    public BindHandler apply(BindHandler bindHandler) {
        return new AbstractBindHandler(bindHandler) {
            @Override
            public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
                if (context.getDepth() > 0 && CAMEL.isAncestorOf(name) && isThirdPartyBean(target)
                        && !isConfigured(name, context)) {
                    return null;
                }
                return super.onStart(name, target, context);
            }
        };
    }

    private static boolean isThirdPartyBean(Bindable<?> target) {
        Class<?> type = target.getType().resolve(Object.class);
        if (type.isPrimitive() || type.isArray() || type.isEnum() || Map.class.isAssignableFrom(type)
                || Collection.class.isAssignableFrom(type)) {
            return false;
        }
        String name = type.getName();
        return !name.startsWith("java.") && !name.startsWith("org.apache.camel.");
    }

    /**
     * Whether a property source has a value for the option, or a property below it for certain.
     */
    private static boolean isConfigured(ConfigurationPropertyName name, BindContext context) {
        for (ConfigurationPropertySource source : context.getSources()) {
            if (source.getConfigurationProperty(name) != null
                    || source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
                return true;
            }
        }
        return false;
    }
}
