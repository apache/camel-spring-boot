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
package org.apache.camel.component.cron.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.component.cron.api.CamelCronConfiguration;
import org.apache.camel.component.cron.api.CamelCronService;
import org.apache.camel.component.timer.TimerComponent;

public class DummyCamelCronService implements CamelCronService, CamelContextAware {

    private CamelContext camelContext;

    private String id;

    public DummyCamelCronService() {
        this("dummy");
    }

    public DummyCamelCronService(String id) {
        this.id = id;
    }

    @Override
    public Endpoint createEndpoint(CamelCronConfiguration configuration) throws Exception {
        TimerComponent timerComponent = camelContext.getComponent("timer", TimerComponent.class);
        return timerComponent.createEndpoint("timer:tick?period=1&delay=0");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

}
