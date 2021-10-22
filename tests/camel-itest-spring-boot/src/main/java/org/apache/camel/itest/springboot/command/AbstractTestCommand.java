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
package org.apache.camel.itest.springboot.command;

import java.util.concurrent.Future;

import org.apache.camel.itest.springboot.Command;
import org.apache.camel.itest.springboot.ITestConfig;
import org.junit.jupiter.api.Assertions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 * An abstract class for commands that need standard test parameters.
 */
public abstract class AbstractTestCommand implements Command {

    @Async // needs to run on a spring background thread
    @Override
    public Future<Object> execute(Object[] parameters) throws Exception {
        Assertions.assertNotNull(parameters, "Parameters cannot be null");
        Assertions.assertEquals(2, parameters.length, "Parameters should contain two elements");
        Object configObj = parameters[0];
        Assertions.assertNotNull(configObj, "The first parameter cannot be null");
        Assertions.assertTrue(configObj instanceof ITestConfig,
            "First parameter should be of type ITestConfig, found type " + configObj.getClass().getName());

        Object compNameObj = parameters[1];
        Assertions.assertNotNull(compNameObj, "The second parameter cannot be null");
        Assertions.assertTrue(compNameObj instanceof String,
            "Second parameter should be of type String, found type " + compNameObj.getClass().getName());

        String compName = (String) compNameObj;

        ITestConfig config = (ITestConfig) configObj;
        Object result = this.executeTest(config, compName);

        return new AsyncResult<>(result);
    }

    public abstract Object executeTest(ITestConfig config, String component) throws Exception;

}
