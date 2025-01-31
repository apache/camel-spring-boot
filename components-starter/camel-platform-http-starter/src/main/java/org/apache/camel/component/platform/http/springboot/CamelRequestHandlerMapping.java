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
package org.apache.camel.component.platform.http.springboot;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.component.platform.http.HttpEndpointModel;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.PlatformHttpListener;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.util.ReflectionHelper;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CamelRequestHandlerMapping extends RequestMappingHandlerMapping implements PlatformHttpListener {

    private final PlatformHttpComponent component;
    private final PlatformHttpEngine engine;

    private CorsConfiguration corsConfiguration;

    public CamelRequestHandlerMapping(PlatformHttpComponent component, PlatformHttpEngine engine) {
        this.component = component;
        this.engine = engine;
        this.component.addPlatformHttpListener(this);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    protected String[] getCandidateBeanNames() {
        // no candidates
        return new String[] {};
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return false;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        // not in use as we do not use class scanning but Camel platform-http component
        return null;
    }

    @Override
    protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
        RestConfiguration restConfiguration = component.getCamelContext().getRestConfiguration();

        if (!restConfiguration.isEnableCORS()) {
            // CORS disabled for camel
            return null;
        }

        if (corsConfiguration == null) {
            Map<String, String> corsHeaders = restConfiguration.getCorsHeaders();
            corsConfiguration = createCorsConfiguration(corsHeaders != null ? corsHeaders : Collections.emptyMap());
        }
        return corsConfiguration;
    }

    @Override
    protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
        return super.getCorsConfiguration(handler, request);
    }

    private CorsConfiguration createCorsConfiguration(Map<String, String> corsHeaders) {
        CorsConfiguration config = new CorsConfiguration();

        String allowedOrigin = corsHeaders.get("Access-Control-Allow-Origin");
        config.addAllowedOrigin(allowedOrigin != null ? allowedOrigin : RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_ORIGIN);

        String allowMethodsStr = corsHeaders.get("Access-Control-Allow-Methods");
        allowMethodsStr = allowMethodsStr != null ? allowMethodsStr : RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_METHODS;
        for (String allowMethod : allowMethodsStr.split(",")) {
            config.addAllowedMethod(allowMethod.trim());
        }

        String allowHeadersStr = corsHeaders.get("Access-Control-Allow-Headers");
        allowHeadersStr = allowHeadersStr != null ? allowHeadersStr : RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_HEADERS;
        for (String allowHeader : allowHeadersStr.split(",")) {
            config.addAllowedHeader(allowHeader.trim());
        }

        String maxAgeStr = corsHeaders.get("Access-Control-Max-Age");
        Long maxAge = maxAgeStr != null ? Long.parseLong(maxAgeStr) : Long.parseLong(RestConfiguration.CORS_ACCESS_CONTROL_MAX_AGE);
        config.setMaxAge(maxAge);

        String allowCredentials = corsHeaders.get("Access-Control-Allow-Credentials");
        config.setAllowCredentials(Boolean.parseBoolean(allowCredentials));

        return config;
    }

    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
        ServletRequestPathUtils.parseAndCache(request);
        return super.getHandlerInternal(request);
    }

    @Override
    public void registerHttpEndpoint(HttpEndpointModel model) {
        List<RequestMappingInfo> requestMappingInfos = asRequestMappingInfo(model);
        Method m = ReflectionHelper.findMethod(SpringBootPlatformHttpConsumer.class, "service",
                HttpServletRequest.class, HttpServletResponse.class);
        for (RequestMappingInfo info : requestMappingInfos) {
            // Needed in case of context reload
            unregisterMapping(info);
            registerMapping(info, model.getConsumer(), m);
        }
    }

    @Override
    public void unregisterHttpEndpoint(HttpEndpointModel model) {
        // noop
    }

    private List<RequestMappingInfo> asRequestMappingInfo(HttpEndpointModel model) {
        List<RequestMappingInfo> result = new ArrayList<>();

        // allowed methods from model or endpoint
        List<RequestMethod> methods = new ArrayList<>();
        String verbs = model.getVerbs();
        if (verbs == null && model.getConsumer() != null) {
            PlatformHttpEndpoint endpoint = (PlatformHttpEndpoint) model.getConsumer().getEndpoint();
            verbs = endpoint.getHttpMethodRestrict();
            if (verbs == null) {
                Collections.addAll(methods, RequestMethod.values());
            }
        }
        if (verbs != null) {
            for (String v : model.getVerbs().split(",")) {
                RequestMethod rm = RequestMethod.resolve(v);
                methods.add(rm);
            }
        }

        if (component.getCamelContext().getRestConfiguration().isEnableCORS()) {
            // when CORS is enabled Camel adds OPTIONS by default in httpMethodsRestrict
            // which causes multiple registration of OPTIONS endpoints in spring.
            // this causes issues when unregistering endpoints because others share
            // the same handler (the consumer) so they get unregistered as well.
            // removing the OPTIONS mappings maintains the intended behavior
            // of this verb while avoiding this issue.
            methods.remove(RequestMethod.OPTIONS);
        }

        for (RequestMethod rm : methods) {
            createRequestMappingInfo(model, rm, result);
        }

        return result;
    }

    private void createRequestMappingInfo(HttpEndpointModel model, RequestMethod rm, List<RequestMappingInfo> result) {
        RequestMethod[] methods = new RequestMethod[]{};
        if (rm != null) {
            methods = new RequestMethod[]{rm};
        }

        SpringBootPlatformHttpConsumer consumer = (SpringBootPlatformHttpConsumer) model.getConsumer();

        String uri = model.getUri();
        if (consumer.getEndpoint().isMatchOnUriPrefix()) {
            // rewrite the uri so that PathPattern is used
            uri += uri.endsWith("/") ? "" : "/";
            uri += "{*matchOnUriPrefix}";
        }

        RequestMappingInfo.Builder info = RequestMappingInfo
                .paths(uri)
                .methods(methods)
                .options(this.getBuilderConfiguration());

        if (model.getConsumes() != null
                && (RequestMethod.POST.name().equals(rm.name()) || RequestMethod.PUT.name().equals(rm.name()) || RequestMethod.PATCH.name().equals(rm.name()))) {
            info.consumes(model.getConsumes().split(","));
        }
        if (model.getProduces() != null) {
            info.produces(model.getProduces().split(","));
        }

        result.add(info.build());
    }

}
