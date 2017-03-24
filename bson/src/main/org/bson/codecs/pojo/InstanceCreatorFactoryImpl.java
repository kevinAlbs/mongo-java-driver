/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo;

import org.bson.codecs.configuration.CodecConfigurationException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.String.format;

final class InstanceCreatorFactoryImpl<T> implements InstanceCreatorFactory<T> {
    private final String className;
    private final Constructor<T> headConstructor;
    private final boolean hasNoArgsConstructor;

    InstanceCreatorFactoryImpl(final String className, final List<Constructor<T>> constructors) {
        this.className = className;
        List<Constructor<T>> sortedConstructors = new ArrayList<Constructor<T>>(constructors);
        Collections.sort(sortedConstructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(final Constructor<?> o1, final Constructor<?> o2) {
                int o1l = o1.getParameterTypes().length;
                int o2l = o2.getParameterTypes().length;
                return (o1l < o2l) ? -1 : ((o1l == o2l) ? 0 : 1);
            }
        });
        this.headConstructor = sortedConstructors.size() > 0 ? sortedConstructors.get(0) : null;
        if (headConstructor != null) {
            headConstructor.setAccessible(true);
        }
        this.hasNoArgsConstructor = headConstructor != null && headConstructor.getParameterTypes().length == 0;
    }

    @Override
    public InstanceCreator<T> create() {
        if (!hasNoArgsConstructor) {
            throw new CodecConfigurationException(format("Cannot find a no-arg constructor for '%s'. Either create one or "
                    + "provide your own InstanceCreatorFactory.", className));
        }
        return new InstanceCreatorInstanceImpl<T>(headConstructor);
    }
}
