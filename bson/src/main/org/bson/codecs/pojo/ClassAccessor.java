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

/**
 * Provides access for getting and setting class data and also the creation of new class instances.
 *
 * @param <T> the type of the class
 * @since 3.5
 */
public interface ClassAccessor<T> {

    /**
     * Gets the value for the given FieldModel from the class instance.
     *
     * @param instance   the class instance to retrieve the value from.
     * @param fieldModel the FieldModel representing the field to get the value for.
     * @param <S>        the FieldModel's type
     * @return the value of the field for the class instance.
     */
    <S> S get(T instance, FieldModel<S> fieldModel);

    /**
     * Sets a value for the given FieldModel
     *
     * @param value      the new value for the field
     * @param fieldModel the FieldModel representing the field to set the value for.
     * @param <S>        the FieldModel's type
     */
    <S> void set(S value, FieldModel<S> fieldModel);

    /**
     * Creates a new instance of the class.
     * <p>Note: This will be called after all the values have been set.</p>
     *
     * @return the new class instance.
     */
    T create();

}
