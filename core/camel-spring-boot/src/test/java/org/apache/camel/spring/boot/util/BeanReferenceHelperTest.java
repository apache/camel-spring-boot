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

import java.lang.reflect.Method;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import com.example.springboot.CamelPrefixedProperties;
import com.example.springboot.ThirdPartyProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanReferenceHelperTest {

    private static final String PREFIX = "camel.component.myComponent";

    private AnnotationConfigApplicationContext applicationContext;

    public static class MyOption {
    }

    public static class MyOtherOption {
    }

    @Configuration
    static class TestConfiguration {
        @Bean(name = "myOption")
        MyOption myOption() {
            return new MyOption();
        }

        @Bean(name = "myVerifier")
        HostnameVerifier myVerifier() {
            return (String hostname, SSLSession session) -> true;
        }

        @Bean(name = "myOtherOption")
        MyOtherOption myOtherOption() {
            return new MyOtherOption();
        }
    }

    @BeforeEach
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(TestConfiguration.class);
    }

    @AfterEach
    public void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    private Object resolve(Object source) {
        return BeanReferenceHelper.resolveBeanReference(applicationContext, source,
                TypeDescriptor.valueOf(MyOption.class), PREFIX);
    }

    @Test
    public void testNullAndEmptyAreNotConfigured() {
        assertNull(resolve(null));
        assertNull(resolve(""));
        assertNull(resolve("   "));
    }

    @Test
    public void testBeanSyntax() {
        assertSame(applicationContext.getBean("myOption"), resolve("#bean:myOption"));
    }

    @Test
    public void testHashSyntax() {
        assertSame(applicationContext.getBean("myOption"), resolve("#myOption"));
    }

    @Test
    public void testPlainBeanId() {
        // a plain bean id used to be silently converted to null
        assertSame(applicationContext.getBean("myOption"), resolve("myOption"));
    }

    @Test
    public void testAutowired() {
        assertSame(applicationContext.getBean("myOption"), resolve("#autowired"));
    }

    @Test
    public void testType() {
        assertSame(applicationContext.getBean("myOption"),
                resolve("#type:" + MyOption.class.getName()));
    }

    @Test
    public void testUnknownBeanFailsClosed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve("#bean:noSuchBean"));
        assertTrue(e.getMessage().contains("#bean:noSuchBean"), e.getMessage());
        assertTrue(e.getMessage().contains(MyOption.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(PREFIX), e.getMessage());
        assertTrue(e.getMessage().contains("#bean:myBeanId"), e.getMessage());
    }

    @Test
    public void testTypoedPlainValueFailsClosed() {
        // this is the case that used to end up as a null value on the component
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve("myOptionn"));
        assertTrue(e.getMessage().contains("myOptionn"), e.getMessage());
    }

    @Test
    public void testWrongBeanTypeFailsClosed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve("#myOtherOption"));
        assertTrue(e.getMessage().contains("myOtherOption"), e.getMessage());
        assertTrue(e.getMessage().contains(MyOption.class.getName()), e.getMessage());
    }

    @Test
    public void testEmptyBeanIdFailsClosed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> resolve("#bean:"));
        assertTrue(e.getMessage().contains("the bean id is empty"), e.getMessage());
    }

    @Test
    public void testClassSyntaxIsReported() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> resolve("#class:" + MyOption.class.getName()));
        assertTrue(e.getMessage().contains("#class: syntax is not supported"), e.getMessage());
    }

    @Test
    public void testUnknownTypeFailsClosed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> resolve("#type:com.foo.DoesNotExist"));
        assertTrue(e.getMessage().contains("com.foo.DoesNotExist"), e.getMessage());
    }

    @Test
    public void testMessageMentionsTheTargetType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> BeanReferenceHelper.resolveBeanReference(applicationContext, "nope",
                        TypeDescriptor.valueOf(MyOtherOption.class), PREFIX));
        assertTrue(e.getMessage().contains(MyOtherOption.class.getName()), e.getMessage());
    }

    @Test
    public void testTypeOfAnUnrelatedBeanFailsClosed() {
        // #type: resolves a single bean of the named type, which need not be assignable to the option
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> resolve("#type:" + MyOtherOption.class.getName()));
        assertTrue(e.getMessage().contains("not assignable to the option type"), e.getMessage());
        assertTrue(e.getMessage().contains(MyOtherOption.class.getName()), e.getMessage());
    }

    // *************************************
    // The generated converters are registered with @ConfigurationPropertiesBinding and so take part in every
    // @ConfigurationProperties binding in the application, not only in Camel's own.
    // *************************************

    private static TypeDescriptor verifierOf(Class<?> owner) throws Exception {
        Method setter = owner.getMethod("setVerifier", HostnameVerifier.class);
        return new TypeDescriptor(new MethodParameter(setter, 0));
    }

    @Test
    public void testTargetInCamelPackageIsCamelsOwn() throws Exception {
        assertTrue(BeanReferenceHelper.isCamelConfigurationTarget(verifierOf(CamelStyleProperties.class)));
    }

    @Test
    public void testTargetWithCamelPrefixIsCamelsOwn() throws Exception {
        assertTrue(BeanReferenceHelper.isCamelConfigurationTarget(verifierOf(CamelPrefixedProperties.class)));
    }

    @Test
    public void testThirdPartyTargetIsNotCamelsOwn() throws Exception {
        assertFalse(BeanReferenceHelper.isCamelConfigurationTarget(verifierOf(ThirdPartyProperties.class)));
    }

    @Test
    public void testUnknownTargetIsTreatedAsCamelsOwn() {
        // a TypeDescriptor that does not carry the bound class must never weaken Camel's own binding
        assertTrue(BeanReferenceHelper.isCamelConfigurationTarget(TypeDescriptor.valueOf(HostnameVerifier.class)));
        assertTrue(BeanReferenceHelper.isCamelConfigurationTarget(null));
    }

    @Test
    public void testThirdPartyBindingIsNotIntercepted() throws Exception {
        // this is what a third party class with a field of a type a starter registers for used to get, and
        // adding a starter to the classpath must not turn it into a startup failure
        assertNull(BeanReferenceHelper.resolveBeanReference(applicationContext, "someValue",
                verifierOf(ThirdPartyProperties.class), PREFIX));
    }

    @Test
    public void testThirdPartyBindingStillResolvesHashReferences() throws Exception {
        assertSame(applicationContext.getBean("myVerifier"), BeanReferenceHelper
                .resolveBeanReference(applicationContext, "#bean:myVerifier", verifierOf(ThirdPartyProperties.class),
                        PREFIX));
    }

    @Test
    public void testCamelBindingResolvesHashReferences() throws Exception {
        assertSame(applicationContext.getBean("myVerifier"), BeanReferenceHelper
                .resolveBeanReference(applicationContext, "#bean:myVerifier", verifierOf(CamelStyleProperties.class),
                        PREFIX));
    }

    @Test
    public void testCamelBindingResolvesPlainBeanIds() throws Exception {
        assertSame(applicationContext.getBean("myVerifier"), BeanReferenceHelper
                .resolveBeanReference(applicationContext, "myVerifier", verifierOf(CamelStyleProperties.class),
                        PREFIX));
    }

    /**
     * Stands in for a generated {@code *ComponentConfiguration}, which always lives under org.apache.camel.
     */
    public static class CamelStyleProperties {

        private HostnameVerifier verifier;

        public HostnameVerifier getVerifier() {
            return verifier;
        }

        public void setVerifier(HostnameVerifier verifier) {
            this.verifier = verifier;
        }
    }

}
