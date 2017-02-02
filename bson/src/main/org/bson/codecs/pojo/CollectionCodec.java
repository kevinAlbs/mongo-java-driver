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
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.bson.codecs.pojo.PojoHelper.getCodecFromDocument;

@SuppressWarnings({"unchecked", "rawtypes"})
class CollectionCodec<T> implements Codec<Collection<T>> {
    private final CodecRegistry registry;
    private final BsonTypeClassMap bsonTypeClassMap;
    private final boolean useDiscriminator;
    private final String discriminatorKey;
    private final Class<Collection<T>> encoderClass;
    private final Codec<T> codec;

    CollectionCodec(final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap, final boolean useDiscriminator,
                    final String discriminatorKey, final Class<Collection<T>> encoderClass) {
        this(registry, bsonTypeClassMap, useDiscriminator, discriminatorKey, encoderClass, null);
    }

    CollectionCodec(final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap, final boolean useDiscriminator,
                    final String discriminatorKey, final Class<Collection<T>> encoderClass, final Codec<T> codec) {
        this.registry = registry;
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.useDiscriminator = useDiscriminator;
        this.discriminatorKey = discriminatorKey;
        this.encoderClass = encoderClass;
        this.codec = codec;
    }

    @Override
    public void encode(final BsonWriter writer, final Collection<T> collection, final EncoderContext encoderContext) {
        writer.writeStartArray();

        for (final T value : collection) {
            Codec<T> itemCodec = codec;
            if (codec == null) {
                Class<?> clazz = value.getClass();
                if (Collection.class.isAssignableFrom(clazz)) {
                    itemCodec = (Codec<T>) this;
                } else if (Map.class.isAssignableFrom(clazz)) {
                    itemCodec = new MapCodec(registry, bsonTypeClassMap, useDiscriminator, discriminatorKey, clazz);
                } else {
                    itemCodec = (Codec<T>) registry.get(clazz);
                }
            }
            itemCodec.encode(writer, value, encoderContext);
        }

        writer.writeEndArray();
    }

    @Override
    public Collection<T> decode(final BsonReader reader, final DecoderContext context) {
        reader.readStartArray();

        Collection<T> collection = getInstance();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            Codec<T> itemCodec = codec;
            if (codec == null) {
                BsonType currentType = reader.getCurrentBsonType();
                if (currentType == BsonType.ARRAY) {
                    itemCodec = (Codec<T>) this;
                } else if (currentType == BsonType.DOCUMENT) {
                    itemCodec = getCodecFromDocument(reader, useDiscriminator, discriminatorKey, registry,
                            new MapCodec(registry, bsonTypeClassMap, useDiscriminator, discriminatorKey, HashMap.class));
                } else {
                    Class<?> fieldClazz = bsonTypeClassMap.get(reader.getCurrentBsonType());
                    itemCodec = (Codec<T>) registry.get(fieldClazz);
                }
            }
            collection.add(itemCodec.decode(reader, context));
        }

        reader.readEndArray();

        return collection;
    }

    @Override
    public Class<Collection<T>> getEncoderClass() {
        return encoderClass;
    }

    private Collection<T> getInstance() {
        try {
            return encoderClass.newInstance();
        } catch (final Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }
}
