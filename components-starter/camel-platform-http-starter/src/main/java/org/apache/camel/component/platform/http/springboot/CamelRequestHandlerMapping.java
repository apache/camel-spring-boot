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

import org.apache.camel.component.platform.http.HttpEndpointModel;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.PlatformHttpListener;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.util.ReflectionHelper;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CamelRequestHandlerMapping extends RequestMappingHandlerMapping implements PlatformHttpListener {

    private final PlatformHttpComponent component;
    private final PlatformHttpEngine engine;
    private final Map<String, RequestMappingInfo> mappings = new HashMap<>();

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
        return new String[]{};
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
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
        ServletRequestPathUtils.parseAndCache(request);
        return super.getHandlerInternal(request);
    }

    @Override
    public void registerHttpEndpoint(HttpEndpointModel model) {
        RequestMappingInfo info = asRequestMappingInfo(model);
        Method m = ReflectionHelper.findMethod(SpringBootPlatformHttpConsumer.class, "service", HttpServletRequest.class, HttpServletResponse.class);
        registerMapping(info, model.getConsumer(), m);
    }

    @Override
    public void unregisterHttpEndpoint(HttpEndpointModel model) {
        // noop
    }

    private RequestMappingInfo asRequestMappingInfo(HttpEndpointModel model) {
        // allowed methods from model or endpoint
        List<RequestMethod> methods = new ArrayList<>();
        String verbs = model.getVerbs();
        if (verbs == null && model.getConsumer() != null) {
            PlatformHttpEndpoint endpoint = (PlatformHttpEndpoint) model.getConsumer().getEndpoint();
            verbs = endpoint.getHttpMethodRestrict();
        }
        if (verbs != null) {
            for (String v : model.getVerbs().split(",")) {
                RequestMethod rm = RequestMethod.valueOf(v);
                methods.add(rm);
            }
        }

        RequestMappingInfo info = RequestMappingInfo
                .paths(model.getUri())
                .methods(methods.toArray(new RequestMethod[0]))
                .options(this.getBuilderConfiguration()).build();
        return info;
    }

}
