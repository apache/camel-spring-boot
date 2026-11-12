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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
                classes = { CamelPropertiesHelperTest.TestConfiguration.class },
                properties = { "camel.test.my-config.enabled = true",
                        "camel.test.my-config.name = Donald Duck",
                        "camel.test.my-config.verify-hostname = true",
                        "camel.test.my-config.no-such-option-on-the-target = bar" })
public class CamelPropertiesHelperTest {

    static final String PREFIX = "camel.test.my-config";

    @Autowired
    ApplicationContext context;

    @Autowired
    CamelContext camelContext;

    /**
     * Bound by Spring Boot from the test properties, like a generated configuration class is.
     */
    @Autowired
    MyDriftedConfiguration config;

    @Configuration
    @EnableConfigurationProperties(MyDriftedConfiguration.class)
    static class TestConfiguration {
        @Bean(name = "myCoolOption")
        MyOption myCoolBean() {
            return new MyOption();
        }
    }

    public static class MyOption {
    }

    /**
     * Mimics a generated {@code *ComponentConfiguration} class: the auto configuration layer options
     * (enabled/customizer) are inherited and are not options on the Camel target bean, and the catalog defaults are
     * field initializers.
     */
    public static class MyConfiguration extends ComponentConfigurationPropertiesCommon {

        private String name;
        private MyOption option;
        private Boolean secure = false;
        private Boolean verifyHostname = true;
        // null by default — suitable for testing programmatic assignment (CAMEL-26782)
        private Long requestTimeout;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public MyOption getOption() {
            return option;
        }

        public void setOption(MyOption option) {
            this.option = option;
        }

        public Boolean getSecure() {
            return secure;
        }

        public void setSecure(Boolean secure) {
            this.secure = secure;
        }

        public Boolean getVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(Boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
        }

        public Long getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Long requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    /**
     * A configuration class holding an option that does not exist on the target bean, which is what generator or
     * catalog drift looks like at runtime.
     */
    @ConfigurationProperties(prefix = PREFIX)
    public static class MyDriftedConfiguration extends MyConfiguration {

        private String noSuchOptionOnTheTarget;
        private String anotherOptionOnlyCarryingItsDefault = "false";

        public String getNoSuchOptionOnTheTarget() {
            return noSuchOptionOnTheTarget;
        }

        public void setNoSuchOptionOnTheTarget(String noSuchOptionOnTheTarget) {
            this.noSuchOptionOnTheTarget = noSuchOptionOnTheTarget;
        }

        public String getAnotherOptionOnlyCarryingItsDefault() {
            return anotherOptionOnlyCarryingItsDefault;
        }

        public void setAnotherOptionOnlyCarryingItsDefault(String anotherOptionOnlyCarryingItsDefault) {
            this.anotherOptionOnlyCarryingItsDefault = anotherOptionOnlyCarryingItsDefault;
        }
    }

    public static class MyClass {

        private int id;
        private String name;
        private MyOption option;
        private CamelContext camelContext;
        private MyFooClass myFooClass;
        private boolean secure;
        private boolean verifyHostname;
        private Long requestTimeout;

        public int getId() {
            return id;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public boolean isVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public MyOption getOption() {
            return option;
        }

        public void setOption(MyOption option) {
            this.option = option;
        }

        public CamelContext getCamelContext() {
            return camelContext;
        }

        public void setCamelContext(CamelContext camelContext) {
            this.camelContext = camelContext;
        }

        public MyFooClass getMyFooClass() {
            return myFooClass;
        }

        public void setMyFooClass(MyFooClass myFooClass) {
            this.myFooClass = myFooClass;
        }

        public Long getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Long requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    @Test
    public void testSetCamelProperties() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "myCoolOption");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);

        Assertions.assertEquals(0, map.size(), "Should configure all options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
    }

