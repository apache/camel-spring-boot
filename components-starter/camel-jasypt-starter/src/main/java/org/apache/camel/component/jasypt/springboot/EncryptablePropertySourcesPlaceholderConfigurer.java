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
package org.apache.camel.component.jasypt.springboot;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.util.StringValueResolver;


import static org.jasypt.properties.PropertyValueEncryptionUtils.decrypt;
import static org.jasypt.properties.PropertyValueEncryptionUtils.isEncryptedValue;


public class EncryptablePropertySourcesPlaceholderConfigurer
        extends PropertySourcesPlaceholderConfigurer {
    /**
     * The encryptor.
     */
    private StringEncryptor stringEncryptor;

    /**
     * PropertySourcesPlaceholderConfigurer  constructor
     * @param stringEncryptor the encryptor
     */
    @Autowired
    public EncryptablePropertySourcesPlaceholderConfigurer(StringEncryptor stringEncryptor){
        this.stringEncryptor = stringEncryptor;
    }


    /**
     * Visit each bean definition in the given bean factory and attempt to replace ${...} property
     * placeholders with values from the given properties. If a property is encrypted, it decrypt first
     * and then replace.
     *
     * @param beanFactory the bean factory to process.
     * @param propertyResolver used to resolve the properties
     * @throws BeansException If an error occurs.
     */
    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
                final ConfigurablePropertyResolver propertyResolver) throws BeansException {

        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        propertyResolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = strVal -> {
            String resolved = this.ignoreUnresolvablePlaceholders ?
                    propertyResolver.resolvePlaceholders(strVal) :
                    propertyResolver.resolveRequiredPlaceholders(strVal);
            if (this.trimValues) {
                resolved = resolved.trim();
            }
            if(isEncryptedValue(resolved)){
                resolved = decrypt(resolved, stringEncryptor);
            }
            return (resolved.equals(this.nullValue) ? null : resolved);
        };
        doProcessProperties(beanFactory, valueResolver);
    }
}
