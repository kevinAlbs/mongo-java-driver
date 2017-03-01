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
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.diagnostics.Loggers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final BsonTypeClassMap bsonTypeClassMap;
    private final Map<String, Codec<?>> codecCache;

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
              final BsonTypeClassMap bsonTypeClassMap) {
        this(classModel, registry, discriminatorLookup, bsonTypeClassMap, new ConcurrentHashMap<String, Codec<?>>());
    }

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
              final BsonTypeClassMap bsonTypeClassMap, final Map<String, Codec<?>> codecCache) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.discriminatorLookup = discriminatorLookup;
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.codecCache = codecCache;
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
                getCodec(fieldModel, (Class<S>) fieldValue.getClass()).encode(writer, fieldValue, encoderContext);
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
                    Class<S> clazz = (Class<S>) bsonTypeClassMap.get(reader.getCurrentBsonType());
                    value = getCodec(fieldModel, clazz).decode(reader, decoderContext);
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
    private <S> Codec<S> getCodec(final FieldModel<S> fieldModel, final Class<S> fieldClazz) {
        Codec<S> codec = (Codec<S>) codecCache.get(fieldModel.getFieldName());
        if (codec == null) {
            codec = fieldModel.getCodec();
            if (codec == null && !fieldModel.getTypeParameters().isEmpty()) {
                codec = createFieldCodecFromTypeParameters(fieldModel, fieldModel.getTypeParameters());
            }
            if (codec == null) {
                codec = getFieldCodecFromRegistry(fieldModel, fieldClazz);
            }

            codecCache.put(fieldModel.getFieldName(), codec);
        }
        return codec;
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S> Codec<S> createFieldCodecFromTypeParameters(final FieldModel<S> fieldModel, final List<Class<?>> types) {
        Codec<S> codec = null;
        Class<?> head = types.get(0);
        List<Class<?>> remainder = types.size() > 1 ? types.subList(1, types.size()) : Collections.<Class<?>>emptyList();

        if (Collection.class.isAssignableFrom(head)) {
            codec = new CollectionCodec(registry, discriminatorLookup, bsonTypeClassMap, fieldModel.useDiscriminator(),
                    classModel.getDiscriminatorKey(), head, createFieldCodecFromTypeParameters(fieldModel, remainder));
        } else if (Map.class.isAssignableFrom(head)) {
            codec = new MapCodec(registry, discriminatorLookup, bsonTypeClassMap, fieldModel.useDiscriminator(),
                    classModel.getDiscriminatorKey(), head, createFieldCodecFromTypeParameters(fieldModel, remainder));
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S> Codec<S> getFieldCodecFromRegistry(final FieldModel<S> fieldModel, final Class<?> clazz) {
        Codec<S> codec = null;
        try {
            codec = (Codec<S>) registry.get(clazz);
            if (codec instanceof PojoCodec) {
                ClassModel<S> classModel = ((PojoCodec<S>) codec).getClassModel();
                boolean createNewCodec = !fieldModel.useDiscriminator() && classModel.useDiscriminator()
                        || (fieldModel.useDiscriminator() && classModel.getDiscriminatorKey() != null
                        && classModel.getDiscriminator() != null);
                if (createNewCodec) {
                    ClassModelImpl<S> newClassModel = new ClassModelImpl<S>(classModel.getType(), classModel.getClassAccessorFactory(),
                            fieldModel.useDiscriminator(), classModel.getDiscriminatorKey(),
                            classModel.getDiscriminator(), classModel.getIdFieldModel(), classModel.getFieldModels());
                    codec = new PojoCodec<S>(newClassModel, registry, discriminatorLookup, bsonTypeClassMap, codecCache);
                }
            }
        } catch (final CodecConfigurationException e) {
            throw new CodecConfigurationException(format("Missing codec for '%s', no codec available for '%s'.",
                    fieldModel.getDocumentFieldName(), clazz.getSimpleName()), e);
        }
        return codec;
    }
}
