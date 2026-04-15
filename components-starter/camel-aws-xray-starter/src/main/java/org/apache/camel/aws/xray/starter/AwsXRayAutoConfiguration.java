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
package org.apache.camel.aws.xray.starter;

import org.apache.camel.CamelContext;
import org.apache.camel.component.aws.xray.NoopTracingStrategy;
import org.apache.camel.component.aws.xray.TraceAnnotatedTracingStrategy;
import org.apache.camel.component.aws.xray.XRayTracer;
import org.apache.camel.spi.InterceptStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(AwsXRayConfigurationProperties.class)
@ConditionalOnProperty(value = "camel.aws-xray.enabled", matchIfMissing = true)
public class AwsXRayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(XRayTracer.class)
    XRayTracer awsTracer(CamelContext context,
            AwsXRayConfigurationProperties config,
            @CamelAwsXRayTracingStrategy ObjectProvider<InterceptStrategy> xRayStrategyProvider) {
        XRayTracer tracer = new XRayTracer();
        context.setTracing(true);
        tracer.setCamelContext(context);
        InterceptStrategy xRayStrategy = xRayStrategyProvider.getIfAvailable();
        if (xRayStrategy != null) {
            tracer.setTracingStrategy(xRayStrategy);
        } else if (config.getTracingStrategy() == AwsXRayConfigurationProperties.TracingStrategy.NOOP) {
            tracer.setTracingStrategy(new NoopTracingStrategy());
        } else {
            // by default let's use TraceAnnotatedTracingStrategy
            tracer.setTracingStrategy(new TraceAnnotatedTracingStrategy());
        }
        tracer.setExcludePatterns(config.getExcludePatterns());
        tracer.init(context);
        return tracer;
    }
}
