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

import com.fasterxml.classmate.AnnotationConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.diagnostics.Loggers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;

final class PojoHelper {

    private static final Logger LOGGER = Loggers.getLogger("PojoCodec");

    @SuppressWarnings("unchecked")
    static <T> void configureClassModelBuilder(final ClassModelBuilder<T> classModelBuilder, final Class<T> clazz) {
        classModelBuilder.type(notNull("clazz", clazz)).annotations(asList(clazz.getAnnotations()));

        TypeResolver resolver = new TypeResolver();
        MemberResolver memberResolver = new MemberResolver(resolver);
        ResolvedType resolved = resolver.resolve(clazz);

        ResolvedTypeWithMembers resolvedType =
                memberResolver.resolve(resolved, new AnnotationConfiguration.StdConfiguration(AnnotationInclusion
                        .INCLUDE_AND_INHERIT_IF_INHERITED), null);

        List<Constructor<T>> publicConstructors = new ArrayList<Constructor<T>>();
        for (RawConstructor rawConstructor : resolved.getConstructors()) {
            if (rawConstructor.isPublic()) {
                publicConstructors.add((Constructor<T>) rawConstructor.getRawMember());
            }
        }

        Map<String, Field> fieldsMap = new HashMap<String, Field>();
        List<ResolvedField> resolvedFields = new ArrayList<ResolvedField>(asList(resolvedType.getMemberFields()));
        for (final ResolvedField resolvedField : resolvedFields) {
            if (resolvedField.isTransient()) {
                continue;
            }
            classModelBuilder.addField(getFieldModelBuilder(resolvedField));
            fieldsMap.put(resolvedField.getName(), resolvedField.getRawMember());
            resolvedField.getRawMember().setAccessible(true);
        }
        classModelBuilder.classAccessorFactory(new ClassAccessorFactoryImpl<T>(publicConstructors, fieldsMap));
    }

    @SuppressWarnings("unchecked")
    static <T> FieldModelBuilder<T> getFieldModelBuilder(final ResolvedField resolvedField) {
        Class<T> erasedType = (Class<T>) resolvedField.getType().getErasedType();
        return configureFieldModelBuilder(new FieldModelBuilder<T>(), resolvedField.getRawMember())
                .type(erasedType)
                .typeParameters(extractTypeParameters(resolvedField.getType()));
    }

    @SuppressWarnings("unchecked")
    static <T> FieldModelBuilder<T> configureFieldModelBuilder(final FieldModelBuilder<T> builder, final Field field) {
        Class<T> type = (Class<T>) field.getType();
        return builder
                .fieldName(field.getName())
                .documentFieldName(field.getName())
                .type(type)
                .fieldName(field.getName())
                .documentFieldName(field.getName())
                .annotations(asList(field.getDeclaredAnnotations()))
                .fieldModelSerialization(new FieldModelSerializationImpl<T>());
    }

    static List<Class<?>> extractTypeParameters(final ResolvedType type) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Class<?> erasedType = type.getErasedType();
        if (Collection.class.isAssignableFrom(erasedType)) {
            ResolvedType collectionType = type.getTypeParameters().get(0);
            Class<?> containerClass;
            if (Set.class.equals(erasedType)) {
                containerClass = HashSet.class;
            } else if (List.class.equals(erasedType) || Collection.class.equals(erasedType)) {
                containerClass = ArrayList.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(extractTypeParameters(collectionType));
        } else if (Map.class.isAssignableFrom(erasedType)) {
            List<ResolvedType> types = type.getTypeParameters();
            ResolvedType keyType = types.get(0);
            ResolvedType valueType = types.get(1);
            if (!keyType.getErasedType().equals(String.class)) {
                throw new IllegalStateException(format("Invalid Map type. Map key types MUST be Strings, found %s instead.",
                        keyType.getErasedType()));
            }
            Class<?> containerClass;
            if (Map.class.equals(erasedType)) {
                containerClass = HashMap.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(extractTypeParameters(valueType));
        } else {
            classes.add(type.getErasedType());
        }

        return classes;
    }

    static <V> V stateNotNull(final String property, final V value) {
        if (value == null) {
            throw new IllegalStateException(format("%s cannot be null", property));
        }
        return value;
    }

    static <T> void encodeClassModel(final BsonWriter writer, final T value, final EncoderContext encoderContext,
                                     final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                     final BsonTypeClassMap bsonTypeClassMap, final Map<FieldModel<?>, Codec<?>> codecCache,
                                     final ClassModel<T> classModel) {
        writer.writeStartDocument();
        ClassAccessor<T> classAccessor = classModel.getClassAccessor();

        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        if (idFieldModel != null) {
            encodeField(writer, value, encoderContext, registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel,
                    classAccessor, idFieldModel);
        }

        if (classModel.useDiscriminator()) {
            writer.writeString(classModel.getDiscriminatorKey(), classModel.getDiscriminator());
        }

        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            if (fieldModel == classModel.getIdFieldModel()) {
                continue;
            }
            encodeField(writer, value, encoderContext, registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel,
                    classAccessor, fieldModel);
        }

        writer.writeEndDocument();
    }

