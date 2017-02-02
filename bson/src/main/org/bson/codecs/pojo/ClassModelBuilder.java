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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;
import static org.bson.codecs.pojo.Conventions.DEFAULT_CONVENTIONS;
import static org.bson.codecs.pojo.PojoHelper.configureClassModelBuilder;
import static org.bson.codecs.pojo.PojoHelper.stateNotNull;

/**
 * A builder for programmatically creating {@code ClassModels}.
 *
 * @param <T> The type of the class the ClassModel represents
 * @since 3.5
 * @see ClassModel
 */
public class ClassModelBuilder<T> {
    private static final String ID_FIELD_NAME = "_id";
    private final List<FieldModelBuilder<?>> fields = new ArrayList<FieldModelBuilder<?>>();
    private ClassAccessorFactory<T> classAccessorFactory;
    private Class<T> type;
    private List<Convention> conventions = DEFAULT_CONVENTIONS;
    private List<Annotation> annotations = Collections.emptyList();
    private Boolean discriminatorEnabled;
    private String discriminator;
    private String discriminatorKey;
    private String idField;

    /**
     * Creates a new ClassModelBuilder instance
     */
    public ClassModelBuilder() {
    }

    /**
     * Creates a new ClassModelBuilder instance using reflection on the {@code clazz}.
     *
     * @param clazz the POJO to reflect and configure the builder with.
     */
    public ClassModelBuilder(final Class<T> clazz) {
        configureClassModelBuilder(this, clazz);
    }

    /**
     * Sets the ClassAccessorFactory for the ClassModel
     *
     * @param classAccessorFactory the ClassAccessorFactory
     * @return this
     */
    public ClassModelBuilder<T> classAccessorFactory(final ClassAccessorFactory<T> classAccessorFactory) {
        this.classAccessorFactory = notNull("classFactoryFactory", classAccessorFactory);
        return this;
    }

    /**
     * @return the ClassAccessorFactory for the ClassModel
     */
    public ClassAccessorFactory<T> getClassAccessorFactory() {
        return classAccessorFactory;
    }

    /**
     * Sets the type of the model
     *
     * @param type the type of the class
     * @return the builder to configure the class being modeled
     */
    public ClassModelBuilder<T> type(final Class<T> type) {
        this.type = notNull("type", type);
        return this;
    }

    /**
     * @return the type if set or null
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Sets the conventions to apply to the model
     *
     * @param conventions a list of conventions
     * @return this
     */
    public ClassModelBuilder<T> conventions(final List<Convention> conventions) {
        this.conventions = notNull("conventions", conventions);
        return this;
    }

    /**
     * @return the conventions o apply to the model
     */
    public List<Convention> getConventions() {
        return conventions;
    }

    /**
     * Sets the annotations for the model
     *
     * @param annotations a list of annotations
     * @return this
     */
    public ClassModelBuilder<T> annotations(final List<Annotation> annotations) {
        this.annotations = notNull("annotations", annotations);
        return this;
    }

