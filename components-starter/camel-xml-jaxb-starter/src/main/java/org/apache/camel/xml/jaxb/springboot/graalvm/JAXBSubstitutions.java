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
package org.apache.camel.xml.jaxb.springboot.graalvm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlSeeAlso;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.runtime.v2.model.annotation.LocatableAnnotation;
import org.glassfish.jaxb.runtime.v2.model.annotation.RuntimeInlineAnnotationReader;

public class JAXBSubstitutions {
}

@TargetClass(RuntimeInlineAnnotationReader.class)
final class SubstituteRuntimeInlineAnnotationReader {

    @Alias
    private Map<Class<? extends Annotation>,Map<Package,Annotation>> packageCache;

    @Substitute
    public <A extends Annotation> A getFieldAnnotation(Class<A> annotation, Field field, Locatable srcPos) {
        return field.getAnnotation(annotation);
    }

    @Substitute
    public Annotation[] getAllFieldAnnotations(Field field, Locatable srcPos) {
        return field.getAnnotations();
    }

    @Substitute
    public <A extends Annotation> A getClassAnnotation(Class<A> a, Class clazz, Locatable srcPos) {
        A ann = ((Class<?>) clazz).getAnnotation(a);
        return (ann != null && ann.annotationType() == XmlSeeAlso.class) ? LocatableAnnotation.create(ann, srcPos) : ann;
    }

    @Substitute
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotation, Method method, Locatable srcPos) {
        return method.getAnnotation(annotation);
    }

    @Substitute
    public Annotation[] getAllMethodAnnotations(Method method, Locatable srcPos) {
        return method.getAnnotations();
    }

    @Substitute
    public <A extends Annotation> A getMethodParameterAnnotation(Class<A> annotation, Method method, int paramIndex,
                                                                 Locatable srcPos) {
        Annotation[] pa = method.getParameterAnnotations()[paramIndex];
        for(Annotation a : pa) {
            if (a.annotationType() == annotation)
                return (A) a;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Substitute
    public <A extends Annotation> A getPackageAnnotation(Class<A> a, Class clazz, Locatable srcPos) {
        Package p = clazz.getPackage();
        if (p == null) {
            return null;
        }

        Map<Package, Annotation> cache = packageCache.get(a);
        if (cache == null) {
            cache = new HashMap<>();
            packageCache.put(a, cache);
        }
        if (cache.containsKey(p)) {
            return (A) cache.get(p);
        } else {
            A ann = p.getAnnotation(a);
            cache.put(p, ann);
            return ann;
        }
    }
}
