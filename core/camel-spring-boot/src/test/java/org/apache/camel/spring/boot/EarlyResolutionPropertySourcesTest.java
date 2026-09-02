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
package org.apache.camel.spring.boot;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyResolutionPropertySourcesTest {

    @Test
    void shouldExtractStringFromOriginTrackedValue() {
        OriginTrackedValue tracked = OriginTrackedValue.of("{{vault:secret}}");
        assertEquals("{{vault:secret}}", EarlyResolutionPropertySources.asString(tracked));
    }

    @Test
    void shouldPreserveHighestPrecedenceValueForDuplicateKeys() {
        Properties props = new Properties();
        EarlyResolutionPropertySources.putIfAbsent(props, "key", "from-prod");
        EarlyResolutionPropertySources.putIfAbsent(props, "key", "from-default");

        assertEquals("from-prod", props.getProperty("key"));
    }

    @Test
    void shouldDetectHigherPrecedencePropertyInMapPropertySource() {
        MutablePropertySources sources = new MutablePropertySources();
        MapPropertySource high = new MapPropertySource("high", Map.of("my.secret", "plain"));
        MapPropertySource low = new MapPropertySource("low", Map.of("my.secret", "{{vault:secret}}"));
        sources.addFirst(high);
        sources.addLast(low);

        assertTrue(EarlyResolutionPropertySources.hasHigherPrecedenceProperty(sources, low, "my.secret"));
        assertFalse(EarlyResolutionPropertySources.hasHigherPrecedenceProperty(sources, high, "my.secret"));
    }
}
