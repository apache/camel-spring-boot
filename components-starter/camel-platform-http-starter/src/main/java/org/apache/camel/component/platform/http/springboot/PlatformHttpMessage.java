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
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.http.common.HttpBinding;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.util.ObjectHelper;

import java.io.IOException;

public class PlatformHttpMessage extends DefaultMessage {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpBinding binding;
    private boolean requestRead;

    public PlatformHttpMessage(Exchange exchange, HttpBinding binding, HttpServletRequest request, HttpServletResponse response) {
        super(exchange);
        this.init(exchange, binding, request, response);
    }

    private PlatformHttpMessage(HttpServletRequest request, HttpServletResponse response, Exchange exchange, HttpBinding binding, boolean requestRead) {
        super(exchange);
        this.request = request;
        this.response = response;
        this.binding = binding;
        this.requestRead = requestRead;
    }

    public void init(Exchange exchange, HttpBinding binding, HttpServletRequest request, HttpServletResponse response) {
        this.setExchange(exchange);
        this.requestRead = false;
        this.binding = binding;
        this.request = request;
        this.response = response;
        this.setHeader("CamelHttpServletRequest", request);
        this.setHeader("CamelHttpServletResponse", response);
        Boolean flag = (Boolean)exchange.getProperty("CamelSkipWwwFormUrlEncoding", Boolean.class);
        if (flag != null && flag) {
            this.setHeader("CamelSkipWwwFormUrlEncoding", Boolean.TRUE);
        }

        binding.readRequest(request, this);
    }

    public void reset() {
        super.reset();
        this.request = null;
        this.response = null;
        this.binding = null;
        this.requestRead = false;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    protected Object createBody() {
        if (this.requestRead) {
            return null;
        } else {
            Object body;
            try {
                body = this.binding.parseBody(request, this);
            } catch (IOException var5) {
                throw new RuntimeCamelException(var5);
            } finally {
                this.requestRead = true;
            }

            return body;
        }
    }

    public PlatformHttpMessage newInstance() {
        PlatformHttpMessage answer = new PlatformHttpMessage(this.request, this.response, this.getExchange(), this.binding, this.requestRead);
        if (answer.camelContext == null) {
            answer.setCamelContext(this.camelContext);
        }
        return answer;
    }

    public String toString() {
        return "PlatformHttpMessage@" + ObjectHelper.getIdentityHashCode(this);
    }


}
