/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bson.codecs.pojo;

import org.bson.codecs.Codec;

import java.util.List;

final class FieldModelImpl<T> implements FieldModel<T> {
    private final String fieldName;
    private final String documentFieldName;
    private final Class<T> type;
    private final List<Class<?>> typeParameters;
    private final Codec<T> codec;
    private final FieldModelSerialization<T> fieldModelSerialization;
    private final boolean useDiscriminator;

    FieldModelImpl(final String fieldName, final String documentFieldName, final Class<T> type, final List<Class<?>> typeParameters,
                   final Codec<T> codec, final FieldModelSerialization<T> fieldModelSerialization, final boolean useDiscriminator) {
        this.fieldName = fieldName;
        this.documentFieldName = documentFieldName;
        this.type = type;
        this.typeParameters = typeParameters;
        this.codec = codec;
        this.fieldModelSerialization = fieldModelSerialization;
        this.useDiscriminator = useDiscriminator;
    }

    @Override
    public boolean shouldSerialize(final T value) {
        return fieldModelSerialization.shouldSerialize(value);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getDocumentFieldName() {
        return documentFieldName;
    }

    @Override
    public Class<T> getFieldType() {
        return type;
    }

    @Override
    public List<Class<?>> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public Codec<T> getCodec() {
        return codec;
    }

    @Override
    public boolean useDiscriminator() {
        return useDiscriminator;
    }

    @Override
    public String toString() {
        return "FieldModel{"
                + "fieldName='" + fieldName + "'"
                + ", documentFieldName='" + documentFieldName + "'"
                + ", type=" + type
                + "}";
    }
}
