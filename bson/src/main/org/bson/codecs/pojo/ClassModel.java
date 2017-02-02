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

import java.util.List;

/**
 * This model represents the metadata for a class and all its fields.
 *
 * @param <T> The type of the class the ClassModel represents
 * @since 3.5
 */
public interface ClassModel<T> {

    /**
     * @return a new ClassAccessor instance for the ClassModel
     */
    ClassAccessor<T> getClassAccessor();

    /**
     * @return the class model ClassFactoryFactory
     */
    ClassAccessorFactory<T> getClassAccessorFactory();

    /**
     * @return the backing class for the MappedType
     */
    Class<T> getType();

    /**
     * @return true if a discriminator should be used when storing the data.
     */
    boolean useDiscriminator();

    /**
     * Gets the value for the discriminator.
     *
     * @return the discriminator value or null if not set
     */
    String getDiscriminator();

    /**
     * Returns the discriminator key.
     *
     * @return the discriminator key or null if not set
     */
    String getDiscriminatorKey();

    /**
     * Gets a field by the document field name.
     *
     * @param documentFieldName the fieldModel's document name
     * @return the field or null if the field is not found
     */
    FieldModel<?> getFieldModel(String documentFieldName);

    /**
     * Returns all the fields on this model
     *
     * @return the list of fields
     */
    List<FieldModel<?>> getFieldModels();

    /**
     * Returns the {@link FieldModel} mapped as the id field for this ClassModel
     *
     * @return the FieldModel for the id
     */
    FieldModel<?> getIdFieldModel();

    /**
     * Returns the name of the class represented by this ClassModel
     *
     * @return the name
     */
    String getName();

}
