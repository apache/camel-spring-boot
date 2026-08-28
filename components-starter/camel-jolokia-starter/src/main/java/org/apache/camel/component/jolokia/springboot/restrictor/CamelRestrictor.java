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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;

public class CamelRestrictor extends AllowAllRestrictor {

	private static final String IPV6_LOOPBACK_COMPRESSED = "::1";
	private static final String IPV6_LOOPBACK_EXPANDED = "0:0:0:0:0:0:0:1";

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
	 * Rejects browser requests from non-loopback origins.
	 * <p/>
	 * {@link AllowAllRestrictor} permits every origin, which leaves the agent open to being driven by a remote page
	 * the user happens to visit - the port being on loopback is no protection against that. Requests from loopback
	 * origins are allowed so local browser clients such as Hawtio continue to work. A request that carries no Origin
	 * or Referer header is also allowed, so ordinary non-browser clients such as curl and the Jolokia CLI are
	 * unaffected. This follows the loopback-origin policy used by the Camel Quarkus Jolokia restrictor.
	 *
	 * @param  pOrigin         the Origin or Referer header of the request, or <tt>null</tt> when absent
	 * @param  pOnlyWhenStrictCheckingIsEnabled whether Jolokia asks to apply the check only in strict mode
	 * @return                 <tt>true</tt> if the request may proceed
	 */
	@Override
	public boolean isOriginAllowed(String pOrigin, boolean pOnlyWhenStrictCheckingIsEnabled) {
		if (pOrigin == null) {
			return true;
		}

		try {
			return isLoopbackHost(new URI(pOrigin).getHost());
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private static boolean isLoopbackHost(String host) {
		if (host == null || host.isEmpty()) {
			return false;
		}

		if (host.startsWith("[") && host.endsWith("]")) {
			host = host.substring(1, host.length() - 1);
		}

		if ("localhost".equalsIgnoreCase(host) || "localhost.localdomain".equalsIgnoreCase(host)) {
			return true;
		}

		if (IPV6_LOOPBACK_COMPRESSED.equals(host) || IPV6_LOOPBACK_EXPANDED.equals(host)) {
			return true;
		}

		String[] octets = host.split("\\.", -1);
		if (octets.length != 4 || !"127".equals(octets[0])) {
			return false;
		}

		try {
			for (int i = 1; i < octets.length; i++) {
				int octet = Integer.parseInt(octets[i]);
				if (octet < 0 || octet > 255) {
					return false;
				}
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
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
