package org.apache.camel.openapi;

import org.springframework.stereotype.Component;

@Component("userService")
public class DummyUserService {
	public void getUser(Integer id) {
		// Mock
	}

	public void updateUser() {
		// Mock
	}

	public void listUsers() {
		// Mock
	}
}
