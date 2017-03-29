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
        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        if (idFieldModel != null) {
            encodeField(writer, value, encoderContext, idFieldModel);
        }

        if (classModel.useDiscriminator()) {
            writer.writeString(classModel.getDiscriminatorKey(), classModel.getDiscriminator());
        }

        for (FieldModel<?> fieldModel : classModel.getFieldModels()) {
            if (fieldModel == classModel.getIdFieldModel()) {
                continue;
            }
            encodeField(writer, value, encoderContext, fieldModel);
        }
        writer.writeEndDocument();
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        if (decoderContext.hasCheckedDiscriminator()) {
            InstanceCreator<T> instanceCreator = classModel.getInstanceCreator();
            decodeFields(reader, decoderContext, instanceCreator);
            return instanceCreator.getInstance();
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
                                 final FieldModel<S> fieldModel) {
        S fieldValue = fieldModel.getFieldAccessor().get(instance);
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
    private void decodeFields(final BsonReader reader, final DecoderContext decoderContext, final InstanceCreator<T> instanceCreator) {
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodeFieldModel(reader, decoderContext, instanceCreator, name, classModel.getFieldModel(name));
            }
        }
        reader.readEndDocument();
    }

    @SuppressWarnings("unchecked")
    private <S> void decodeFieldModel(final BsonReader reader, final DecoderContext decoderContext,
                                      final InstanceCreator<T> instanceCreator, final String name, final FieldModel<S> fieldModel) {
        if (fieldModel != null) {
            try {
                S value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(fieldModel.getCachedCodec(), reader);
                }
                instanceCreator.set(value, fieldModel);
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
        if (!areEquivalentTypes(fieldType, instanceType)) {
            codec = (Codec<S>) codecCache.get(fieldModel.getFieldName(), instanceType);
            if (codec == null) {
                codec = specializePojoCodec(fieldModel, getCodecFromClass((Class<S>) instanceType));
                codecCache.put(fieldModel.getFieldName(), instanceType, (Codec<V>) codec);
            }
        }
        return codec;
    }

    private <S, V> boolean areEquivalentTypes(final Class<S> t1, final Class<V> t2) {
        if (t1 == t2) {
            return true;
        } else if (Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)) {
            return true;
        } else if (Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2)) {
            return true;
        }
        return false;
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
                pojoCodec = new PojoCodec<S>(specialized, codecProvider, registry, discriminatorLookup);
            }
            pojoCodec.populateCodecCache();
            codec = pojoCodec;
        }
        return codec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S, V> ClassModel<S> getSpecializedClassModel(final ClassModel<S> clazzModel, final FieldModel<V> fieldModel) {
        boolean useDiscriminator = fieldModel.useDiscriminator() == null ? clazzModel.useDiscriminator() : fieldModel.useDiscriminator();
        boolean validDiscriminator = clazzModel.getDiscriminatorKey() != null && clazzModel.getDiscriminator() != null;
        boolean changeTheDiscriminator = (useDiscriminator != clazzModel.useDiscriminator()) && validDiscriminator;

        if (clazzModel.getFieldNameToTypeParameterMap().isEmpty() && !changeTheDiscriminator){
            return clazzModel;
        }

        ArrayList<FieldModel<?>> concreteFieldModels = new ArrayList<FieldModel<?>>(clazzModel.getFieldModels());
        FieldModel<?> concreteIdField = clazzModel.getIdFieldModel();

        List<TypeData<?>> fieldTypeParameters = fieldModel.getTypeData().getTypeParameters();
        for (int i = 0; i < concreteFieldModels.size(); i++) {
            FieldModel<?> model = concreteFieldModels.get(i);
            String fieldName = model.getFieldName();
            TypeParameterMap typeParameterMap = clazzModel.getFieldNameToTypeParameterMap().get(fieldName);
            if (typeParameterMap != null) {
                FieldModel<?> concreteFieldModel = getSpecializedFieldModel(model, typeParameterMap, fieldTypeParameters);
                concreteFieldModels.set(i, concreteFieldModel);
                if (concreteIdField != null && concreteIdField.getFieldName().equals(fieldName)) {
                    concreteIdField = concreteFieldModel;
                }
            }
        }

        boolean discriminatorEnabled = changeTheDiscriminator ? fieldModel.useDiscriminator() : clazzModel.useDiscriminator();
        return new ClassModel<S>(clazzModel.getType(), clazzModel.getFieldNameToTypeParameterMap(),
                clazzModel.getInstanceCreatorFactory(), discriminatorEnabled, clazzModel.getDiscriminatorKey(),
                clazzModel.getDiscriminator(), concreteIdField, concreteFieldModels);
    }

    @SuppressWarnings("unchecked")
    private <V> FieldModel<V> getSpecializedFieldModel(final FieldModel<V> fieldModel, final TypeParameterMap typeParameterMap,
                                                       final List<TypeData<?>> fieldTypeParameters) {
        TypeData<V> specializedFieldType = fieldModel.getTypeData();
        Map<Integer, Integer> fieldToClassParamIndexMap = typeParameterMap.getFieldToClassParamIndexMap();
        Integer classTypeParamRepresentsWholeField = fieldToClassParamIndexMap.get(-1);
        if (classTypeParamRepresentsWholeField != null) {
            specializedFieldType = (TypeData<V>) fieldTypeParameters.get(classTypeParamRepresentsWholeField);
        } else {
            TypeData.Builder<V> builder = TypeData.builder(fieldModel.getTypeData().getType());
            List<TypeData<?>> typeParameters = new ArrayList<TypeData<?>>(fieldModel.getTypeData().getTypeParameters());
            for (int i = 0; i < typeParameters.size(); i++) {
                for (Map.Entry<Integer, Integer> mapping : fieldToClassParamIndexMap.entrySet()) {
                    if (mapping.getKey() == i) {
                        typeParameters.set(i, fieldTypeParameters.get(mapping.getValue()));
                    }
                }
            }
            builder.addTypeParameters(typeParameters);
            specializedFieldType = builder.build();
        }
        if (fieldModel.getTypeData().equals(specializedFieldType)) {
            return fieldModel;
        }

        return new FieldModel<V>(fieldModel.getFieldName(), fieldModel.getDocumentFieldName(), specializedFieldType, null,
                fieldModel.getFieldSerialization(), fieldModel.useDiscriminator(), fieldModel.getFieldAccessor());
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
