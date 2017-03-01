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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static org.bson.codecs.pojo.PojoCodecHelper.getCodecFromDocument;

@SuppressWarnings({"unchecked", "rawtypes"})
class MapCodec<T> implements Codec<Map<String, T>> {
    private final CodecRegistry registry;
    private final DiscriminatorLookup discriminatorLookup;
    private final BsonTypeClassMap bsonTypeClassMap;
    private final boolean useDiscriminator;
    private final String discriminatorKey;
    private final Class<Map<String, T>> encoderClass;
    private final Codec<T> codec;

    MapCodec(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup, final BsonTypeClassMap bsonTypeClassMap,
             final boolean useDiscriminator, final String discriminatorKey, final Class<Map<String, T>> encoderClass) {
        this(registry, discriminatorLookup, bsonTypeClassMap, useDiscriminator, discriminatorKey, encoderClass, null);
    }

    MapCodec(final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup, final BsonTypeClassMap bsonTypeClassMap,
             final boolean useDiscriminator, final String discriminatorKey, final Class<Map<String, T>> encoderClass,
             final Codec<T> codec) {
        this.registry = registry;
        this.discriminatorLookup = discriminatorLookup;
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.useDiscriminator = useDiscriminator;
        this.discriminatorKey = discriminatorKey;
        this.encoderClass = encoderClass;
        this.codec = codec;
    }

    @Override
    public void encode(final BsonWriter writer, final Map<String, T> map, final EncoderContext encoderContext) {
        writer.writeStartDocument();

        for (final Entry<String, T> entry : map.entrySet()) {
            writer.writeName(entry.getKey());
            T value = entry.getValue();
            Codec<T> itemCodec = codec;
            if (codec == null) {
                Class<?> clazz = value.getClass();
                if (Collection.class.isAssignableFrom(clazz)) {
                    itemCodec = new CollectionCodec(registry, discriminatorLookup, bsonTypeClassMap, useDiscriminator, discriminatorKey,
                            clazz);
                } else if (Map.class.isAssignableFrom(clazz)) {
                    itemCodec = (Codec<T>) this;
                } else {
                    itemCodec = (Codec<T>) registry.get(clazz);
                }
            }
            itemCodec.encode(writer, value, encoderContext);
        }

        writer.writeEndDocument();
    }

    @Override
    public Map<String, T> decode(final BsonReader reader, final DecoderContext context) {
        reader.readStartDocument();
        Map<String, T> map = getInstance();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            Codec<T> itemCodec = codec;
            if (codec == null) {
                BsonType currentType = reader.getCurrentBsonType();
                if (currentType == BsonType.DOCUMENT) {
                    itemCodec = getCodecFromDocument(reader, useDiscriminator, discriminatorKey, registry, discriminatorLookup,
                            (Codec<T>) this);
                } else if (currentType == BsonType.ARRAY) {
                    itemCodec = new CollectionCodec(registry, discriminatorLookup, bsonTypeClassMap, useDiscriminator, discriminatorKey,
                            ArrayList.class);
                } else {
                    Class<?> fieldClazz = bsonTypeClassMap.get(reader.getCurrentBsonType());
                    itemCodec = (Codec<T>) registry.get(fieldClazz);
                }
            }
            map.put(reader.readName(), itemCodec.decode(reader, context));
        }
        reader.readEndDocument();
        return map;
    }

    @Override
    public Class<Map<String, T>> getEncoderClass() {
        return encoderClass;
    }

    private Map<String, T> getInstance() {
        try {
            return encoderClass.newInstance();
        } catch (final Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }

}
