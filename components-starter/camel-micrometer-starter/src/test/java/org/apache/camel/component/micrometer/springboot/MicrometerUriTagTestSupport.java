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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Base class for the tests capturing the uri tag of the {@code http.server.requests} meters.
 */
public abstract class MicrometerUriTagTestSupport {

    protected static final String HTTP_SERVER_REQUESTS = "http.server.requests";
    protected static final String URI_TAG = "uri";

    @Autowired
    protected Environment env;

    @Autowired
    protected MeterRegistry meterRegistry;

    /**
     * Performs a HTTP GET on the given path, and returns the http status code.
     */
    protected int get(String path) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + env.getRequiredProperty("local.server.port") + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    /**
     * The uri tag values of the captured http server request meters, and how many requests each of them counted.
     */
    protected Map<String, Long> uriTags() {
        Map<String, Long> answer = new LinkedHashMap<>();
        for (Timer timer : meterRegistry.find(HTTP_SERVER_REQUESTS).timers()) {
            answer.merge(timer.getId().getTag(URI_TAG), timer.count(), Long::sum);
        }
        return answer;
    }

    /**
     * Number of requests counted for the given uri tag value.
     */
    protected long count(String uri) {
        Timer timer = meterRegistry.find(HTTP_SERVER_REQUESTS).tag(URI_TAG, uri).timer();
        return timer != null ? timer.count() : 0;
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("servlet:/users/{id}")
                            .setBody().constant("Hello");
                }
            };
        }
    }
}
