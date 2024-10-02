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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Suspendable;
import org.apache.camel.SuspendableService;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.http.common.HttpBinding;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringBootPlatformHttpConsumer extends DefaultConsumer implements Suspendable, SuspendableService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootPlatformHttpConsumer.class);

    private HttpBinding binding;
    private final boolean handleWriteResponseError;

    public SpringBootPlatformHttpConsumer(PlatformHttpEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.binding = new SpringBootPlatformHttpBinding();
        this.binding.setHeaderFilterStrategy(endpoint.getHeaderFilterStrategy());
        this.binding.setMuteException(endpoint.isMuteException());
        this.binding.setFileNameExtWhitelist(endpoint.getFileNameExtWhitelist());
        this.handleWriteResponseError = endpoint.isHandleWriteResponseError();
    }

    /**
     * Used for testing purposes
     */
    void setBinding(HttpBinding binding) {
        this.binding = binding;
    }

    @Override
    public PlatformHttpEndpoint getEndpoint() {
        return (PlatformHttpEndpoint) super.getEndpoint();
    }


    /**
     * This method is invoked by Spring Boot when invoking Camel via platform-http
     */
    public void service(HttpServletRequest request, HttpServletResponse response) {
        LOG.trace("Service: {}", request);
        try {
            handleService(request, response);
        } catch (Exception e) {
            // do not leak exception back to caller
            LOG.warn("Error handling request due to: {}", e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e1) {
                // ignore
            }
        }
    }

    protected void handleService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (isSuspended()) {
            LOG.debug("Consumer suspended, cannot service request: {}", request);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        Exchange exchange = createExchange(true);
        exchange.setPattern(ExchangePattern.InOut);
        HttpHelper.setCharsetFromContentType(request.getContentType(), exchange);
        exchange.setIn(new PlatformHttpMessage(exchange, binding, request, response));
        String contextPath = getEndpoint().getPath();
        exchange.getIn().setHeader(SpringBootPlatformHttpConstants.CONTEXT_PATH, contextPath);
        // set context path as header
        String httpPath = (String) exchange.getIn().getHeader(Exchange.HTTP_PATH);
        // here we just remove the CamelServletContextPath part from the HTTP_PATH
        if (contextPath != null
            && httpPath.startsWith(contextPath)) {
            exchange.getIn().setHeader(Exchange.HTTP_PATH,
                    httpPath.substring(contextPath.length()));
        }

        // TODO: async with CompletionStage returned to spring boot?

        // we want to handle the UoW
        try {
            createUoW(exchange);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing request for exchangeId: {}", exchange.getExchangeId());
        }
        try {
            getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        } finally {
            afterProcess(response, exchange);
        }
    }

    protected void afterProcess(HttpServletResponse response, Exchange exchange) throws Exception {
        boolean writeFailure = false;
        try {
            // now lets output to the res
            if (LOG.isTraceEnabled()) {
                LOG.trace("Writing res for exchangeId: {}", exchange.getExchangeId());
            }
            binding.writeResponse(exchange, response);
        } catch (Exception e) {
            writeFailure = true;
            handleFailure(exchange, e);
        } finally {
            doneUoW(exchange);
            releaseExchange(exchange, false);
        }
        try {
            if (writeFailure && !response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleFailure(Exchange exchange, Throwable failure) {
        getExceptionHandler().handleException(
                "Failed writing HTTP response url: " + getEndpoint().getPath() + " due to: " + failure.getMessage(),
                failure);
        if (handleWriteResponseError) {
            Exception existing = exchange.getException();
            if (existing != null) {
                failure.addSuppressed(existing);
            }
            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, failure);
            exchange.setException(failure);
        }
    }

}
