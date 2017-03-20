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

final class ClassAccessorInstanceImpl<T> implements ClassAccessor<T> {
    private final Constructor<T> constructor;
    private T newInstance;

    ClassAccessorInstanceImpl(final Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public <S> S get(final T instance, final FieldModel<S> fieldModel) {
        return fieldModel.getFieldAccessor().get(instance);
    }

    @Override
    public <S> void set(final S value, final FieldModel<S> fieldModel) {
        fieldModel.getFieldAccessor().set(getInstance(), value);
    }

    @Override
    public T create() {
        return newInstance;
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
