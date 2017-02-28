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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;


final class ClassModelImpl<T> implements ClassModel<T> {
    private final Map<String, FieldModel<?>> fieldMap;
    private final List<FieldModel<?>> fieldModels;
    private final Class<T> type;
    private final FieldModel<?> idField;
    private final boolean discriminatorEnabled;
    private final String discriminatorKey;
    private final String discriminator;
    private final ClassAccessorFactory<T> classAccessorFactory;


    ClassModelImpl(final Class<T> clazz, final ClassAccessorFactory<T> classAccessorFactory, final Boolean discriminatorEnabled,
                   final String discriminatorKey, final String discriminator, final FieldModel<?> idField,
                   final List<FieldModel<?>> fieldModels) {
        this.fieldMap = new HashMap<String, FieldModel<?>>();
        this.fieldModels = new ArrayList<FieldModel<?>>();
        this.idField = idField;
        this.discriminatorEnabled = discriminatorEnabled;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
        this.type = clazz;
        this.classAccessorFactory = classAccessorFactory;

        for (FieldModel<?> fieldModel : fieldModels) {
            addField(fieldModel);
        }
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
        return format("ClassModel<%s>", getName());
    }

    private void addField(final FieldModel<?> fieldModel) {
        fieldMap.put(fieldModel.getDocumentFieldName(), fieldModel);
        fieldModels.add(fieldModel);
    }
}
