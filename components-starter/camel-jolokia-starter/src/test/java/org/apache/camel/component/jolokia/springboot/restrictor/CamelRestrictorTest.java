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

import org.junit.jupiter.api.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelRestrictorTest {

	private final CamelRestrictor restrictor = new CamelRestrictor();

	private static ObjectName name(String value) throws MalformedObjectNameException {
		return new ObjectName(value);
	}

	@Test
	void rejectsCrossOriginBrowserRequests() {
		// AllowAllRestrictor permits every origin, which lets a page the user visits drive the agent -
		// binding to loopback is no protection against that
		assertFalse(restrictor.isOriginAllowed("http://evil.example.com", false));
		assertFalse(restrictor.isOriginAllowed("http://evil.example.com", true));
	}

	@Test
	void allowsRequestsCarryingNoOrigin() {
		// curl, Hawtio and the Jolokia CLI send no Origin or Referer header
		assertTrue(restrictor.isOriginAllowed(null, false));
		assertTrue(restrictor.isOriginAllowed(null, true));
	}

	@Test
	void stillLimitsMBeansToTheAllowedDomains() throws Exception {
		assertTrue(restrictor.isAttributeReadAllowed(name("org.apache.camel:type=context"), "CamelId"));
		assertFalse(restrictor.isAttributeReadAllowed(name("com.example:type=Secret"), "value"));
		assertTrue(restrictor.isObjectNameHidden(name("com.example:type=Secret")));
	}

	@Test
	void managementOfCamelMBeansRemainsPossible() throws Exception {
		// the starter exists to manage Camel through Jolokia, so operations on the Camel domain stay allowed;
		// that capability is why the agent binds to loopback by default
		assertTrue(restrictor.isOperationAllowed(name("org.apache.camel:type=context"), "stop"));
		assertFalse(restrictor.isOperationAllowed(name("com.example:type=Secret"), "reveal"));
	}
}
