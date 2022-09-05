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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.component.servlet.ServletComponent;
import org.apache.camel.component.servlet.ServletConsumer;
import org.apache.camel.component.servlet.ServletEndpoint;

public class ServletPlatformHttpEngine implements PlatformHttpEngine {

	private final ServletComponent servletComponent;

	public ServletPlatformHttpEngine(ServletComponent servletComponent) {
		this.servletComponent = servletComponent;
	}

	@Override
	public Consumer createConsumer(PlatformHttpEndpoint platformHttpEndpoint, Processor processor) {
		try {
			return new ServletConsumer((ServletEndpoint) servletComponent.createEndpoint(platformHttpEndpoint.getEndpointUri()), processor);
		} catch (Exception e) {
			throw new IllegalArgumentException("The following  endpoint uri " + platformHttpEndpoint.getEndpointUri() + " is not supported", e);
		}
	}

	@Override
	public int getServerPort() {
		return PlatformHttpEngine.super.getServerPort();
	}
}
