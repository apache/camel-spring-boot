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
package org.apache.camel.component.micrometer.springboot;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration(proxyBeanMethods = false)
@Conditional(ConditionalOnCamelContextAndAutoConfigurationBeans.class)
@ConditionalOnProperty(prefix = "camel.metrics", name = "uriTagEnabled", havingValue = "true")
@AutoConfigureAfter({CamelAutoConfiguration.class})
public class MicrometerTagsAutoConfiguration {

    /**
     * To integrate with micrometer to include uri in tags when for example using
     * camel rest-dsl with servlet.
     */
    @Bean
    WebMvcTagsProvider webMvcTagsProvider() {
        return new DefaultWebMvcTagsProvider() {
            @Override
            public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Throwable exception) {
                String uri = request.getServletPath();
                if (uri == null || uri.isEmpty()) {
                    uri = request.getPathInfo();
                } else {
                    String p = request.getPathInfo();
                    if (p != null) {
                        uri = uri + p;
                    }
                }
                return Tags.concat(
                        super.getTags(request, response, handler, exception),
                        Tags.of(Tag.of("uri", uri))
                );
            }
        };
    }
}
