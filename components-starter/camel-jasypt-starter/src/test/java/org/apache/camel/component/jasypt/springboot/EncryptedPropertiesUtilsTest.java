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

import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.iv.IvGenerator;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.Test;

import static org.apache.camel.component.jasypt.springboot.JasyptEncryptedPropertiesUtils.getIVGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EncryptedPropertiesUtilsTest {

    @Test
    public void noIvGeneratorPropertyTest(){
        //IVGenerator is null
        JasyptEncryptedPropertiesConfiguration configuration = new JasyptEncryptedPropertiesConfiguration();
        IvGenerator ivGenerator = getIVGenerator(configuration);
        assertThat(ivGenerator).isInstanceOf(NoIvGenerator.class);
    }

    @Test
    public void nonExistentIvGeneratorTest(){
        JasyptEncryptedPropertiesConfiguration configuration = new JasyptEncryptedPropertiesConfiguration();
        configuration.setIvGeneratorClassName("com.mycompany.iv.MyNonExixtentIvGenerator");
        assertThatExceptionOfType(EncryptionInitializationException.class)
                .isThrownBy(() -> { IvGenerator ivGenerator = getIVGenerator(configuration);});
    }

    @Test
    public void randomIvGeneratorTest(){
        JasyptEncryptedPropertiesConfiguration configuration = new JasyptEncryptedPropertiesConfiguration();
        configuration.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        IvGenerator ivGenerator = getIVGenerator(configuration);
        assertThat(ivGenerator).isInstanceOf(RandomIvGenerator.class);
    }
}
