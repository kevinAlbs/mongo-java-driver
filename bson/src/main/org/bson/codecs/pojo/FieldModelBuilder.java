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

import org.bson.codecs.Codec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.bson.assertions.Assertions.notNull;
import static org.bson.codecs.pojo.PojoBuilderHelper.configureFieldModelBuilder;
import static org.bson.codecs.pojo.PojoBuilderHelper.stateNotNull;

/**
 * A builder for programmatically creating {@code FieldModels}.
 *
 * @param <T> the type of the field
 * @since 3.5
 * @see FieldModel
 */
public final class FieldModelBuilder<T> {
    private String fieldName;
    private String documentFieldName;
    private Class<T> type;
    private FieldModelSerialization<T> fieldModelSerialization;
    private Codec<T> codec;
    private List<Annotation> annotations = emptyList();
    private List<Class<?>> typeParameters = emptyList();
    private boolean discriminatorEnabled = true;

    /**
     * Creates a new FieldModelBuilder instance
     */
    public FieldModelBuilder() {
    }

    /**
     * Creates a new FieldModelBuilder instance using reflection on the {@code field}.
     *
     * @param field the field to reflect and configure the builder with.
     */
    public FieldModelBuilder(final Field field) {
        configureFieldModelBuilder(this, field);
    }

    /**
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the field name
     *
     * @param fieldName the field name
     * @return this
     */
    public FieldModelBuilder<T> fieldName(final String fieldName) {
        this.fieldName = notNull("fieldName", fieldName);
        return this;
    }

    /**
     * @return the field name
     */
    public String getDocumentFieldName() {
        return documentFieldName;
    }

    /**
     * Sets the document field name as it will be stored in the database.
     *
     * @param documentFieldName the document field name
     * @return this
     */
    public FieldModelBuilder<T> documentFieldName(final String documentFieldName) {
        this.documentFieldName = notNull("documentFieldName", documentFieldName);
        return this;
    }

    /**
     * @return the type
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Sets a custom codec for the field
     *
     * @param codec the custom codec for the field
     * @return this
     */
    public FieldModelBuilder<T> codec(final Codec<T> codec) {
        this.codec = codec;
        return this;
    }

    /**
     * @return the custom codec to use if set or null
     */
    Codec<T> getCodec() {
        return codec;
    }

    /**
     * Sets the type for the field
     *
     * @param type the type for the field
     * @return this
     */
    public FieldModelBuilder<T> type(final Class<T> type) {
        this.type = notNull("type", type);
        return this;
    }

    /**
     * Sets the {@link FieldModelSerialization} checker
     *
     * @param fieldModelSerialization checks if a field should be serialized
     * @return this
     */
    public FieldModelBuilder<T> fieldModelSerialization(final FieldModelSerialization<T> fieldModelSerialization) {
        this.fieldModelSerialization = notNull("fieldModelSerializationChecker", fieldModelSerialization);
        return this;
    }

    /**
     * @return the {@link FieldModelSerialization} checker if set or null
     */
    public FieldModelSerialization<T> getFieldModelSerialization() {
        return fieldModelSerialization;
    }

    /**
     * Returns the annotations
     *
     * @return the annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Sets the annotations
     *
     * @param annotations the annotations
     * @return this
     */
    public FieldModelBuilder<T> annotations(final List<Annotation> annotations) {
        this.annotations = unmodifiableList(notNull("annotations", annotations));
        return this;
    }

    /**
     * Returns the typeParameters
     *
     * @return the typeParameters
     */
    public List<Class<?>> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Sets the typeParameters
     *
     * @param typeParameters the typeParameters
     * @return this
     */
    public FieldModelBuilder<T> typeParameters(final List<Class<?>> typeParameters) {
        this.typeParameters = unmodifiableList(notNull("typeParameters", new ArrayList<Class<?>>(typeParameters)));
        return this;
    }

    /**
     * @return true if a discriminator should be used when serializing, otherwise false
     */
    public boolean isDiscriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * Enables or disables the use of a discriminator when serializing
     *
     * @param discriminatorEnabled the useDiscriminator value
     * @return this
     */
    public FieldModelBuilder<T> discriminatorEnabled(final boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * Creates the FieldModel from the FieldModelBuilder.
     * @return the fieldModel
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FieldModel<T> build() {
        return new FieldModelImpl(
                stateNotNull("fieldName", fieldName),
                stateNotNull("documentFieldName", documentFieldName),
                boxType(stateNotNull("type", type)),
                boxTypes(typeParameters),
                codec,
                stateNotNull("fieldModelSerialization", fieldModelSerialization),
                discriminatorEnabled);
    }

    @Override
    public String toString() {
        return format("FieldModelBuilder{type=%s, fieldName=%s}", type, fieldName);
    }

    private Class<?> boxType(final Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return PRIMITIVE_CLASS_MAP.get(clazz);
        } else {
            return clazz;
        }
    }

    private List<Class<?>> boxTypes(final List<Class<?>> clazzes) {
        List<Class<?>> boxedTypes = new ArrayList<Class<?>>();
        for (Class<?> clazz : clazzes) {
            boxedTypes.add(boxType(clazz));
        }
        return boxedTypes;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASS_MAP;
    static {
        Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
        map.put(boolean.class, Boolean.class);
        map.put(byte.class, Byte.class);
        map.put(char.class, Character.class);
        map.put(double.class, Double.class);
        map.put(float.class, Float.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(short.class, Short.class);
        PRIMITIVE_CLASS_MAP = Collections.unmodifiableMap(map);
    }

}
