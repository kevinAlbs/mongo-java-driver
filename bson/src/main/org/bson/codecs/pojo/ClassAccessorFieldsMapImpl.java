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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

final class ClassAccessorFieldsMapImpl<T> implements ClassAccessor<T> {
    private final ClassModel<T> classModel;
    private final List<Constructor<T>> constructors;
    private final Map<String, Object> fieldNameToValueMap;

    ClassAccessorFieldsMapImpl(final ClassModel<T> classModel,
                               final List<Constructor<T>> constructors) {
        this.classModel = classModel;
        this.constructors = constructors;
        this.fieldNameToValueMap = new HashMap<String, Object>();
    }

    @Override
    public <S> S get(final T instance, final FieldModel<S> fieldModel) {
        return fieldModel.getFieldAccessor().get(instance);
    }

    @Override
    public <S> void set(final S value, final FieldModel<S> fieldModel) {
        fieldNameToValueMap.put(fieldModel.getFieldName(), value);
    }

    @Override
    public T create() {
        List<Object> modelArgs = new ArrayList<Object>();
        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            Object value = fieldNameToValueMap.get(fieldModel.getDocumentFieldName());
            modelArgs.add(value);
        }

        Constructor<T> constructor = null;
        for (Constructor<T> possibleConstructor : constructors) {
            if (possibleConstructor.isVarArgs()) {
                continue;
            }
            constructor = possibleConstructor;
            if (constructor.getParameterTypes().length >= modelArgs.size()) {
                break;
            }
        }

        if (constructor == null) {
            throw new CodecConfigurationException(format("Cannot find suitable constructor for: %s", classModel.getType()));
        }

        try {
            return constructor.newInstance(modelArgs.toArray(new Object[modelArgs.size()]));
        } catch (Exception e) {
            throw new CodecConfigurationException(format("Unable to create new instance of '%s' with the following arguments: %s",
                    classModel.getType(), modelArgs));
        }
    }
}
