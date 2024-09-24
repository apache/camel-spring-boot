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
package org.apache.camel.component.jolokia.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@ConfigurationProperties(prefix = "camel.component.jolokia")
public class JolokiaComponentConfiguration {

	/**
	 *
	 * Enable the component, default true.
	 *
	 */
	private boolean enabled = true;

	/**
	 *
	 * If set to true, Spring’s application context is searched for additional beans of org.jolokia.support.spring.SpringJolokiaConfigHolder class
	 * that are used to configure the agent, default false.
	 *
	 * @see <a href="https://jolokia.org/reference/html/manual/spring.html">Support for Spring Framework in Jolokia</a>
	 *
	 */
	private boolean lookupConfig = false;

	/**
	 *
	 * If set to true, Spring’s application context is searched for additional beans of org.jolokia.server.core.service.api.JolokiaService.
	 * These beans are added to Jolokia internal context as services used by the Agent, default false.
	 *
	 * @see <a href="https://jolokia.org/reference/html/manual/spring.html">Support for Spring Framework in Jolokia</a>
	 *
	 */
	private boolean lookupServices = false;

	/**
	 *
	 * Specifies how system properties with jolokia. prefix (the prefix is then stripped) affect Jolokia Agent configuration, default 'never'.
	 * There are three modes available:
	 *
	 * <ul>
	 * <li>never - No lookup is done on system properties as all. This is the default mode.</li>
	 *
	 * <li>fallback - System properties with a prefix jolokia. are used as fallback configuration values if not specified locally in the Spring application context.
	 * E.g. jolokia.port=8888 will change the port on which the agent is listening to 8888 if the port is not explicitly specified in the configuration.</li>
	 *
	 * <li>override - System properties with a prefix jolokia. are used as configuration values even if they are specified locally in the Spring application context.
	 * E.g. jolokia.port=8888 will change the port on which the agent is listening to 8888 in any case.</li>
	 * </ul>
	 * @see <a href="https://jolokia.org/reference/html/manual/spring.html">Support for Spring Framework in Jolokia</a>
	 *
	 */
	private String systemPropertiesMode = "never";

	/**
	 *
	 * If set to true, additional org.jolokia.support.spring.backend.SpringRequestHandler is added to the agent,
	 * so we can invoke Spring bean operations using Jolokia protocol, default false.
	 *
	 * @see <a href="https://jolokia.org/reference/html/manual/spring.html">Support for Spring Framework in Jolokia</a>
	 *
	 */
	private boolean exposeApplicationContext = false;

	/**
	 *
	 * Configuration for the exposed endpoint.
	 * Example:
	 * <pre>
	 * 	camel.component.jolokia.serverConfig.discoveryEnabled=true
	 * </pre>
	 * @see <a href="https://jolokia.org/reference/html/manual/agents.html#jvm-agent">JVM agent configuration options</a>
	 *
	 */
	private Map<String, String> serverConfig = new HashMap<>();

	/**
	 *
	 * To set default properties to make the jolokia enpoint work on k8s/OCP, default true.
	 *
	 * It sets:
	 * <ul>
	 * <li>protocol = https</li>
	 * <li>caCert = /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt</li>
	 * <li>useSslClientAuthentication = true</li>
	 * </ul>
	 */
	private boolean kubernetesDiscover = true;

	/**
	 *
	 * To prefer the default CA file, default true.
	 * <pre>
	 * /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
	 * </pre>
	 */
	private boolean kubernetesUseDefaultCa = true;

	/**
	 *
	 * All operations on MBeans are allowed by default, the Camel restrictor allows only operations on Camel domain, default true.
	 * If key 'restrictorClass' in serverConfig has been provided, this property will be ignorated.
	 * <pre>
	 * org.apache.camel.component.jolokia.springboot.restrictor.CamelRestrictor
	 * </pre>
	 */
	private boolean useCamelRestrictor = true;

	/**
	 *
	 * In case of custom bean configuration (it is necessary to provide a bean named 'camelConfigHolder' of type SpringJolokiaConfigHolder)
	 * containing the same keys provided by the configuration (application.properties),
	 * it prefers values from configuration on values from bean, default true.
	 *
	 */
	private boolean configFromPropertiesFirst = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isLookupConfig() {
		return lookupConfig;
	}

	public void setLookupConfig(final boolean lookupConfig) {
		this.lookupConfig = lookupConfig;
	}

	public boolean isLookupServices() {
		return lookupServices;
	}

	public void setLookupServices(final boolean lookupServices) {
		this.lookupServices = lookupServices;
	}

	public String getSystemPropertiesMode() {
		return systemPropertiesMode;
	}

	public void setSystemPropertiesMode(final String systemPropertiesMode) {
		this.systemPropertiesMode = systemPropertiesMode;
	}

	public boolean isExposeApplicationContext() {
		return exposeApplicationContext;
	}

	public void setExposeApplicationContext(final boolean exposeApplicationContext) {
		this.exposeApplicationContext = exposeApplicationContext;
	}

	public Map<String, String> getServerConfig() {
		return serverConfig;
	}

	public void setServerConfig(final Map<String, String> serverConfig) {
		this.serverConfig = serverConfig;
	}

	public boolean isKubernetesDiscover() {
		return kubernetesDiscover;
	}

	public void setKubernetesDiscover(final boolean kubernetesDiscover) {
		this.kubernetesDiscover = kubernetesDiscover;
	}

	public boolean isKubernetesUseDefaultCa() {
		return kubernetesUseDefaultCa;
	}

	public void setKubernetesUseDefaultCa(final boolean kubernetesUseDefaultCa) {
		this.kubernetesUseDefaultCa = kubernetesUseDefaultCa;
	}

	public boolean isUseCamelRestrictor() {
		return useCamelRestrictor;
	}

	public void setUseCamelRestrictor(final boolean useCamelRestrictor) {
		this.useCamelRestrictor = useCamelRestrictor;
	}

	public boolean isConfigFromPropertiesFirst() {
		return configFromPropertiesFirst;
	}

	public void setConfigFromPropertiesFirst(final boolean configFromPropertiesFirst) {
		this.configFromPropertiesFirst = configFromPropertiesFirst;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", JolokiaComponentConfiguration.class.getSimpleName() + "[", "]")
				.add("enabled=" + enabled)
				.add("lookupConfig=" + lookupConfig)
				.add("lookupServices=" + lookupServices)
				.add("systemPropertiesMode='" + systemPropertiesMode + "'")
				.add("exposeApplicationContext=" + exposeApplicationContext)
				.add("serverConfig=" + serverConfig)
				.add("kubernetesDiscover=" + kubernetesDiscover)
				.add("kubernetesUseDefaultCa=" + kubernetesUseDefaultCa)
				.add("useCamelRestrictor=" + useCamelRestrictor)
				.add("configFromPropertiesFirst=" + configFromPropertiesFirst)
				.toString();
	}
}
