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
import java.lang.reflect.Field;
import java.util.Map;

import static java.lang.String.format;

final class ClassAccessorInstanceImpl<T> implements ClassAccessor<T> {
    private final ClassModel<T> classModel;
    private final Map<String, Field> fieldsMap;
    private final Constructor<T> constructor;
    private T newInstance;

    ClassAccessorInstanceImpl(final ClassModel<T> classModel, final Map<String, Field> fieldsMap, final Constructor<T> constructor) {
        this.classModel = classModel;
        this.fieldsMap = fieldsMap;
        this.constructor = constructor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S get(final T instance, final FieldModel<S> fieldModel) {
        try {
            return (S) getField(fieldModel.getFieldName()).get(instance);
        } catch (final IllegalAccessException e) {
            throw new CodecConfigurationException(format("Unable to get value for field: %s", fieldModel.getFieldName()), e);
        } catch (final IllegalArgumentException e) {
            throw new CodecConfigurationException(format("Unable to get value for field: %s", fieldModel.getFieldName()), e);
        }
    }

    @Override
    public <S> void set(final S value, final FieldModel<S> fieldModel) {
        try {
            getField(fieldModel.getFieldName()).set(getInstance(), value);
        } catch (final IllegalAccessException e) {
            throw new CodecConfigurationException(format("Unable to set value '%s' for field: %s", value, fieldModel.getFieldName()), e);
        } catch (final IllegalArgumentException e) {
            throw new CodecConfigurationException(format("Unable to set value '%s' for field: %s", value, fieldModel.getFieldName()), e);
        }
    }

    @Override
    public T create() {
        return newInstance;
    }

    private Field getField(final String fieldName) {
        Field field = fieldsMap.get(fieldName);
        if (field == null) {
            throw new CodecConfigurationException(format("ClassModel: %s does not contain field: %s", classModel, fieldName));
        }
        return field;
    }

    private T getInstance() {
        if (this.newInstance == null) {
            this.constructor.setAccessible(true);
            try {
                this.newInstance = constructor.newInstance();
            } catch (Exception e) {
                throw new CodecConfigurationException(e.getMessage(), e);
            }
        }
        return this.newInstance;
    }
}
