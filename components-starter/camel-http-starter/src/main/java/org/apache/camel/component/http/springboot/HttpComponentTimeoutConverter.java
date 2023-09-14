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
package org.apache.camel.component.http.springboot;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;


@Configuration(proxyBeanMethods = false)
@ConfigurationPropertiesBinding
@Component
public class HttpComponentTimeoutConverter extends HttpComponentConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        Set<ConvertiblePair> answer = new LinkedHashSet<>();
        answer.add(new ConvertiblePair(Integer.class, org.apache.hc.core5.util.Timeout.class));
        answer.add(new ConvertiblePair(Long.class, org.apache.hc.core5.util.Timeout.class));
        answer.add(new ConvertiblePair(String.class, org.apache.hc.core5.util.Timeout.class));
        return answer;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        if (source instanceof Integer value) {
            return Timeout.ofMilliseconds(value.longValue());
        } else if (source instanceof Long value) {
            return Timeout.ofMilliseconds(value);
        } else if (source instanceof String value) {
            if (value.startsWith("#")) {
                return super.convert(source, sourceType, targetType);
            }
            return Timeout.ofMilliseconds(Long.parseLong(value));
        }
        return null;
    }
}
