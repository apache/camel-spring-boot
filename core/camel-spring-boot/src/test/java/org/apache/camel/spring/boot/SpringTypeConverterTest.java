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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipFile;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = SpringTypeConverterTest.SpringTypeConversionConfiguration.class)
public class SpringTypeConverterTest {
    @Autowired
    @Qualifier("camelSpringConversionService")
    ConversionService conversionService;

    @Autowired
    @Qualifier("camelSpringTypeConverter")
    SpringTypeConverter converter;

    @Test
    public void testConversionService() {
        Collection<?> source = Arrays.asList(new Person("Name", 30));

        Assertions.assertFalse(conversionService.canConvert(Person.class, String.class));
        Assertions.assertTrue(conversionService.canConvert(source.getClass(), String.class));

        try {
            conversionService.convert(source, String.class);
        } catch (ConversionFailedException e) {
            // Expected as Person can't be converted to a string according to
            // Spring's FallbackObjectToStringConverter, see javadoc for:
            //
            // org.springframework.core.convert.support.FallbackObjectToStringConverter
            //
            Assertions.assertTrue(e.getCause() instanceof ConverterNotFoundException);
        }

        Assertions.assertNull(converter.convertTo(String.class, source));
    }

    @Test
    public void testStringToFileInputStreamConversionIsBlocked() {
        // Spring's ObjectToObjectConverter sees FileInputStream(String) constructor and
        // treats String content as a file path — must return null so Camel's own converters
        // handle String -> InputStream correctly
        Assertions.assertNull(converter.convertTo(FileInputStream.class, null, "some file content"));
    }

    @Test
    public void testStringToInputStreamConversionIsBlocked() {
        Assertions.assertNull(converter.convertTo(InputStream.class, null, "some file content"));
    }

    @Test
    public void testStringToFileReaderConversionIsBlocked() {
        // FileReader(String) would open the content as a path, exactly as FileInputStream(String) did
        Assertions.assertNull(converter.convertTo(FileReader.class, null, "some file content"));
    }

    @Test
    public void testStringToReaderConversionIsBlocked() {
        Assertions.assertNull(converter.convertTo(Reader.class, null, "some file content"));
    }

    @Test
    public void testStringToFileWriterConversionIsBlocked() throws Exception {
        // FileWriter(String) would go further and create the named file
        Path target = Files.createTempDirectory("spring-type-converter").resolve("must-not-be-created.txt");
        Assertions.assertNull(converter.convertTo(FileWriter.class, null, target.toString()));
        Assertions.assertFalse(Files.exists(target), "converting a String must not create a file on disk");
    }

    @Test
    public void testStringToZipFileConversionIsBlocked() {
        Assertions.assertNull(converter.convertTo(ZipFile.class, null, "some file content"));
    }

    @Test
    public void testStringToFileConversionIsStillAllowed() {
        // String -> File is a documented Spring conversion where the String really is a path, so the guard
        // must not swallow it
        Assertions.assertEquals(new File("/tmp/example.txt"),
                converter.convertTo(File.class, null, "/tmp/example.txt"));
    }

    public static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{" + "name='" + name + '\'' + ", age=" + age + '}';
        }
    }

    @Configuration
    public static class SpringTypeConversionConfiguration {
        @Bean
        ConversionService camelSpringConversionService(ApplicationContext applicationContext) {
            DefaultConversionService service = new DefaultConversionService();
            for (Converter converter : applicationContext.getBeansOfType(Converter.class).values()) {
                service.addConverter(converter);
            }

            return service;
        }

        @Bean
        SpringTypeConverter camelSpringTypeConverter(List<ConversionService> conversionServices) {
            return new SpringTypeConverter(conversionServices);
        }
    }
}
