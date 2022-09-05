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

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.component.servlet.ServletComponent;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(type = "org.apache.camel.component.servlet.springboot.ServletComponentAutoConfiguration")
@AutoConfigureAfter(name = {
		"org.apache.camel.component.servlet.springboot.ServletComponentAutoConfiguration",
		"org.apache.camel.component.servlet.springboot.ServletComponentConverter"})
public class ServletPlatformHttpAutoConfiguration {

	private final CamelContext camelContext;

	public ServletPlatformHttpAutoConfiguration(
			CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	@Lazy
	@Bean(name = "platform-http-engine")
	@ConditionalOnMissingBean(PlatformHttpEngine.class)
	@DependsOn("configureServletComponent")
	public PlatformHttpEngine servletPlatformHttpEngine() {
		return new ServletPlatformHttpEngine((ServletComponent) camelContext.getComponent("servlet"));
	}
}
