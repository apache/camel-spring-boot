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
package org.apache.camel.component.cxf.security.jaas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import java.net.URL;
import java.util.Map;

/**
 * Simple LoginModule which checks plain username and password.
 */
@Component
public class SimpleLoginModule implements LoginModule {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleLoginModule.class);

	public static final String USERNAME = "admin";
	public static final String PASSWORD = "admin";

	private CallbackHandler callbackHandler;

	private boolean succeeded = false;

	static {
		final URL jaasConfig = SimpleLoginModule.class.getClassLoader().getResource("simple-jaas.conf");
		if (jaasConfig != null) {
			// Set jaas configuration file
			System.setProperty("java.security.auth.login.config", jaasConfig.toString());
		} else {
			LOG.debug("JAAS configuration doesn't exist.");
		}
	}

	public SimpleLoginModule() {
	}

	public boolean abort() throws LoginException {
		return false;
	}

	public boolean commit() throws LoginException {
		return succeeded;
	}

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		this.callbackHandler = callbackHandler;
		succeeded = false;
	}

	public boolean login() throws LoginException {
		succeeded = false;
		final Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("name:");
		callbacks[1] = new PasswordCallback("password:", false);

		try {
			callbackHandler.handle(callbacks);
		} catch (Exception e) {
			throw new LoginException("Error with callback processing.");
		}

		final NameCallback nameCallback = (NameCallback) callbacks[0];
		final PasswordCallback passwordCallback = (PasswordCallback) callbacks[1];

		final String name = nameCallback.getName();
		final String password = new String(passwordCallback.getPassword());

		if (USERNAME.equals(name) && PASSWORD.equals(password)) {
			succeeded = true;
		} else {
			throw new FailedLoginException("Sorry! No login for you.");
		}
		return succeeded;
	}

	public boolean logout() throws LoginException {
		return false;
	}
}
