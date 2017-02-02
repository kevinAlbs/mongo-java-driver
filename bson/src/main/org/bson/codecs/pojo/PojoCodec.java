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
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.diagnostics.Loggers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@SuppressWarnings("unchecked")
final class PojoCodec<T> implements CollectibleCodec<T> {
    private static final Logger LOGGER = Loggers.getLogger("PojoCodec");

    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final Map<FieldModel<?>, Codec<?>> fieldCodecs;
    private final BsonTypeClassMap bsonTypeClassMap;

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap) {
        this(classModel, registry, bsonTypeClassMap, new HashMap<FieldModel<?>, Codec<?>>());
    }

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap,
              final Map<FieldModel<?>, Codec<?>> fieldCodecs) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.fieldCodecs = fieldCodecs;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        ClassAccessor<T> classAccessor = classModel.getClassAccessor();

        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        if (idFieldModel != null) {
            encodeField(value, classAccessor, idFieldModel, writer, encoderContext);
        }

        if (classModel.useDiscriminator()) {
            writer.writeString(classModel.getDiscriminatorKey(), classModel.getDiscriminator());
        }

        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            if (fieldModel == classModel.getIdFieldModel()) {
                continue;
            }
            encodeField(value, classAccessor, fieldModel, writer, encoderContext);
        }

        writer.writeEndDocument();
    }

    /**
     * Decode an entity applying the passed options.
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return an instance of the type parameter {@code T}.
     */
    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        ClassAccessor<T> classAccessor = classModel.getClassAccessor();
        decodeFields(classAccessor, reader, decoderContext);
        return classAccessor.create();
    }

    @Override
    public Class<T> getEncoderClass() {
        return classModel.getType();
    }

    @Override
    public T generateIdIfAbsentFromDocument(final T instance) {
        if (classModel.getIdFieldModel() == null) {
            throw new CodecConfigurationException(format("ClassModel for %s has no IdFieldModel and can't be inserted.",
                    classModel.getType()));
        }
        return instance;
    }

    @Override
    public boolean documentHasId(final T instance) {
        throw new UnsupportedOperationException("Checking if the instance has an _id field is not supported.");
    }

    @Override
    public BsonValue getDocumentId(final T instance) {
        throw new UnsupportedOperationException("Getting the BsonValue for the _id field is not supported.");
    }

    @Override
    public String toString() {
        return format("PojoCodec<%s>", classModel);
    }

    private <S> Encoder<S> getFieldEncoder(final FieldModel<S> fieldModel, final Object fieldValue) {
        Codec<S> codec = getCodec(fieldModel);
        if (codec == null) {
            codec = (Codec<S>) registry.get(fieldValue.getClass());
        }
        return codec;
    }

    private <S> Decoder<S> getFieldDecoder(final FieldModel<S> fieldModel, final BsonType bsonType) {
        Codec<S> codec = getCodec(fieldModel);
        if (codec == null) {
            Class<?> fieldClazz = bsonTypeClassMap.get(bsonType);
            codec = (Codec<S>) registry.get(fieldClazz);
        }
        return codec;
    }

    private <S> Codec<S> getCodec(final FieldModel<S> fieldModel) {
        Codec<S> codec = (Codec<S>) fieldCodecs.get(fieldModel);
        if (codec == null) {
            codec = fieldModel.getCodec() != null ? fieldModel.getCodec() : createFieldCodec(fieldModel, fieldModel.getTypeParameters());
            if (codec != null) {
                fieldCodecs.put(fieldModel, codec);
            }
        }
        return codec;
    }

    @SuppressWarnings("rawtypes")
    private <S> Codec<S> createFieldCodec(final FieldModel<S> model, final List<Class<?>> types) {
        Codec<S> fieldCodec = null;
        Class<?> first = types.get(0);
        List<Class<?>> remainder = types.size() > 1 ? types.subList(1, types.size()) : Collections.<Class<?>>emptyList();

        if (first == Object.class) {
            fieldCodec = null;
        } else if (Collection.class.isAssignableFrom(first)) {
            fieldCodec = new CollectionCodec(registry, bsonTypeClassMap, model.useDiscriminator(), classModel.getDiscriminatorKey(), first,
                    createFieldCodec(model, remainder));
        } else if (Map.class.isAssignableFrom(first)) {
            fieldCodec = new MapCodec(registry, bsonTypeClassMap, model.useDiscriminator(), classModel.getDiscriminatorKey(), first,
                    createFieldCodec(model, remainder));
        } else {
            try {
                fieldCodec = (Codec<S>) registry.get(first);
                if (fieldCodec instanceof PojoCodec) {
                    fieldCodec = ((PojoCodec<S>) fieldCodec).checkFieldUseDiscriminator(model);
                }
            } catch (final CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Missing codec for '%s', no codec available for '%s'.",
                        model.getDocumentFieldName(), first.getSimpleName()), e);
            }
        }
        return fieldCodec;
    }

    private PojoCodec<T> checkFieldUseDiscriminator(final FieldModel<?> fieldModel) {
        if (fieldModel.useDiscriminator() == classModel.useDiscriminator()) {
            return this;
        } else if (fieldModel.useDiscriminator() && (classModel.getDiscriminatorKey() == null || classModel.getDiscriminator() == null)) {
            return this;
        } else {
            ClassModelImpl<T> newClassModel = new ClassModelImpl<T>(classModel.getType(), classModel.getClassAccessorFactory(),
                    fieldModel.useDiscriminator(), classModel.getDiscriminatorKey(), classModel.getDiscriminator(),
                    classModel.getIdFieldModel(), classModel.getFieldModels());
            return new PojoCodec<T>(newClassModel, registry, bsonTypeClassMap, fieldCodecs);
        }
    }

    private <S> void encodeField(final T instance, final ClassAccessor<T> classAccessor, final FieldModel<S> fieldModel,
                                 final BsonWriter writer, final EncoderContext encoderContext) {
        S fieldValue = classAccessor.get(instance, fieldModel);
        if (fieldModel.shouldSerialize(fieldValue)) {
            writer.writeName(fieldModel.getDocumentFieldName());
            if (fieldValue == null) {
                writer.writeNull();
            } else {
                getFieldEncoder(fieldModel, fieldValue).encode(writer, fieldValue, encoderContext);
            }
        }
    }

    private void decodeFields(final ClassAccessor<T> classAccessor, final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                FieldModel<T> fieldModel = (FieldModel<T>) classModel.getFieldModel(name);
                if (fieldModel != null) {
                    try {
                        T value = null;
                        if (reader.getCurrentBsonType() == BsonType.NULL) {
                            reader.readNull();
                        } else {
                            Decoder<T> decoder = getFieldDecoder(fieldModel, reader.getCurrentBsonType());
                            value = decoder.decode(reader, decoderContext);
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
        }
        reader.readEndDocument();
    }

}
