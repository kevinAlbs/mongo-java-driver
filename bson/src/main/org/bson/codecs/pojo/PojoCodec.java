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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


final class PojoCodec<T> implements Codec<T> {
    private static final Logger LOGGER = Loggers.getLogger("PojoCodec");
    private final ClassModel<T> classModel;
    private final PojoCodecProvider codecProvider;
    private final CodecRegistry registry;
    private final DiscriminatorLookup discriminatorLookup;
    private final ClassModelCodecCache codecCache;
    private boolean setFields;

    @SuppressWarnings("unchecked")
    PojoCodec(final ClassModel<T> classModel, final PojoCodecProvider codecProvider, final CodecRegistry registry,
              final DiscriminatorLookup discriminatorLookup) {
        this.classModel = classModel;
        this.codecProvider = codecProvider;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = new ClassModelCodecCache();
    }

    synchronized PojoCodec<T> populateCodecCache() {
        if (!setFields) {
            setFields = true;
            for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
                addToCache(fieldModel);
            }
        }
        return this;
    }

    private <S> void addToCache(final FieldModel<S> fieldModel) {
        Codec<S> codec = getCodecFromFieldModel(fieldModel);
        fieldModel.cachedCodec(codec);
        codecCache.put(fieldModel.getFieldName(), fieldModel.getTypeData().getType(), codec);
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
            return getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), registry,
                    discriminatorLookup, this).decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
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
                getCachedCodec(fieldModel, fieldValue.getClass()).encode(writer, fieldValue, encoderContext);
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
                    value = decoderContext.decodeWithChildContext(fieldModel.getCachedCodec(), reader);
                }
                classAccessor.set(value, fieldModel);
            } catch (BsonInvalidOperationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. %s", name, e.getMessage()), e);
            } catch (CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. %s", name, e.getMessage()), e);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(format("Found field not present in the ClassModel: %s", name));
            }
            reader.skipValue();
        }
    }

    @SuppressWarnings("unchecked")
    private <S, V> Codec<S> getCachedCodec(final FieldModel<S> fieldModel, final Class<V> instanceType) {
        Codec<S> codec = fieldModel.getCachedCodec();
        Class<S> fieldType = codec.getEncoderClass();
        if (fieldType != instanceType && fieldType.isAssignableFrom(instanceType)) {
            codec = (Codec<S>) codecCache.get(fieldModel.getFieldName(), instanceType);
            if (codec == null) {
                codec = specializePojoCodec(fieldModel, getCodecFromClass((Class<S>) instanceType));
                codecCache.put(fieldModel.getFieldName(), instanceType, (Codec<V>) codec);
            }
        }
        return codec;
    }

    private <S> Codec<S> getCodecFromFieldModel(final FieldModel<S> fieldModel) {
        return fieldModel.getCodec() != null ? fieldModel.getCodec()
                : specializePojoCodec(fieldModel, getCodecFromTypeData(fieldModel.getTypeData()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private  <S> Codec<S> getCodecFromTypeData(final TypeData<S> typeData) {
        Codec<S> codec = null;
        Class<S> head = typeData.getType();

        if (Collection.class.isAssignableFrom(head)) {
            codec = new CollectionCodec(head, getCodecFromTypeData(typeData.getTypeParameters().get(0)));
        } else if (Map.class.isAssignableFrom(head)) {
            codec = new MapCodec(head, getCodecFromTypeData(typeData.getTypeParameters().get(1)));
        } else {
            codec = getCodecFromClass(head);
        }
        return codec;
    }

    @SuppressWarnings("unchecked")
    private <S> Codec<S> getCodecFromClass(final Class<S> clazz) {
        Codec<S> codec = null;
        if (classModel.getType().equals(clazz)) {
            codec = (Codec<S>) this;
        } else {
            codec = codecProvider.getPojoCodec(clazz, registry);
        }
        if (codec == null) {
            codec = registry.get(clazz);
        }
        return codec;
    }

    private <S> Codec<S> specializePojoCodec(final FieldModel<S> fieldModel, final Codec<S> defaultCodec) {
        Codec<S> codec = defaultCodec;
        if (codec != null && codec instanceof PojoCodec) {
            PojoCodec<S> pojoCodec = (PojoCodec<S>) codec;
            ClassModel<S> specialized = getSpecializedClassModel(pojoCodec.getClassModel(), fieldModel);
            if (!pojoCodec.getClassModel().equals(specialized)) {
                codec = new PojoCodec<S>(specialized, codecProvider, registry, discriminatorLookup);
            }
            pojoCodec.populateCodecCache();
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S, V> ClassModel<S> getSpecializedClassModel(final ClassModel<S> clazzModel, final FieldModel<V> fieldModel) {
        // Only change the discriminator when turning it off OR if turning it on ensure the class model has a discriminator key and value
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
        return new ClassModel<S>(clazzModel.getType(), clazzModel.getGenericFieldNames(), clazzModel.getClassAccessorFactory(),
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

        return new FieldModel<V>(fieldModel.getFieldName(), fieldModel.getDocumentFieldName(), fieldTypeData, null,
                fieldModel.getFieldSerialization(), fieldModel.useDiscriminator(), fieldModel.getFieldAccessorFactory());
    }

    @SuppressWarnings("unchecked")
    private Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                              final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                              final Codec<T> defaultCodec) {
        Codec<T> codec = defaultCodec;
        if (useDiscriminator) {
            reader.mark();
            reader.readStartDocument();
            boolean discriminatorKeyFound = false;
            while (!discriminatorKeyFound && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (discriminatorKey.equals(name)) {
                    discriminatorKeyFound = true;
                    codec = (Codec<T>) registry.get(discriminatorLookup.lookup(reader.readString()));
                } else {
                    reader.skipValue();
                }
            }
            reader.reset();
        }
        return codec;
    }

}
