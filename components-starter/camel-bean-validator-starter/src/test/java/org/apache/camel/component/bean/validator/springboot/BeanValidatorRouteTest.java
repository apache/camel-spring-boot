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
package org.apache.camel.component.bean.validator.springboot;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.condition.OS.AIX;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        BeanValidatorRouteTest.class
    }
)
public class BeanValidatorRouteTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    
    private Locale origLocale;

    @BeforeEach
    public void setLanguage() {
        origLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void restoreLanguage() {
        Locale.setDefault(origLocale);
    }

    //@DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideValidCars")
    void validateShouldSuccessWithImpliciteDefaultGroup(Object cars) {

        Exchange exchange = template.request("bean-validator://x", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideValidCars")
    void validateShouldSuccessWithExpliciteDefaultGroup(Object cars) {

        Exchange exchange = template.request("bean-validator://x?group=javax.validation.groups.Default", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideInvalidCarsWithoutLicensePlate")
    void validateShouldFailWithImpliciteDefaultGroup(Object cars, int numberOfViolations) {

        final String url = "bean-validator://x";

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("licensePlate", cv.getPropertyPath().toString());
                assertEquals(null, cv.getInvalidValue());
                assertEquals("must not be null", cv.getMessage());
            });
        }

        setLicensePlates(cars, "D-A");

        Exchange exchange = template.request(url, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideInvalidCarsWithoutLicensePlate")
    void validateShouldFailWithExpliciteDefaultGroup(Object cars, int numberOfViolations) {

        final String url = "bean-validator://x?group=javax.validation.groups.Default";

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("licensePlate", cv.getPropertyPath().toString());
                assertEquals(null, cv.getInvalidValue());
                assertEquals("must not be null", cv.getMessage());
            });
        }

        setLicensePlates(cars, "D-A");

        Exchange exchange = template.request(url, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideInvalidCarsWithShortLicensePlate")
    void validateShouldFailWithOptionalChecksGroup(Object cars, int numberOfViolations) {

        final String url = "bean-validator://x?group=org.apache.camel.component.bean.validator.springboot.OptionalChecks";

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("licensePlate", cv.getPropertyPath().toString());
                assertEquals("D-A", cv.getInvalidValue());
                assertEquals("size must be between 5 and 14", cv.getMessage());
            });
        }

        setLicensePlates(cars, "DD-AB-123");

        Exchange exchange = template.request(url, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideInvalidCarsWithoutManufacturer")
    void validateShouldFailWithOrderedChecksGroup(Object cars, int numberOfViolations) {

        final String url = "bean-validator://x?group=org.apache.camel.component.bean.validator.springboot.OrderedChecks";

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("manufacturer", cv.getPropertyPath().toString());
                assertEquals(null, cv.getInvalidValue());
                assertEquals("must not be null", cv.getMessage());
            });
        }

        setManufacturer(cars, "BMW");

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("licensePlate", cv.getPropertyPath().toString());
                assertEquals("D-A", cv.getInvalidValue());
                assertEquals("size must be between 5 and 14", cv.getMessage());
            });
        }

        setLicensePlates(cars, "DD-AB-123");

        Exchange exchange = template.request(url, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideCarsWithRedefinedDefaultGroup")
    void validateShouldSuccessWithRedefinedDefaultGroup(Object cars) {

        final String url = "bean-validator://x";

        Exchange exchange = template.request(url, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(cars);
            }
        });

        assertNotNull(exchange);
    }

    @DisabledOnOs(AIX)
    @ParameterizedTest
    @MethodSource("provideCarsWithRedefinedDefaultGroupAndShortLicencePlate")
    void validateShouldFailWithRedefinedDefaultGroup(Object cars, int numberOfViolations) {

        final String url = "bean-validator://x";

        try {
            template.requestBody(url, cars);
            fail("should throw exception");
        } catch (CamelExecutionException e) {
            assertInstanceOf(BeanValidationException.class, e.getCause());

            BeanValidationException exception = (BeanValidationException) e.getCause();
            Set<ConstraintViolation<Object>> constraintViolations = exception.getConstraintViolations();

            assertEquals(numberOfViolations, constraintViolations.size());
            constraintViolations.forEach(cv -> {
                assertEquals("licensePlate", cv.getPropertyPath().toString());
                assertEquals("D-A", cv.getInvalidValue());
                assertEquals("size must be between 5 and 14", cv.getMessage());
            });
        }
    }

    static Car createCar(String manufacturer, String licencePlate) {
        return new CarWithAnnotations(manufacturer, licencePlate);
    }

    private static Stream<Arguments> provideValidCars() {
        return Stream.of(
                Arguments.of(createCar("BMW", "DD-AB-123")),
                Arguments.of(Arrays.asList(
                        createCar("BMW", "DD-AB-123"),
                        createCar("VW", "XX-YZ-789"))));
    }

    private static Stream<Arguments> provideInvalidCarsWithoutLicensePlate() {
        return Stream.of(
                Arguments.of(createCar("BMW", null), 1),
                Arguments.of(Arrays.asList(
                        createCar("BMW", null),
                        createCar("VW", null)), 2));
    }

    private static Stream<Arguments> provideInvalidCarsWithShortLicensePlate() {
        return Stream.of(
                Arguments.of(createCar("BMW", "D-A"), 1),
                Arguments.of(Arrays.asList(
                        createCar("BMW", "D-A"),
                        createCar("VW", "D-A")), 2));
    }

    private static Stream<Arguments> provideInvalidCarsWithoutManufacturer() {
        return Stream.of(
                Arguments.of(createCar(null, "D-A"), 1),
                Arguments.of(Arrays.asList(
                        createCar(null, "D-A"),
                        createCar(null, "D-A")), 2));
    }

    private static Stream<Arguments> provideCarsWithRedefinedDefaultGroup() {
        return Stream.of(
                Arguments.of(new CarWithRedefinedDefaultGroup(null, "DD-AB-123")),
                Arguments.of(Arrays.asList(
                        new CarWithRedefinedDefaultGroup(null, "DD-AB-123")),
                        new CarWithRedefinedDefaultGroup(null, "XX-YZ-789")));
    }

    private static Stream<Arguments> provideCarsWithRedefinedDefaultGroupAndShortLicencePlate() {
        return Stream.of(
                Arguments.of(new CarWithRedefinedDefaultGroup(null, "D-A"), 1),
                Arguments.of(Arrays.asList(
                        new CarWithRedefinedDefaultGroup(null, "D-A"),
                        new CarWithRedefinedDefaultGroup(null, "D-A")), 2));
    }

    private void setLicensePlates(Object cars, String licensePlate) {
        if (cars instanceof Car) {
            ((Car) cars).setLicensePlate(licensePlate);
        } else {
            ((Iterable) cars).forEach(car -> ((Car) car).setLicensePlate(licensePlate));
        }
    }

    private void setManufacturer(Object cars, String manufacturer) {
        if (cars instanceof Car) {
            ((Car) cars).setManufacturer(manufacturer);
        } else {
            ((Iterable) cars).forEach(car -> ((Car) car).setManufacturer(manufacturer));
        }
    }


}
