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

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Suspendable;
import org.apache.camel.SuspendableService;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SpringBootPlatformHttpConsumer extends DefaultConsumer implements Suspendable, SuspendableService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootPlatformHttpConsumer.class);

    private final DefaultHttpBinding binding;

    public SpringBootPlatformHttpConsumer(PlatformHttpEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.binding = new SpringBootPlatformHttpBinding();
        this.binding.setHeaderFilterStrategy(endpoint.getHeaderFilterStrategy());
        this.binding.setMuteException(endpoint.isMuteException());
        this.binding.setFileNameExtWhitelist(endpoint.getFileNameExtWhitelist());
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
            LOG.warn("Error handling request due to: " + e.getMessage(), e);
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
            afterProcess(response, exchange, true);
        }
    }

    protected void afterProcess(HttpServletResponse response, Exchange exchange, boolean rethrow) throws IOException, ServletException {
        try {
            // now lets output to the res
            if (LOG.isTraceEnabled()) {
                LOG.trace("Writing res for exchangeId: {}", exchange.getExchangeId());
            }
            binding.writeResponse(exchange, response);
        } catch (IOException e) {
            LOG.error("Error processing request", e);
            if (rethrow) {
                throw e;
            } else {
                exchange.setException(e);
            }
        } catch (Exception e) {
            LOG.error("Error processing request", e);
            if (rethrow) {
                throw new ServletException(e);
            } else {
                exchange.setException(e);
            }
        } finally {
            doneUoW(exchange);
            releaseExchange(exchange, false);
        }
    }

}
