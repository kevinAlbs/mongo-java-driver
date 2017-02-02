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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class ClassAccessorFactoryImpl<T> implements ClassAccessorFactory<T> {

    private final Map<String, Field> fieldsMap;
    private final List<Constructor<T>> constructors;

    ClassAccessorFactoryImpl(final List<Constructor<T>> constructors, final Map<String, Field> fieldsMap) {
        this.fieldsMap = fieldsMap;
        List<Constructor<T>> sortedConstructors = new ArrayList<Constructor<T>>(constructors);
        Collections.sort(sortedConstructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(final Constructor<?> o1, final Constructor<?> o2) {
                int o1l = o1.getParameterTypes().length;
                int o2l = o2.getParameterTypes().length;
                return (o1l < o2l) ? -1 : ((o1l == o2l) ? 0 : 1);
            }
        });
        this.constructors = sortedConstructors;
    }

    @Override
    public ClassAccessor<T> create(final ClassModel<T> classModel) {
        return constructors.get(0).getParameterTypes().length == 0
                ? new ClassAccessorInstanceImpl<T>(classModel, fieldsMap, constructors.get(0))
                : new ClassAccessorFieldsMapImpl<T>(classModel, fieldsMap, constructors);
    }
}
