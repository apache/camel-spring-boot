/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.observation.starter.observation;

import io.micrometer.observation.ObservationRegistry;

/**
 * Taken from https://github.com/spring-projects/spring-boot/tree/v3.0.2/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/observation.
 */
@FunctionalInterface
public interface ObservationRegistryCustomizer<T extends ObservationRegistry> {

	/**
	 * Customize the given {@code registry}.
	 * @param registry the registry to customize
	 */
	void customize(T registry);

}
