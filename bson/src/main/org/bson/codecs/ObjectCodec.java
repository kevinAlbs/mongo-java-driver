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

package org.bson.codecs;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;

/**
 * A low level Object codec that utilises a {@link BsonTypeClassMap} to map bson types to classes when encoding data.
 *
 * @since 3.5
 */
public final class ObjectCodec implements Codec<Object> {
    private final ObjectCodecProvider codecProvider;
    private final CodecRegistry codecRegistry;
    private final BsonTypeClassMap bsonTypeClassMap;
    private final Codec<?>[] codecCache = new Codec<?>[256];

    ObjectCodec(final ObjectCodecProvider codecProvider, final CodecRegistry codecRegistry, final BsonTypeClassMap bsonTypeClassMap) {
        this.codecProvider = notNull("codecProvider", codecProvider);
        this.codecRegistry = notNull("codecRegistry", codecRegistry);
        this.bsonTypeClassMap = notNull("bsonTypeClassMap", bsonTypeClassMap);

        for (BsonType cur : bsonTypeClassMap.keys()) {
            Class<?> clazz = bsonTypeClassMap.get(cur);
            if (clazz != null) {
                codecCache[cur.getValue()] = codecProvider.getNestedCodec(clazz, codecRegistry);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
        getValueCodec(value.getClass()).encode(writer, value, encoderContext);
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext decoderContext) {
        return getCachedCodec(reader.getCurrentBsonType()).decode(reader, decoderContext);
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }

    private Codec<?> getCachedCodec(final BsonType bsonType) {
        Codec<?> codec = codecCache[bsonType.getValue()];
        if (codec == null) {
            Class<?> clazz = bsonTypeClassMap.get(bsonType);
            if (clazz == null) {
                throw new CodecConfigurationException(format("No class mapped for BSON type %s.", bsonType));
            } else {
                throw new CodecConfigurationException(format("Can't find a codec for %s.", clazz));
            }
        }
        return codec;
    }

    @SuppressWarnings("unchecked")
    private <T> Codec<Object> getValueCodec(final Class<T> clazz) {
        Codec<Object> codec = (Codec<Object>) codecProvider.getNestedCodec(clazz, codecRegistry);
        if (codec == null) {
                throw new CodecConfigurationException(format("Can't find a codec for %s.", clazz));
        }
        return codec;
    }
}
