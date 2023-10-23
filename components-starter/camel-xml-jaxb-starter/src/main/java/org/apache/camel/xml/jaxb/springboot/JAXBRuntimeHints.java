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
package org.apache.camel.xml.jaxb.springboot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlInlineBinaryData;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSchemaTypes;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.asm.ClassReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ReflectionUtils;

import static org.apache.camel.spring.boot.aot.ReflectionHelper.applyIfMatch;
import static org.apache.camel.spring.boot.aot.ReflectionHelper.getClassesByAnnotations;
import static org.apache.camel.spring.boot.aot.RuntimeHintsHelper.registerClassHierarchy;

final class JAXBRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JAXBRuntimeHints.class);

    private static final List<Class<? extends Annotation>> JAXB_ROOT_ANNOTATIONS = List.of(
            XmlRootElement.class, XmlType.class, XmlRegistry.class, XmlJavaTypeAdapter.class, XmlSeeAlso.class);

    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = List.of(
            XmlAccessorType.class,
            XmlAnyAttribute.class,
            XmlAnyElement.class,
            XmlAttachmentRef.class,
            XmlAttribute.class,
            XmlElement.class,
            XmlElementDecl.class,
            XmlElementRef.class,
            XmlElementRefs.class,
            XmlElements.class,
            XmlElementWrapper.class,
            XmlEnum.class,
            XmlEnumValue.class,
            XmlID.class,
            XmlIDREF.class,
            XmlInlineBinaryData.class,
            XmlList.class,
            XmlMimeType.class,
            XmlMixed.class,
            XmlNs.class,
            XmlRegistry.class,
            XmlRootElement.class,
            XmlSchema.class,
            XmlSchemaType.class,
            XmlSchemaTypes.class,
            XmlSeeAlso.class,
            XmlTransient.class,
            XmlType.class,
            XmlValue.class,
            XmlJavaTypeAdapter.class,
            XmlJavaTypeAdapters.class);

    private static final List<String> NATIVE_PROXY_DEFINITIONS = List.of(
            "org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler",
            "com.sun.xml.txw2.output.CharacterEscapeHandler",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Bindings",
            "org.glassfish.jaxb.core.v2.schemagen.episode.SchemaBindings",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Klass",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Package",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Annotated",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Annotation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Any",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Appinfo",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.AttrDecls",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.AttributeType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexContent",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexExtension",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexRestriction",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexTypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexTypeModel",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ContentModelContainer",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Documentation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Element",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ExplicitGroup",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ExtensionType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.FixedOrDefault",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Import",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.List",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.LocalAttribute",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.LocalElement",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.NestedParticle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.NoFixedFacet",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Occurs",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Particle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Redefinable",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Schema",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SchemaTop",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleContent",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleDerivation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleExtension",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleRestriction",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleRestrictionModel",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleTypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TopLevelAttribute",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TopLevelElement",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TypeDefParticle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Union",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Wildcard",
            "com.sun.xml.txw2.TypedXmlWriter");
    private static final List<String> JAXB_RUNTIME_CLASSES = List.of("org.glassfish.jaxb.runtime.v2.ContextFactory",
            "com.sun.xml.internal.stream.XMLInputFactoryImpl",
            "com.sun.xml.internal.stream.XMLOutputFactoryImpl",
            "com.sun.org.apache.xpath.internal.functions.FuncNot",
            "org.glassfish.jaxb.core.v2.model.nav.ReflectionNavigator",
            "org.glassfish.jaxb.runtime.v2.runtime.property.SingleElementLeafProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.ArrayElementLeafProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.SingleElementNodeProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.SingleReferenceNodeProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.SingleMapNodeProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.ArrayElementNodeProperty",
            "org.glassfish.jaxb.runtime.v2.runtime.property.ArrayReferenceNodeProperty",
            "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl", XmlAccessOrder.class.getName());


    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        List<Class<?>> classes = getClassesByAnnotations(classLoader, JAXB_ROOT_ANNOTATIONS);
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(XmlSeeAlso.class)) {
                XmlSeeAlso annotation = c.getAnnotation(XmlSeeAlso.class);
                for (Class<?> type : annotation.value()) {
                    hints.reflection().registerType(type, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                }
            }
            applyIfMatch(c, XmlJavaTypeAdapter.class, XmlJavaTypeAdapter::value,
                    type -> hints.reflection().registerType(type, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS,
                            MemberCategory.DECLARED_FIELDS));
            hints.reflection().registerType(c, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.DECLARED_FIELDS);
        }
        boolean classDetected = false;
        for (String className : getClassesFromIndexes(classLoader)) {
            registerClassHierarchy(hints, classLoader, className, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS
            );
            classDetected = true;
        }
        if (classes.isEmpty() && !classDetected) {
            return;
        }
        // Register all JAXB indexes
        hints.resources().registerPattern("*/jaxb.index");

        hints.reflection().registerTypeIfPresent(classLoader, "jakarta.xml.bind.annotation.W3CDomHandler",
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        for (Class<?> c : JAXB_ANNOTATIONS) {
            hints.reflection().registerType(c, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
        hints.proxies().registerJdkProxy(TypeReference.of(XmlSeeAlso.class), TypeReference.of("org.glassfish.jaxb.core.v2.model.annotation.Locatable"));
        for (String className : NATIVE_PROXY_DEFINITIONS) {
            hints.proxies().registerJdkProxy(TypeReference.of(className));
        }
        for (String className : JAXB_RUNTIME_CLASSES) {
            hints.reflection().registerTypeIfPresent(classLoader, className,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
        // Register the JAXB resource bundles
        hints.reflection().registerTypeIfPresent(classLoader, "jakarta.xml.bind.Messages");
        hints.resources().registerPattern("jakarta/xml/bind/Messages.properties");
        hints.reflection().registerTypeIfPresent(classLoader, "jakarta.xml.bind.helpers.Messages");
        hints.resources().registerPattern("jakarta/xml/bind/helpers/Messages.properties");
    }

    private static List<String> getClassesFromIndexes(ClassLoader classLoader) {
        List<String> classNames = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        try {
            for (Resource resource : resolver.getResources("classpath*:**/jaxb.index")) {
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                String packageName = getPackageName(resource, "jaxb.index");
                if (packageName == null) {
                    LOG.debug("The package name could not be found for the resource {}", resource);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new StringReader(resource.getContentAsString(StandardCharsets.UTF_8)))) {
                    String line = reader.readLine();
                    while (line != null) {
                        if (line.startsWith("#") || line.isBlank()) {
                            line = reader.readLine();
                            continue;
                        }
                        String className = "%s%s".formatted(packageName, line.trim());
                        LOG.debug("Found the class {} to register", className);
                        classNames.add(className);
                        line = reader.readLine();
                    }
                }
            }
        } catch (IOException e) {
            LOG.debug("Could not load the JAXB indexes: {}", e.getMessage());
        }
        return classNames;
    }

    /**
     * Give the package name of the given resource.
     *
     * @param resource the resource for which the package name is expected.
     * @param fileName the name of file corresponding to the resource
     * @return the package name if it could be found, {@code null} otherwise.
     * @throws IOException an error occurs while trying to retrieve the package name.
     */
    private static String getPackageName(Resource resource, String fileName) throws IOException {
        URL url = resource.getURL();
        String protocol = url.getProtocol();
        String packageName = null;
        if ("jar".equals(protocol)) {
            String path = url.getPath();
            String suffix = ".jar!/";
            int index = path.indexOf(suffix);
            if (index == -1) {
                LOG.trace("The jar suffix could not be found in {}", path);
            } else {
                packageName = path.substring(index + suffix.length(), path.length() - fileName.length());
            }
        } else if (resource.isFile()) {
            File file = resource.getFile();
            File[] files = file.getParentFile().listFiles((dir, name) -> name.endsWith(".class"));
            if (files != null && files.length > 0) {
                try (InputStream is = new FileInputStream(files[0])) {
                    ClassReader reader = new ClassReader(is);
                    String className = reader.getClassName();
                    int index = className.lastIndexOf('/');
                    if (index == -1) {
                        packageName = "";
                    } else {
                        packageName = className.substring(0, index + 1);
                    }
                }
            } else {
                LOG.trace("No class file could be found in {}", file.getParentFile());
            }
        }
        if (packageName != null) {
            packageName = packageName.replace('/', '.');
        }
        return packageName;
    }
}
