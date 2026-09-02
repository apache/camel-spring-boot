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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.component.micrometer.springboot.metrics.CamelMetricsConfiguration;
import org.apache.camel.http.common.CamelServlet;
import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import java.util.Optional;

@AutoConfiguration(after = CamelAutoConfiguration.class)
@Conditional({ ConditionalOnCamelContextAndAutoConfigurationBeans.class })
@ConditionalOnProperty(prefix = "camel.metrics", name = "uri-tag-enabled", havingValue = "true")
public class MicrometerTagsAutoConfiguration {

    /**
     * Name of the low cardinality key holding the http uri.
     */
    private static final String URI = "uri";

    /**
     * Maximum length of the uri tag value when using dynamic uri tags, to keep the tag value bounded.
     */
    private static final int MAX_URI_LENGTH = 200;

    /**
     * To integrate with micrometer to include expanded uri in tags when for example using camel rest-dsl with servlet.
     */
    @Bean
    ServerRequestObservationConvention serverRequestObservationConvention(Optional<CamelServlet> servlet,
            CamelMetricsConfiguration configuration) {
        return new DefaultServerRequestObservationConvention() {

            @Override
            public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
                // here, we just want to have an additional KeyValue to the observation, keeping the default values
                KeyValue uri = custom(context);
                KeyValues answer = super.getLowCardinalityKeyValues(context);
                // when the request is not for a camel consumer, then we keep the uri computed by the default
                // spring convention (the mapped pattern, or a constant such as UNKNOWN or NOT_FOUND), instead of
                // the requested path, which would add a new meter for every distinct path being requested
                return uri != null ? answer.and(uri) : answer;
            }

            protected KeyValue custom(ServerRequestObservationContext context) {
                HttpServletRequest request = context.getCarrier();
                if (request == null || servlet.isEmpty()) {
                    return null;
                }
                HttpConsumer consumer = servlet.get().getServletResolveConsumerStrategy().resolve(request,
                        servlet.get().getConsumers());
                if (consumer == null) {
                    // the request is not for a camel consumer, so let the default spring convention resolve the uri
                    return null;
                }

                String uri;
                if (configuration.isUriTagDynamic()) {
                    // dynamic uri with the actual value from the http request, this is opt-in as the uri is dynamic
                    // and therefore leads to a tag value per distinct request path
                    uri = dynamicUri(request);
                } else {
                    // the static path of the camel consumer, such as /users/{id}
                    uri = consumer.getPath();
                }
                if (uri == null || uri.isEmpty()) {
                    return null;
                }

                return KeyValue.of(URI, uri);
            }
        };
    }

    /**
     * The uri from the http request, as requested by the client.
     */
    private static String dynamicUri(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String path = request.getServletPath();
        if (path != null) {
            sb.append(path);
        }
        String info = request.getPathInfo();
        if (info != null) {
            sb.append(info);
        }
        return sanitize(sb.toString());
    }

    /**
     * The dynamic uri is client provided, so keep the tag value bounded in length and free of control characters that
     * the monitoring system may not be able to render.
     */
    private static String sanitize(String uri) {
        String answer = uri.length() > MAX_URI_LENGTH ? uri.substring(0, MAX_URI_LENGTH) : uri;
        StringBuilder sb = new StringBuilder(answer.length());
        for (int i = 0; i < answer.length(); i++) {
            char ch = answer.charAt(i);
            sb.append(Character.isISOControl(ch) ? '_' : ch);
        }
        return sb.toString();
    }
}
