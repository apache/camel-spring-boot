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
package org.apache.camel.component.jolokia.springboot.restrictor;

import org.jolokia.server.core.restrictor.AllowAllRestrictor;

import javax.management.ObjectName;

import java.util.List;
import java.util.function.Function;

public class CamelRestrictor extends AllowAllRestrictor {

	private final List<String> allowedDomains = List.of("org.apache.camel", "java.lang", "java.nio", "jboss.threads");

	private Function<ObjectName, Boolean> objectNameEvaluator =
			objectName -> this.getAllowedDomains().contains(objectName.getDomain());

	@Override
	public boolean isAttributeReadAllowed(ObjectName pName, String pAttribute) {
		return objectNameEvaluator.apply(pName);
	}

	@Override
	public boolean isAttributeWriteAllowed(ObjectName pName, String pAttribute) {
		return objectNameEvaluator.apply(pName);
	}

	@Override
	public boolean isOperationAllowed(ObjectName pName, String pOperation) {
		return objectNameEvaluator.apply(pName);
	}

	@Override
	public boolean isObjectNameHidden(ObjectName name) {
		return !objectNameEvaluator.apply(name);
	}

	/**
	 * Provides the list of allowed domains from JMX.
	 * @return List of String, the list of the allowed domains.
	 */
	protected List<String> getAllowedDomains() {
		return allowedDomains;
	}

	protected final List<String> getDefaultDomains() {
		return allowedDomains;
	}
}
