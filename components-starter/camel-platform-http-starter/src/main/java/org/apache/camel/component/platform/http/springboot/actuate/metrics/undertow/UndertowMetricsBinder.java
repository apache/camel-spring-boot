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
package org.apache.camel.component.platform.http.springboot.actuate.metrics.undertow;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.xnio.XnioWorker;

import java.util.Collections;

public class UndertowMetricsBinder implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

    private final MeterRegistry meterRegistry;
    private final Iterable<Tag> tags;
    private volatile UndertowMetrics undertowMetrics;

    public UndertowMetricsBinder(MeterRegistry meterRegistry) {
        this(meterRegistry, Collections.emptyList());
    }

    public UndertowMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
        this.meterRegistry = meterRegistry;
        this.tags = tags;
    }

    @Override
    public void destroy() {
        if (this.undertowMetrics != null) {
            this.undertowMetrics.close();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        UndertowComponents undertowComponents = findUndertowComponents(applicationContext);
        if (undertowComponents != null) {
            this.undertowMetrics = new UndertowMetrics(undertowComponents.xnioWorker(), undertowComponents.sessionManager(),
                    undertowComponents.deployment(), tags);
            this.undertowMetrics.bindTo(this.meterRegistry);
        }
    }

    private UndertowComponents findUndertowComponents(ApplicationContext applicationContext) {
        if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
            WebServer webServer = webServerApplicationContext.getWebServer();
            if (webServer instanceof UndertowServletWebServer undertowServletWebServer) {
                Undertow undertow = undertowServletWebServer.getUndertow();
                XnioWorker xnioWorker = undertowServletWebServer.getUndertow().getWorker();
                DeploymentManager deploymentManager = undertowServletWebServer.getDeploymentManager();

                return new UndertowComponents(
                        xnioWorker,
                        deploymentManager.getDeployment().getSessionManager(),
                        deploymentManager.getDeployment(),
                        undertow
                );
            }
        }
        return null;
    }

}
