package org.apache.camel.openapi;

import org.springframework.stereotype.Component;

@Component("bookService")
public class DummyBookService {
	public void getOrder(Integer id) {
		// Mock
	}

	public void updateOrder() {
		// Mock
	}

	public void listOrders() {
		// Mock
	}
}
