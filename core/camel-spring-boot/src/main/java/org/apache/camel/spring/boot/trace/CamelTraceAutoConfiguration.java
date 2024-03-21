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
package org.apache.camel.spring.boot.trace;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.BacklogTracer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelTraceConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class CamelTraceAutoConfiguration {

    @Bean
    public BacklogTracer backlogTracer(CamelContext camelContext, CamelTraceConfigurationProperties config)
            throws Exception {
        if (!config.isEnabled() && !config.isStandby()) {
            return null;
        }

        // must enable source location so tracer tooling knows to map breakpoints to source code
        camelContext.setSourceLocationEnabled(true);

        // enable tracer on camel
        camelContext.setBacklogTracing(config.isEnabled());
        camelContext.setBacklogTracingStandby(config.isStandby());
        camelContext.setBacklogTracingTemplates(config.isTraceTemplates());

        BacklogTracer tracer = org.apache.camel.impl.debugger.BacklogTracer.createTracer(camelContext);
        tracer.setEnabled(config.isEnabled());
        tracer.setStandby(config.isStandby());
        tracer.setBacklogSize(config.getBacklogSize());
        tracer.setRemoveOnDump(config.isRemoveOnDump());
        tracer.setBodyMaxChars(config.getBodyMaxChars());
        tracer.setBodyIncludeStreams(config.isBodyIncludeStreams());
        tracer.setBodyIncludeFiles(config.isBodyIncludeFiles());
        tracer.setIncludeExchangeProperties(config.isIncludeExchangeProperties());
        tracer.setIncludeExchangeVariables(config.isIncludeExchangeVariables());
        tracer.setIncludeException(config.isIncludeException());
        tracer.setTraceRests(config.isTraceRests());
        tracer.setTraceTemplates(config.isTraceTemplates());
        tracer.setTracePattern(config.getTracePattern());
        tracer.setTraceFilter(config.getTraceFilter());

        camelContext.getCamelContextExtension().addContextPlugin(BacklogTracer.class, tracer);
        camelContext.addService(tracer);

        return tracer;
    }

}
