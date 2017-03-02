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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

final class ClassModelImpl<T> implements ClassModel<T> {
    private final Class<T> type;
    private final ClassAccessorFactory<T> classAccessorFactory;
    private final boolean discriminatorEnabled;
    private final String discriminatorKey;
    private final String discriminator;
    private final FieldModel<?> idField;
    private final List<FieldModel<?>> fieldModels;
    private final List<String> genericFieldNames;
    private final Map<String, FieldModel<?>> fieldMap;

    ClassModelImpl(final Class<T> clazz, final List<String> genericFieldNames, final ClassAccessorFactory<T> classAccessorFactory,
                   final Boolean discriminatorEnabled, final String discriminatorKey, final String discriminator,
                   final FieldModel<?> idField, final List<FieldModel<?>> fieldModels) {
        this.type = clazz;
        this.genericFieldNames = genericFieldNames;
        this.classAccessorFactory = classAccessorFactory;
        this.discriminatorEnabled = discriminatorEnabled;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
        this.idField = idField;
        this.fieldModels = fieldModels;
        this.fieldMap = generateFieldMap(fieldModels);
    }

    public List<String> getGenericFieldNames() {
        return genericFieldNames;
    }

    @Override
    public ClassAccessor<T> getClassAccessor() {
        return classAccessorFactory.create(this);
    }

    @Override
    public ClassAccessorFactory<T> getClassAccessorFactory() {
        return classAccessorFactory;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean useDiscriminator() {
        return discriminatorEnabled;
    }

    @Override
    public String getDiscriminatorKey() {
        return discriminatorKey;
    }

    @Override
    public String getDiscriminator() {
        return discriminator;
    }

    @Override
    public FieldModel<?> getFieldModel(final String documentFieldName) {
        return idField != null && documentFieldName.equals(idField.getDocumentFieldName()) ? idField : fieldMap.get(documentFieldName);
    }

    @Override
    public List<FieldModel<?>> getFieldModels() {
        return unmodifiableList(fieldModels);
    }

    @Override
    public FieldModel<?> getIdFieldModel() {
        return idField;
    }

    @Override
    public String getName() {
        return getType().getSimpleName();
    }

    @Override
    public String toString() {
        return "ClassModelImpl{"
                + "type=" + type
                + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassModelImpl)) {
            return false;
        }

        ClassModelImpl<?> that = (ClassModelImpl<?>) o;

        if (discriminatorEnabled != that.discriminatorEnabled) {
            return false;
        }
        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) {
            return false;
        }
        if (getClassAccessorFactory() != null ? !getClassAccessorFactory().equals(that.getClassAccessorFactory())
                : that.getClassAccessorFactory() != null) {
            return false;
        }
        if (getDiscriminatorKey() != null ? !getDiscriminatorKey().equals(that.getDiscriminatorKey())
                : that.getDiscriminatorKey() != null) {
            return false;
        }
        if (getDiscriminator() != null ? !getDiscriminator().equals(that.getDiscriminator()) : that.getDiscriminator() != null) {
            return false;
        }
        if (idField != null ? !idField.equals(that.idField) : that.idField != null) {
            return false;
        }
        if (!getFieldModels().equals(that.getFieldModels())) {
            return false;
        }
        if (getGenericFieldNames() != null ? !getGenericFieldNames().equals(that.getGenericFieldNames()) : that.getGenericFieldNames()
                != null) {
            return false;
        }
        if (fieldMap != null ? !fieldMap.equals(that.fieldMap) : that.fieldMap != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getType() != null ? getType().hashCode() : 0;
        result = 31 * result + (getClassAccessorFactory() != null ? getClassAccessorFactory().hashCode() : 0);
        result = 31 * result + (discriminatorEnabled ? 1 : 0);
        result = 31 * result + (getDiscriminatorKey() != null ? getDiscriminatorKey().hashCode() : 0);
        result = 31 * result + (getDiscriminator() != null ? getDiscriminator().hashCode() : 0);
        result = 31 * result + (idField != null ? idField.hashCode() : 0);
        result = 31 * result + getFieldModels().hashCode();
        result = 31 * result + (getGenericFieldNames() != null ? getGenericFieldNames().hashCode() : 0);
        result = 31 * result + (fieldMap != null ? fieldMap.hashCode() : 0);
        return result;
    }

    private static Map<String, FieldModel<?>> generateFieldMap(final List<FieldModel<?>> fieldModels) {
        Map<String, FieldModel<?>> fieldMap = new HashMap<String, FieldModel<?>>();
        for (FieldModel<?> fieldModel : fieldModels) {
            fieldMap.put(fieldModel.getDocumentFieldName(), fieldModel);
        }
        return fieldMap;
    }
}
