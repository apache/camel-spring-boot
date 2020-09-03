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
package org.apache.camel.spring.boot.util;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class HierarchicalCondition extends SpringBootCondition {
    private final String[] items;

    public HierarchicalCondition(String... items) {
        this.items = Arrays.copyOf(items, items.length);
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        if (items.length == 0) {
            return ConditionOutcome.match( ConditionMessage.forCondition("no condition").because("no conditions"));
        }

        final ConditionMessage.Builder message = ConditionMessage.forCondition(this.items[0]);
        final Environment environment = conditionContext.getEnvironment();

        return HierarchicalPropertiesEvaluator.evaluate(environment, items)
            ? ConditionOutcome.match(message.because("enabled"))
            : ConditionOutcome.noMatch(message.because("not enabled"));
    }
}
