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
package org.apache.camel.spring.boot.aot;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code ReflectionHelper} utility class providing methods needed for the native mode.
 */
public final class ReflectionHelper {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReflectionHelper.class);

    private ReflectionHelper() {

    }

    /**
     * Apply a specific action anytime the annotation is found in the class according to the type of target supported
     * by the annotation which can be either {@link ElementType#TYPE}, {@link ElementType#CONSTRUCTOR},
     * {@link ElementType#METHOD}, {@link ElementType#FIELD}, or {@link ElementType#PARAMETER}.
     *
     * @param c the target class
     * @param a the target type of annotation
     * @param getter the method allowing to extract the excepted values from the annotation
     * @param onMatch the action to perform in case of a match
     * @param <A> the type of the target annotation
     * @param <T> the type of the content extracted from the annotation found
     */
    public static <A extends Annotation, T> void applyIfMatch(Class<?> c, Class<A> a, Function<A, T> getter,
                                                              Consumer<T> onMatch) {
        Set<ElementType> targets = null;
        if (a.isAnnotationPresent(Target.class)) {
            targets = Collections.newSetFromMap(new EnumMap<>(ElementType.class));
            targets.addAll(Arrays.asList(a.getAnnotation(Target.class).value()));
        }
        if ((targets == null || targets.contains(ElementType.TYPE)) && c.isAnnotationPresent(a)) {
            onMatch.accept(getter.apply(c.getAnnotation(a)));
        }
        boolean checkConstructors = targets == null || targets.contains(ElementType.CONSTRUCTOR);
        boolean checkParameters = targets == null || targets.contains(ElementType.PARAMETER);
        if (checkConstructors || checkParameters) {
            for (Constructor<?> constructor : c.getDeclaredConstructors()) {
                if (checkConstructors && constructor.isAnnotationPresent(a)) {
                    onMatch.accept(getter.apply(constructor.getAnnotation(a)));
                }
                if (checkParameters) {
                    for (Parameter parameter : constructor.getParameters()) {
                        if (parameter.isAnnotationPresent(a)) {
                            onMatch.accept(getter.apply(parameter.getAnnotation(a)));
                        }
                    }
                }
            }
        }
        if (targets == null || targets.contains(ElementType.FIELD)) {
            ReflectionUtils.doWithFields(c,
                field -> onMatch.accept(getter.apply(field.getAnnotation(a))), field -> field.isAnnotationPresent(a));
        }
        boolean checkMethods = targets == null || targets.contains(ElementType.METHOD);
        if (checkMethods || checkParameters) {
            ReflectionUtils.doWithMethods(
                c,
                method -> {
                    if (checkMethods && method.isAnnotationPresent(a)) {
                        onMatch.accept(getter.apply(method.getAnnotation(a)));
                    }
                    if (checkParameters) {
                        for (Parameter parameter : method.getParameters()) {
                            if (parameter.isAnnotationPresent(a)) {
                                onMatch.accept(getter.apply(parameter.getAnnotation(a)));
                            }
                        }
                    }
                }
            );
        }
    }

    /**
     * Give all the classes available in the given class loader that are annotated with at least one of the annotations.
     *
     * @param classLoader the class loader from which the classes to find are loaded
     * @param annotations the target annotations
     * @return the list of classes that are annotated with at least one of the annotations.
     */
    public static List<Class<?>> getClassesByAnnotations(ClassLoader classLoader, List<Class<? extends Annotation>> annotations) {
        return getClassesByFilters(classLoader, annotations.stream().map(AnnotationTypeFilter::new).collect(Collectors.toList()));
    }

    /**
     * Give all the classes available in the given class loader that match with at least one of the filters.
     *
     * @param classLoader the class loader from which the classes to find are loaded
     * @param includeFilters the filters to apply the classes found
     * @return a list of classes that match with at least one of the given filters
     */
    public static List<Class<?>> getClassesByFilters(ClassLoader classLoader, List<TypeFilter> includeFilters) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        provider.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));
        provider.setMetadataReaderFactory(new SafeMetadataReaderFactory(provider.getMetadataReaderFactory()));
        provider.addExcludeFilter(
            (metadata, factory) -> {
                String className = metadata.getClassMetadata().getClassName();
                return className.startsWith("org.springframework.") || className.startsWith("java.") || className.startsWith("jakarta.");
            });
        for (TypeFilter filter : includeFilters) {
            provider.addIncludeFilter(filter);
        }
        return provider.findCandidateComponents("")
                .stream()
                .map(b -> asClass(b, classLoader))
                .collect(Collectors.toList());
    }

    /**
     * Convert the given bean definition into a class.
     * @param bean the bean definition to convert.
     * @param classLoader the classloader from which the class of the bean is loaded
     * @return the class corresponding to the bean definition if it could be found, {@code null} otherwise.
     */
    private static Class<?> asClass(BeanDefinition bean, ClassLoader classLoader) {
        String beanClassName = bean.getBeanClassName();
        if (beanClassName == null) {
            LOG.debug("The name of the class corresponding to the bean '{}' could not be found", bean);
        } else {
            try {
                return ClassUtils.forName(beanClassName, classLoader);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.debug("The class corresponding to the bean '{}' could not be found: {}", bean, e.getMessage());
            }
        }
        return null;
    }

    /**
     * {@code SafeMetadataReaderFactory} is a specific {@link MetadataReaderFactory} whose methods never throw any
     * exceptions, if an error occurs while calling the underlying {@link MetadataReaderFactory} a debug message is
     * logged and the default result is returned.
     */
    private static class SafeMetadataReaderFactory implements MetadataReaderFactory {

        /**
         * The instance of the default result in case of an error.
         */
        private static final MetadataReader DEFAULT = new MetadataReader() {
            @Override
            public Resource getResource() {
                return new ByteArrayResource(new byte[0]);
            }

            @Override
            public ClassMetadata getClassMetadata() {
                return AnnotationMetadata.introspect(Object.class);
            }

            @Override
            public AnnotationMetadata getAnnotationMetadata() {
                return AnnotationMetadata.introspect(Object.class);
            }
        };
        private final MetadataReaderFactory delegate;

        SafeMetadataReaderFactory(MetadataReaderFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public MetadataReader getMetadataReader(String className) {
            try {
                return delegate.getMetadataReader(className);
            } catch (Exception | NoClassDefFoundError e) {
                LOG.debug("Could not get the metadata of the class {}", className);
            }
            return DEFAULT;
        }

        @Override
        public MetadataReader getMetadataReader(Resource resource) {
            try {
                return delegate.getMetadataReader(resource);
            } catch (Exception | NoClassDefFoundError e) {
                LOG.debug("Could not get the metadata of the resource {}", resource);
            }
            return DEFAULT;
        }
    }
}
