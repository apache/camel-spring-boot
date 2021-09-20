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

import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_OUT_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_UNENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_UNENCRYPTED_PROPS_OUT_CC;

@Component("encryptedPropertiesBean")
public class EncryptedPropertiesBean {

    Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${encrypted.password}")
    private String encryptedPassword;

    @Value("${unencrypted.property}")
    private String unencryptedPassword;


    public void testEncryptedProperty(Exchange exchange) {
        LOG.info("test properties decryption outside camel context: test.password        = {}", encryptedPassword);
        exchange.getIn().setBody(encryptedPassword);
    }

    public void testUnencryptedProperty(Exchange exchange) {
        LOG.info("test unencrypted properties outside camel context: encrypted.property  = {}", unencryptedPassword);
        exchange.getIn().setBody(unencryptedPassword);
    }
}
