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

import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.support.spring.SpringJolokiaAgent;
import org.jolokia.support.spring.SpringJolokiaConfigHolder;
import org.jolokia.support.spring.SpringJolokiaLogHandlerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(type = "org.jolokia.support.spring.SpringJolokiaAgent")
@EnableConfigurationProperties(JolokiaComponentConfiguration.class)
@ConditionalOnProperty(name = "camel.component.jolokia.enabled", havingValue = "true", matchIfMissing = true)
public class JolokiaComponentAutoConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(JolokiaComponentAutoConfiguration.class);

	protected static final String DEFAULT_CA_ON_K8S = "/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt";

	@Autowired
	private JolokiaComponentConfiguration configuration;

	@Bean
	public SpringJolokiaAgent jolokia(final SpringJolokiaConfigHolder camelConfigHolder,
			final SpringJolokiaLogHandlerHolder camelLogHandlerHolder) {
		LOG.debug("creating agent instance");

		//in case of custom camelConfigHolder, the properties are merged
		final Map<String, String> mergedMap = mergeConfig(configuration.getServerConfig(), camelConfigHolder.getConfig());
		camelConfigHolder.setConfig(mergedMap);
		configuration.setServerConfig(mergedMap);

		final SpringJolokiaAgent agent = new SpringJolokiaAgent();
		agent.setLookupConfig(configuration.isLookupConfig());
		agent.setLookupServices(configuration.isLookupServices());
		agent.setSystemPropertiesMode(configuration.getSystemPropertiesMode());
		agent.setExposeApplicationContext(configuration.isExposeApplicationContext());
		agent.setConfig(camelConfigHolder);
		agent.setLogHandler(camelLogHandlerHolder);
		printConfiguration();
		return agent;
	}

	@Bean
	@ConditionalOnMissingBean(name = "camelLogHandlerHolder")
	public SpringJolokiaLogHandlerHolder camelLogHandlerHolder() {
		LOG.debug("jolokia logging using slf4j");
		final SpringJolokiaLogHandlerHolder logHandlerHolder = new SpringJolokiaLogHandlerHolder();
		logHandlerHolder.setType("slf4j");
		return logHandlerHolder;
	}

	@Bean
	@ConditionalOnMissingBean(name = "camelConfigHolder")
	public SpringJolokiaConfigHolder camelConfigHolder() {
		LOG.debug("jolokia configuration from properties");
		final SpringJolokiaConfigHolder springJolokiaConfigHolder = new SpringJolokiaConfigHolder();
		setDefaultConfigValue("host", "0.0.0.0");
		setDefaultConfigValue("autoStart", "true");
		if (configuration.isUseCamelRestrictor()
				&& !configuration.getServerConfig().containsKey(ConfigKey.RESTRICTOR_CLASS.getKeyValue())) {
			setDefaultConfigValue(ConfigKey.RESTRICTOR_CLASS.getKeyValue(),
					"org.apache.camel.component.jolokia.springboot.restrictor.CamelRestrictor");
		}
		if (configuration.isKubernetesDiscover()) {
			LOG.debug("trying to discover k8s environment");
			final String caCert = configuration.isKubernetesUseDefaultCa() ? DEFAULT_CA_ON_K8S
					: configuration.getServerConfig().getOrDefault("caCert", DEFAULT_CA_ON_K8S);
			if (Files.exists(Path.of(caCert))) {
				setDefaultConfigValue("protocol", "https");
				setDefaultConfigValue("useSslClientAuthentication", "true");
				setDefaultConfigValue("caCert", caCert);
			} else {
				LOG.debug("kubernetesDiscover is enabled but the file {} does not exist, no additional properties will be set", caCert);
			}
		}
		springJolokiaConfigHolder.setConfig(configuration.getServerConfig());
		return springJolokiaConfigHolder;
	}

	private void setDefaultConfigValue(String key, String defaultValue) {
		final String configValue = configuration.getServerConfig().getOrDefault(key, defaultValue);
		LOG.debug("jolokia config set {} = {}", key, configValue);
		configuration.getServerConfig().put(key, configValue);
	}

	private void printConfiguration() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("jolokia agent configuration: {}", configuration);
		}
	}

	private Map<String, String> mergeConfig(Map<String, String> fromConfig, Map<String, String> fromBean) {
		//in case no custom bean, it is the same object
		if (fromConfig.equals(fromBean)) {
			return fromConfig;
		}
		final Map<String, String> mergedMap = new HashMap<>(fromConfig);
		fromBean.forEach((beanKey, beanVal) -> {
			if (configuration.isConfigFromPropertiesFirst()) {
				mergedMap.put(beanKey, fromConfig.getOrDefault(beanKey, beanVal));
			} else {
				mergedMap.put(beanKey, beanVal);
			}
		});
		return mergedMap;
	}
}
