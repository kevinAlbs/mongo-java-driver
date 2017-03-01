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

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.PojoHelper.decodeClassModel;
import static org.bson.codecs.pojo.PojoHelper.encodeClassModel;


final class PojoCodec<T> implements Codec<T> {

    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final DiscriminatorLookup discriminatorLookup;
    private final BsonTypeClassMap bsonTypeClassMap;
    private final Map<FieldModel<?>, Codec<?>> fieldCodecs;

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
              final BsonTypeClassMap bsonTypeClassMap) {
        this(classModel, registry, discriminatorLookup, bsonTypeClassMap, new HashMap<FieldModel<?>, Codec<?>>());
    }

    PojoCodec(final ClassModel<T> classModel, final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
              final BsonTypeClassMap bsonTypeClassMap, final Map<FieldModel<?>, Codec<?>> fieldCodecs) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.discriminatorLookup = discriminatorLookup;
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.fieldCodecs = fieldCodecs;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        encodeClassModel(writer, value, encoderContext, registry, discriminatorLookup, bsonTypeClassMap, fieldCodecs, classModel);
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return decodeClassModel(reader, decoderContext, registry, discriminatorLookup, bsonTypeClassMap, fieldCodecs, classModel, this);
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
}
