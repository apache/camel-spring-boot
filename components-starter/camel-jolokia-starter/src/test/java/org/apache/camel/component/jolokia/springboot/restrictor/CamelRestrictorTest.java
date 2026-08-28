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

import static org.assertj.core.api.Assertions.assertThat;

class CamelRestrictorTest {

	private final CamelRestrictor restrictor = new CamelRestrictor();

	private static ObjectName name(String value) throws MalformedObjectNameException {
		return new ObjectName(value);
	}

	@Test
	void rejectsCrossOriginBrowserRequests() {
		// AllowAllRestrictor permits every origin, which lets a page the user visits drive the agent -
		// binding to loopback is no protection against that
		assertThat(restrictor.isOriginAllowed("http://evil.example.com", false)).isFalse();
		assertThat(restrictor.isOriginAllowed("http://evil.example.com", true)).isFalse();
	}

	@Test
	void allowsLoopbackBrowserOrigins() {
		assertThat(restrictor.isOriginAllowed("http://localhost:8080", false)).isTrue();
		assertThat(restrictor.isOriginAllowed("http://127.0.0.1:8778", false)).isTrue();
		assertThat(restrictor.isOriginAllowed("http://127.255.255.255:8778", false)).isTrue();
		assertThat(restrictor.isOriginAllowed("http://[::1]:8778", false)).isTrue();
		assertThat(restrictor.isOriginAllowed("http://[0:0:0:0:0:0:0:1]:8778", true)).isTrue();
	}

	@Test
	void rejectsMalformedAndNonLoopbackOrigins() {
		assertThat(restrictor.isOriginAllowed("not a URI", false)).isFalse();
		assertThat(restrictor.isOriginAllowed("http://localhost.example.com", false)).isFalse();
		assertThat(restrictor.isOriginAllowed("http://128.0.0.1:8778", false)).isFalse();
	}

	@Test
	void allowsRequestsCarryingNoOrigin() {
		// curl and the Jolokia CLI send no Origin or Referer header
		assertThat(restrictor.isOriginAllowed(null, false)).isTrue();
		assertThat(restrictor.isOriginAllowed(null, true)).isTrue();
	}

	@Test
	void stillLimitsMBeansToTheAllowedDomains() throws Exception {
		assertThat(restrictor.isAttributeReadAllowed(name("org.apache.camel:type=context"), "CamelId")).isTrue();
		assertThat(restrictor.isAttributeReadAllowed(name("com.example:type=Secret"), "value")).isFalse();
		assertThat(restrictor.isObjectNameHidden(name("com.example:type=Secret"))).isTrue();
	}

	@Test
	void managementOfCamelMBeansRemainsPossible() throws Exception {
		// the starter exists to manage Camel through Jolokia, so operations on the Camel domain stay allowed;
		// that capability is why the agent binds to loopback by default
		assertThat(restrictor.isOperationAllowed(name("org.apache.camel:type=context"), "stop")).isTrue();
		assertThat(restrictor.isOperationAllowed(name("com.example:type=Secret"), "reveal")).isFalse();
	}
}
