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

import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.diagnostics.Loggers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.PojoCodecHelper.getCodecFromDocument;


final class PojoCodec<T> implements Codec<T> {
    private static final Logger LOGGER = Loggers.getLogger("PojoCodec");
    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final DiscriminatorLookup discriminatorLookup;
    private final Map<String, Codec<?>> fieldCodecMap;

    @SuppressWarnings("unchecked")
    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.discriminatorLookup = discriminatorLookup;
        this.fieldCodecMap = new HashMap<String, Codec<?>>();
        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            Class<?> fieldType = fieldModel.getTypeData().getType();
            if (classModel.getType().equals(fieldType)) {
                fieldCodecMap.put(fieldModel.getFieldName(), specialisePojoCodec((FieldModel<T>) fieldModel, this));
            } else if (fieldType.equals(Object.class)) {
                fieldCodecMap.put(fieldModel.getFieldName(), getCodecOrNull(Object.class));
            } else {
                fieldCodecMap.put(fieldModel.getFieldName(), getCodec(fieldModel));
            }
        }
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        ClassAccessor<T> classAccessor = classModel.getClassAccessor();

        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        if (idFieldModel != null) {
            encodeField(writer, value, encoderContext, classAccessor, idFieldModel);
        }

        if (classModel.useDiscriminator()) {
            writer.writeString(classModel.getDiscriminatorKey(), classModel.getDiscriminator());
        }

        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            if (fieldModel == classModel.getIdFieldModel()) {
                continue;
            }
            encodeField(writer, value, encoderContext, classAccessor, fieldModel);
        }
        writer.writeEndDocument();
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        if (decoderContext.hasCheckedDiscriminator()) {
            ClassAccessor<T> classAccessor = classModel.getClassAccessor();
            decodeFields(reader, decoderContext, classAccessor);
            return classAccessor.create();
        } else {
            Codec<T> codec = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), registry,
                    discriminatorLookup, this);
            return codec.decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return classModel.getType();
    }

    @Override
    public String toString() {
        return format("PojoCodec<%s>", classModel);
    }

    ClassModel<T> getClassModel() {
        return classModel;
    }

    @SuppressWarnings("unchecked")
    private <S> void encodeField(final BsonWriter writer, final T instance, final EncoderContext encoderContext,
                                 final ClassAccessor<T> classAccessor, final FieldModel<S> fieldModel) {
        S fieldValue = classAccessor.get(instance, fieldModel);
        if (fieldModel.shouldSerialize(fieldValue)) {
            writer.writeName(fieldModel.getDocumentFieldName());
            if (fieldValue == null) {
                writer.writeNull();
            } else {
                getEncoderCodec(fieldModel, fieldValue.getClass()).encode(writer, fieldValue, encoderContext);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void decodeFields(final BsonReader reader, final DecoderContext decoderContext, final ClassAccessor<T> classAccessor) {
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodeFieldModel(reader, decoderContext, classAccessor, name, classModel.getFieldModel(name));
            }
        }
        reader.readEndDocument();
    }

    @SuppressWarnings("unchecked")
    private <S> void decodeFieldModel(final BsonReader reader, final DecoderContext decoderContext, final ClassAccessor<T> classAccessor,
                                      final String name, final FieldModel<S> fieldModel) {
        if (fieldModel != null) {
            try {
                S value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(getDecoderCodec(fieldModel), reader);
                }
                classAccessor.set(value, fieldModel);
            } catch (BsonInvalidOperationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. %s", name, e.getMessage()), e);
            } catch (CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. %s", name, e.getMessage()), e);
            }
        } else {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(format("Found field not present in the ClassModel: %s", name));
            }
            reader.skipValue();
        }
    }

    @SuppressWarnings("unchecked")
    private <S> Codec<S> getDecoderCodec(final FieldModel<S> fieldModel) {
        Codec<S> codec = (Codec<S>) fieldCodecMap.get(fieldModel.getFieldName());
        if (codec == null) {
            throw new CodecConfigurationException(format("Failed to decode '%s'. No codec for type: %s", fieldModel.getFieldName(),
                    fieldModel.getTypeData().getType()));
        }
        return codec;
    }

    @SuppressWarnings("unchecked")
    private <S, V> Codec<S> getEncoderCodec(final FieldModel<S> fieldModel, final Class<V> instanceType) {
        Codec<S> codec = (Codec<S>) fieldCodecMap.get(fieldModel.getFieldName());
        Class<S> fieldType = codec.getEncoderClass();
        if (fieldType != instanceType && fieldType.isAssignableFrom(instanceType)) {
            codec = specialisePojoCodec(fieldModel, getCodecOrNull((Class<S>) instanceType));
        }
        if (codec == null) {
            throw new CodecConfigurationException(format("Failed to encode '%s'. No codec for type: %s", fieldModel.getFieldName(),
                    fieldModel.getTypeData().getType()));
        }
        return codec;
    }

    private <S> Codec<S> getCodec(final FieldModel<S> fieldModel) {
        Codec<S> codec = fieldModel.getCodec();
        if (codec == null) {
            codec =  specialisePojoCodec(fieldModel, getCodecFromFieldTypeData(fieldModel.getTypeData()));
        }
        if (codec == null) {
            throw new CodecConfigurationException(format("No codec available for: %s: %s", fieldModel.getFieldName(),
                    fieldModel.getTypeData().getType().getSimpleName()));
        }
        return codec;
    }

    private <S> Codec<S> specialisePojoCodec(final FieldModel<S> fieldModel, final Codec<S> defaultCodec) {
        Codec<S> codec = defaultCodec;
        if (codec != null && codec instanceof PojoCodec) {
            ClassModel<S> original = ((PojoCodec<S>) codec).getClassModel();
            ClassModel<S> specialised = specialise(((PojoCodec<S>) codec).getClassModel(), fieldModel);
            if (!original.equals(specialised)) {
                codec = new PojoCodec<S>(specialised, registry, discriminatorLookup);
            }
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private  <S> Codec<S> getCodecFromFieldTypeData(final TypeData<S> typeData) {
        Codec<S> codec = null;
        Class<S> head = typeData.getType();

        if (Collection.class.isAssignableFrom(head)) {
            Codec<?> nestedCodec = getCodecFromFieldTypeData(typeData.getTypeParameters().get(0));
            codec = new CollectionCodec(head, nestedCodec);
        } else if (Map.class.isAssignableFrom(head)) {
            codec = new MapCodec(head, getCodecFromFieldTypeData(typeData.getTypeParameters().get(1)));
        } else {
            codec = getCodecOrNull(head);
        }
        return codec;
    }

    @SuppressWarnings("unchecked")
    private <S> Codec<S> getCodecOrNull(final Class<S> clazz) {
        Codec<S> codec = null;
        try {
            codec = registry.get(clazz);
        } catch (final CodecConfigurationException e) {
            // No codec found
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S, V> ClassModel<S> specialise(final ClassModel<S> clazzModel, final FieldModel<V> fieldModel) {
        boolean changeDiscriminator = !fieldModel.useDiscriminator() && clazzModel.useDiscriminator()
                || (fieldModel.useDiscriminator() && !clazzModel.useDiscriminator() && clazzModel.getDiscriminatorKey() != null
                && clazzModel.getDiscriminator() != null);
        if (clazzModel.getGenericFieldNames().isEmpty() && !changeDiscriminator){
            return clazzModel;
        }

        TypeData fieldTypeData = fieldModel.getTypeData();
        ArrayList<FieldModel<?>> concreteFieldModels = new ArrayList<FieldModel<?>>(clazzModel.getFieldModels());
        FieldModel<?> concreteIdField = clazzModel.getIdFieldModel();

        List<TypeData> fieldTypeParameters = fieldTypeData.getTypeParameters();
        for (int i = 0; i < concreteFieldModels.size(); i++) {
            FieldModel<?> model = concreteFieldModels.get(i);
            String fieldName = model.getFieldName();
            int index = clazzModel.getGenericFieldNames().indexOf(fieldName);
            if (index > -1 && index < fieldTypeParameters.size()) {
                FieldModel<?> concreteFieldModel = createFieldModelWithTypes(model, fieldTypeParameters.get(index));
                concreteFieldModels.set(i, concreteFieldModel);
                if (concreteIdField != null && concreteIdField.getFieldName().equals(fieldName)) {
                    concreteIdField = concreteFieldModel;
                }
            }
        }

        boolean useDiscriminator = changeDiscriminator ? fieldModel.useDiscriminator() : clazzModel.useDiscriminator();
        return new ClassModelImpl<S>(clazzModel.getType(), clazzModel.getGenericFieldNames(), clazzModel.getClassAccessorFactory(),
                useDiscriminator, clazzModel.getDiscriminatorKey(), clazzModel.getDiscriminator(), concreteIdField, concreteFieldModels);
    }

    private <V> FieldModel<V> createFieldModelWithTypes(final FieldModel<V> fieldModel, final TypeData<V> typeData) {
        if (fieldModel.getTypeData().equals(typeData)) {
            return fieldModel;
        }

        TypeData<V> fieldTypeData = typeData;
        if (List.class.isAssignableFrom(fieldModel.getTypeData().getType())) {
            fieldTypeData = TypeData.builder(fieldModel.getTypeData().getType()).addTypeParameter(typeData).build();
        } else if (Map.class.isAssignableFrom(fieldModel.getTypeData().getType())) {
            fieldTypeData = TypeData.builder(fieldModel.getTypeData().getType())
                    .addTypeParameter(TypeData.builder(String.class).build()).addTypeParameter(typeData).build();
        }

        return new FieldModelImpl<V>(fieldModel.getFieldName(), fieldModel.getDocumentFieldName(), fieldTypeData, null,
                new FieldModelSerialization<V>() {
                    @Override
                    public boolean shouldSerialize(final V value) {
                        return fieldModel.shouldSerialize(value);
                    }
                },
                fieldModel.useDiscriminator());
    }
}
