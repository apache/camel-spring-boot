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
package org.apache.camel.opentelemetry2.starter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;

import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskDecorator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = OtelContextPropagatingTaskDecoratorTest.class)
public class OtelContextPropagatingTaskDecoratorTest {

    private static final ContextKey<String> TEST_KEY = ContextKey.named("test-key");

    @Autowired
    private TaskDecorator taskDecorator;

    @Test
    void taskDecoratorBeanIsRegistered() {
        assertNotNull(taskDecorator);
    }

    @Test
    void taskDecoratorPropagatesOtelContext() throws Exception {
        AtomicReference<String> valueOnWorkerThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Set a value in the OTel context on the current thread
        Context contextWithValue = Context.current().with(TEST_KEY, "propagated-value");
        try (var ignored = contextWithValue.makeCurrent()) {
            // Decorate the runnable while the context is active
            Runnable decorated = taskDecorator.decorate(() -> {
                valueOnWorkerThread.set(Context.current().get(TEST_KEY));
                latch.countDown();
            });

            // Run on a different thread — context should still be propagated
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.execute(decorated);
                latch.await();
            } finally {
                executor.shutdown();
            }
        }

        assertEquals("propagated-value", valueOnWorkerThread.get());
    }
}