    @Test
    public void testSetCamelPropertiesAutowired() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "myCoolOption");
        map.put("camelContext", "#autowired");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);

        Assertions.assertEquals(0, map.size(), "Should configure all options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
        Assertions.assertSame(camelContext, target.getCamelContext());
    }

    @Test
    public void testSetCamelPropertiesType() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "myCoolOption");
        map.put("camelContext", "#type:org.apache.camel.CamelContext");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);

        Assertions.assertEquals(0, map.size(), "Should configure all options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
        Assertions.assertSame(camelContext, target.getCamelContext());
    }

    @Test
    public void testSetCamelPropertiesClass() throws Exception {
        MyClass target = new MyClass();

        // must use linked hash map as we must create foo first before we set its name as nested property
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "myCoolOption");
        map.put("camelContext", "#type:org.apache.camel.CamelContext");
        map.put("myFooClass", "#class:org.apache.camel.spring.boot.util.MyFooClass");
        map.put("myFooClass.name", "Goofy");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);

        Assertions.assertEquals(0, map.size(), "Should configure all options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
        Assertions.assertSame(camelContext, target.getCamelContext());

        MyFooClass myFooClass = target.getMyFooClass();
        Assertions.assertNotNull(myFooClass);
        Assertions.assertSame(camelContext, myFooClass.getCamelContext());
        Assertions.assertEquals("Goofy", myFooClass.getName());
    }

    @Test
    public void testSetCamelPropertiesReference() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "#myCoolOption");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);

        Assertions.assertEquals(0, map.size(), "Should configure all options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
    }

    @Test
    public void testSetCamelPropertiesUnknownOption() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "#myCoolOption");
        map.put("unknown", "foo");

        try {
            CamelPropertiesHelper.setCamelProperties(camelContext, target, map, true);
            Assertions.fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            // expected
            Assertions.assertTrue(e.getMessage().startsWith("Cannot configure option [unknown] with value [foo]"));
        }

        Assertions.assertEquals(1, map.size(), "Should configure the three first options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
    }

    @Test
    public void testCopyConfigurationPropertiesIgnoresAutoConfigurationOptions() {
        MyClass target = new MyClass();

        // enabled and customizer are always set on a generated configuration class, and must not be
        // attempted on the target bean
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertNotNull(config.getCustomizer());

        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, configWithoutDrift(),
                target);

        Assertions.assertEquals("Donald Duck", target.getName());
    }

    @Test
    public void testCopyConfigurationPropertiesFailsOnConfiguredOptionThatCannotBeSet() {
        MyClass target = new MyClass();

        // camel.test.my-config.no-such-option-on-the-target is set on the test application
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, config,
                        target));
        Assertions.assertTrue(e.getMessage().contains("camel.test.my-config.no-such-option-on-the-target"),
                e.getMessage());
        Assertions.assertTrue(e.getMessage().contains(CamelPropertiesHelper.LENIENT_CONFIGURATION_BINDING),
                e.getMessage());
    }

    @Test
    public void testCopyConfigurationPropertiesIgnoresDefaultThatCannotBeSet() {
        MyClass target = new MyClass();

        // nothing configured anotherOptionOnlyCarryingItsDefault, so it only carries a catalog default and must
        // not break startup
        Assertions.assertEquals("false", config.getAnotherOptionOnlyCarryingItsDefault());
        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, configWithoutDrift(),
                target);

        Assertions.assertEquals("Donald Duck", target.getName());
    }

    @Test
    public void testCopyConfigurationPropertiesDoesNotOverwriteWithCatalogDefault() {
        MyClass target = new MyClass();
        target.setSecure(true);

        // secure is not set on the test application, so it only carries the catalog default false
        Assertions.assertEquals(false, config.getSecure());
        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, configWithoutDrift(),
                target);

        Assertions.assertTrue(target.isSecure(), "The catalog default must not overwrite the value set on the target");
    }

    @Test
    public void testCopyConfigurationPropertiesAppliesOptionConfiguredToItsDefault() {
        MyClass target = new MyClass();
        target.setVerifyHostname(false);

        // verify-hostname is set on the test application to the same value as its catalog default
        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, configWithoutDrift(),
                target);

        Assertions.assertTrue(target.isVerifyHostname(), "A configured option must be applied whatever its value");
    }

    @Test
    void testCopyConfigurationPropertiesCopiesProgrammaticallyAssignedValue() {
        MyClass target = new MyClass();

        // Simulate the same pattern as SpringBootPlatformWebMvcConfiguration: an auto-configuration class
        // sets a value on the configuration bean in Java (not from Spring Boot application properties).
        // Before the fix (CAMEL-26782), this programmatically-assigned value was silently dropped because
        // it was not recorded in BoundConfigurationProperties.
        MyConfiguration cfg = configWithoutDrift();
        cfg.setRequestTimeout(30000L); // assigned in Java, not via application properties

        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, cfg, target);

        Assertions.assertEquals(30000L, target.getRequestTimeout(),
                "A value assigned programmatically in Java must be copied to the target");
    }

    @Test
    void testCopyConfigurationPropertiesDoesNotCopyProgrammaticValueMatchingDefault() {
        // anotherOptionOnlyCarryingItsDefault has the field initializer "false" in MyDriftedConfiguration
        // and is NOT configured via application properties. It has no setter on MyClass, so if it were
        // passed to the target it would cause a startup failure (lenient binding is off).
        // The fix must recognise it as carrying only its catalog default and drop it silently.
        MyClass target = new MyClass();
        MyDriftedConfiguration cfg = new MyDriftedConfiguration();
        cfg.setName(config.getName());
        cfg.setVerifyHostname(config.getVerifyHostname());
        // anotherOptionOnlyCarryingItsDefault is left at its "false" field initializer — not Spring-bound

        // must not throw and must not set anything that wasn't configured
        CamelPropertiesHelper.copyConfigurationProperties(camelContext, context, PREFIX, cfg, target);

        Assertions.assertEquals("Donald Duck", target.getName(),
                "The Spring-bound name must still be applied");
    }

    /**
     * The bound configuration without the option that cannot be set, for the tests that are not about that failure.
     */
    private MyConfiguration configWithoutDrift() {
        MyConfiguration answer = new MyConfiguration();
        answer.setName(config.getName());
        answer.setSecure(config.getSecure());
        answer.setVerifyHostname(config.getVerifyHostname());
        return answer;
    }

    @Test
    public void testSetCamelPropertiesUnknownOptionIgnore() throws Exception {
        MyClass target = new MyClass();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "123");
        map.put("name", "Donald Duck");
        map.put("option", "#myCoolOption");
        map.put("unknown", "foo");

        CamelPropertiesHelper.setCamelProperties(camelContext, target, map, false);

        Assertions.assertEquals(1, map.size(), "Should configure the three first options");
        Assertions.assertEquals(123, target.getId());
        Assertions.assertEquals("Donald Duck", target.getName());
        Assertions.assertSame(context.getBean("myCoolOption"), target.getOption());
    }

}
