/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geektimes.enterprise.inject.util;

import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.geektimes.enterprise.inject.util.Exceptions.newDefinitionException;

/**
 * The utilities class for Disposer Method
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public abstract class Disposers {

    private static final List<Class<? extends Annotation>> forbiddenParameterAnnotationTypes =
            asList(Observes.class, ObservesAsync.class);

    /**
     * A disposer method must be a default-access, public, protected or private, non-abstract method of
     * a managed bean class.
     * <p>
     * A disposer method may be either static or non-static.
     * <p>
     * A bean may declare multiple disposer methods.
     * <p>
     * Each disposer method must have exactly one disposed parameter, of the same type as the corresponding
     * producer method return type or producer field type.
     *
     * @param annotatedType the {@link AnnotatedType} of enabled bean
     * @return
     * @throws DefinitionException If the disposer method violate these rules :
     *                             <ul>
     *                                 <li>a method has more than one parameter annotated {@link Disposes @Disposes}</li>
     *                                 <li>a disposer method is annotated {@link Produces @Produces} or
     *                                 {@link Inject @Inject} has a parameter annotated
     *                                 {@link Observes @Observes} or has a parameter annotated {@link ObservesAsync @ObservesAsync}</li>
     *                                 <li>an interceptor or decorator has a method annotated {@link Disposes @Disposes}</li>
     *                             </ul>
     */
    public static Set<AnnotatedMethod> resolveDisposerMethods(AnnotatedType annotatedType) throws DefinitionException {
        Set<AnnotatedMethod> disposerMethods = new LinkedHashSet<>();

        Set<AnnotatedMethod> methods = annotatedType.getMethods();
        methods.forEach(method -> {
            List<AnnotatedParameter> parameters = method.getParameters();
            int disposesParamCount = 0;
            for (AnnotatedParameter parameter : parameters) {
                if (parameter.isAnnotationPresent(Disposes.class)) {
                    disposesParamCount++;
                }
            }
            if (disposesParamCount == 1) {
                disposerMethods.add(method);
            } else if (disposesParamCount > 1) {
                throw newDefinitionException("The method[%s] has more than one parameter annotated @%s",
                        method.getJavaMember(), Disposes.class);
            }
        });

        disposerMethods.forEach(Disposers::validateDisposerMethod);

        return unmodifiableSet(disposerMethods);
    }

    private static void validateDisposerMethod(AnnotatedMethod disposerMethod) {
        if (disposerMethod.isAnnotationPresent(Produces.class)) {
            throw newDefinitionException("The disposer method[%s] must not annotate @%s",
                    disposerMethod.getJavaMember(), Produces.class.getName());
        }
        if (disposerMethod.isAnnotationPresent(Inject.class)) {
            List<AnnotatedParameter> parameters = disposerMethod.getParameters();
            for (AnnotatedParameter parameter : parameters) {
                for (Class<? extends Annotation> forbiddenAnnotationType : forbiddenParameterAnnotationTypes) {
                    if (parameter.isAnnotationPresent(forbiddenAnnotationType)) {
                        throw newDefinitionException("The disposer method[%s] annotates @%s must not have a parameter annotated @%s",
                                disposerMethod.getJavaMember(), Inject.class.getName(), forbiddenAnnotationType.getName());
                    }
                }
            }
        }
    }
}