    /**
     * @return the annotations on the modeled type if set or null
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Sets the discriminator to be used when storing instances of the modeled type
     *
     * @param discriminator the discriminator value
     * @return this
     */
    public ClassModelBuilder<T> discriminator(final String discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    /**
     * @return the discriminator to be used when storing instances of the modeled type or null if not set
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Sets the discriminator key to be used when storing instances of the modeled type
     *
     * @param discriminatorKey the discriminator key value
     * @return this
     */
    public ClassModelBuilder<T> discriminatorKey(final String discriminatorKey) {
        this.discriminatorKey = discriminatorKey;
        return this;
    }

    /**
     * @return the discriminator key to be used when storing instances of the modeled type or null if not set
     */
    public String getDiscriminatorKey() {
        return discriminatorKey;
    }

    /**
     * Enables or disables the use of a discriminator when serializing
     *
     * @param discriminatorEnabled the discriminatorEnabled value
     * @return this
     */
    public ClassModelBuilder<T> discriminatorEnabled(final boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * @return true if a discriminator should be used when serializing, otherwise false
     */
    public Boolean isDiscriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * Designates a field as the {@code _id} field for this type.  If another field is currently marked as the  {@code _id} field,
     * that setting is cleared in favor of the named field.
     *
     * @param idField the FieldModel field name to use for the {@code _id} field
     * @return this
     */
    public ClassModelBuilder<T> idField(final String idField) {
        this.idField = notNull("idField", idField);
        return this;
    }

    /**
     * @return the designated {@code _id} field for this type or null if not set
     */
    public String getIdField() {
        return idField;
    }

    /**
     * Adds a FieldModelBuilder to the ClassModelBuilder
     *
     * @param fieldModelBuilder the fieldModelBuilder
     * @return this
     */
    public ClassModelBuilder<T> addField(final FieldModelBuilder<?> fieldModelBuilder) {
        fields.add(notNull("fieldModelBuilder", fieldModelBuilder));
        return this;
    }

    /**
     * Remove a field from the builder
     *
     * @param name the field to remove
     * @return returns the FieldModelBuilder if there is a matching FieldModelBuilder with the same name, otherwise returns null
     */
    public FieldModelBuilder<?> removeField(final String name) {
        notNull("name", name);
        FieldModelBuilder<?> fieldModelBuilder = getField(name);
        fields.remove(fieldModelBuilder);
        return fieldModelBuilder;
    }

    /**
     * Gets a field by the given name.
     *
     * <p>
     * Note: Searches against the actual field name in the POJO and not the {@code documentFieldName}.
     * </p>
     *
     * @param name the name of the field to find.
     * @return the field or null if the field is not found
     */
    public FieldModelBuilder<?> getField(final String name) {
        notNull("name", name);
        for (FieldModelBuilder<?> fieldModelBuilder : fields) {
            if (fieldModelBuilder.getFieldName().equals(name)) {
                return fieldModelBuilder;
            }
        }
        return null;
    }

    /**
     * @return the fields on the modeled type
     */
    public List<FieldModelBuilder<?>> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Creates a new ClassModel instance based on the mapping data provided.
     *
     * @return the new instance
     */
    public ClassModel<T> build() {
        List<FieldModel<?>> fieldModels = new ArrayList<FieldModel<?>>();
        FieldModel<?> idFieldModel = null;

        stateNotNull("type", type);
        for (Convention convention : conventions) {
            convention.apply(this);
        }

        stateNotNull("classAccessorFactory", classAccessorFactory);
        boolean useDiscriminator = discriminatorEnabled != null ? discriminatorEnabled : false;
        if (useDiscriminator) {
            stateNotNull("discriminatorKey", discriminatorKey);
            stateNotNull("discriminator", discriminator);
        }

        for (FieldModelBuilder<?> fieldModelBuilder : fields) {
            boolean isIdField = fieldModelBuilder.getFieldName().equals(idField);
            if (isIdField) {
                fieldModelBuilder.documentFieldName(ID_FIELD_NAME);
            }

            FieldModel<?> model = fieldModelBuilder.build();
            fieldModels.add(model);
            if (isIdField) {
                idFieldModel = model;
            }
        }
        validateFieldModels(fieldModels);
        return new ClassModelImpl<T>(type, classAccessorFactory, useDiscriminator, discriminatorKey, discriminator, idFieldModel,
                fieldModels);
    }

    @Override
    public String toString() {
        return format("ClassModelBuilder{type=%s}", type);
    }

    private void validateFieldModels(final List<FieldModel<?>> fieldModels) {
        Map<String, Integer> fieldNameMap = new HashMap<String, Integer>();
        Map<String, Integer> fieldDocumentNameMap = new HashMap<String, Integer>();
        String duplicateFieldName = null;
        String duplicateDocumentFieldName = null;

        for (FieldModel<?> fieldModel : fieldModels) {
            String fieldName = fieldModel.getFieldName();
            if (fieldNameMap.containsKey(fieldName)) {
                duplicateFieldName = fieldName;
                break;
            }
            fieldNameMap.put(fieldName, 1);

            String documentFieldName = fieldModel.getDocumentFieldName();
            if (fieldDocumentNameMap.containsKey(documentFieldName)) {
                duplicateDocumentFieldName = documentFieldName;
                break;
            }
            fieldDocumentNameMap.put(documentFieldName, 1);
        }
        if (idField != null && !fieldNameMap.containsKey(idField)) {
            throw new IllegalStateException(format("Invalid id field, field named field '%s' can not be found.", idField));
        } else if (duplicateFieldName != null) {
            throw new IllegalStateException(format("Duplicate field named '%s' found.", duplicateFieldName));
        } else if (duplicateDocumentFieldName != null) {
            throw new IllegalStateException(format("Duplicate document field named '%s' found.", duplicateDocumentFieldName));
        }
    }

}