    private static <T, S> void encodeField(final BsonWriter writer, final T instance, final EncoderContext encoderContext,
                                           final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                           final BsonTypeClassMap bsonTypeClassMap,
                                           final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                           final ClassAccessor<T> classAccessor, final FieldModel<S> fieldModel) {
        S fieldValue = classAccessor.get(instance, fieldModel);
        if (fieldModel.shouldSerialize(fieldValue)) {
            writer.writeName(fieldModel.getDocumentFieldName());
            if (fieldValue == null) {
                writer.writeNull();
            } else {
                getFieldEncoder(registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel, fieldModel,
                        fieldValue).encode(writer, fieldValue, encoderContext);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, S> Encoder<S> getFieldEncoder(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                                     final BsonTypeClassMap bsonTypeClassMap,
                                                     final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                                     final FieldModel<S> fieldModel, final S value) {
        Codec<S> codec = (Codec<S>) codecCache.get(fieldModel);
        if (codec == null) {
            codec = fieldModel.getCodec() != null ? fieldModel.getCodec()
                    : createFieldCodec(registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel, fieldModel,
                    fieldModel.getTypeParameters());
            if (codec != null) {
                codecCache.put(fieldModel, codec);
            } else {
                codec = (Codec<S>) registry.get(value.getClass());
            }
        }
        return codec;
    }

    static <T> T decodeClassModel(final BsonReader reader, final DecoderContext decoderContext, final CodecRegistry registry,
                                  final DiscriminatorLookup discriminatorLookup, final BsonTypeClassMap bsonTypeClassMap,
                                  final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                  final PojoCodec<T> defaultCodec) {
        if (decoderContext.hasCheckedDiscriminator()) {
            ClassAccessor<T> classAccessor = classModel.getClassAccessor();
            decodeFields(reader, decoderContext, registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel, classAccessor);
            return classAccessor.create();
        } else {
            Codec<T> codec = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), registry,
                    discriminatorLookup, defaultCodec);
            return codec.decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void decodeFields(final BsonReader reader, final DecoderContext decoderContext, final CodecRegistry registry,
                                         final DiscriminatorLookup discriminatorLookup, final BsonTypeClassMap bsonTypeClassMap,
                                         final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                         final ClassAccessor<T> classAccessor) {
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
                            Decoder<T> decoder = getFieldDecoder(registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel,
                                    fieldModel, reader.getCurrentBsonType());
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

    @SuppressWarnings("unchecked")
    private static <T, S> Decoder<S> getFieldDecoder(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                                     final BsonTypeClassMap bsonTypeClassMap,
                                                     final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                                     final FieldModel<S> fieldModel, final BsonType bsonType) {
        Codec<S> codec = (Codec<S>) codecCache.get(fieldModel);
        if (codec == null) {
            codec = fieldModel.getCodec() != null ? fieldModel.getCodec()
                    : createFieldCodec(registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel, fieldModel,
                    fieldModel.getTypeParameters());
            if (codec != null) {
                codecCache.put(fieldModel, codec);
            } else {
                Class<?> fieldClazz = bsonTypeClassMap.get(bsonType);
                codec = (Codec<S>) registry.get(fieldClazz);
            }
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T, S> Codec<S> createFieldCodec(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                                    final BsonTypeClassMap bsonTypeClassMap,
                                                    final Map<FieldModel<?>, Codec<?>> codecCache, final ClassModel<T> classModel,
                                                    final FieldModel<S> fieldModel, final List<Class<?>> types) {
        Codec<S> codec = null;
        Class<?> first = types.get(0);
        List<Class<?>> remainder = types.size() > 1 ? types.subList(1, types.size()) : Collections.<Class<?>>emptyList();

        if (first == Object.class) {
            codec = null;
        } else if (Collection.class.isAssignableFrom(first)) {
            codec = new CollectionCodec(registry, discriminatorLookup, bsonTypeClassMap, fieldModel.useDiscriminator(),
                    classModel.getDiscriminatorKey(), first, createFieldCodec(registry, discriminatorLookup, bsonTypeClassMap,
                    codecCache, classModel, fieldModel, remainder));
        } else if (Map.class.isAssignableFrom(first)) {
            codec = new MapCodec(registry, discriminatorLookup, bsonTypeClassMap, fieldModel.useDiscriminator(), classModel
                    .getDiscriminatorKey(), first,
                    createFieldCodec(registry, discriminatorLookup, bsonTypeClassMap, codecCache, classModel, fieldModel, remainder));
        } else {
            codec = getFieldCodecFromRegistry(registry, discriminatorLookup, bsonTypeClassMap, codecCache, fieldModel, first);
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Codec<T> getFieldCodecFromRegistry(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                                          final BsonTypeClassMap bsonTypeClassMap,
                                                          final Map<FieldModel<?>, Codec<?>> codecCache, final FieldModel<T> fieldModel,
                                                          final Class<?> clazz) {
        Codec<T> codec = null;
        try {
            codec = (Codec<T>) registry.get(clazz);
            if (codec instanceof PojoCodec) {
                ClassModel<T> classModel = ((PojoCodec<T>) codec).getClassModel();
                boolean createNewCodec = !fieldModel.useDiscriminator() && classModel.useDiscriminator()
                        || (fieldModel.useDiscriminator() && classModel.getDiscriminatorKey() != null
                        && classModel.getDiscriminator() != null);
                if (createNewCodec) {
                    ClassModelImpl<T> newClassModel = new ClassModelImpl<T>(classModel.getType(), classModel.getClassAccessorFactory(),
                            fieldModel.useDiscriminator(), classModel.getDiscriminatorKey(),
                            classModel.getDiscriminator(), classModel.getIdFieldModel(), classModel.getFieldModels());
                    codec = new PojoCodec<T>(newClassModel, registry, discriminatorLookup, bsonTypeClassMap, codecCache);
                }
            }
        } catch (final CodecConfigurationException e) {
            throw new CodecConfigurationException(format("Missing codec for '%s', no codec available for '%s'.",
                    fieldModel.getDocumentFieldName(), clazz.getSimpleName()), e);
        }
        return codec;
    }

    @SuppressWarnings("unchecked")
    static <T> Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                             final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                             final Codec<T> defaultCodec) {
        Codec<T> codec = defaultCodec;
        if (useDiscriminator && discriminatorKey != null) {
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

    private PojoHelper() {
    }

}
