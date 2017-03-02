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

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.BsonTypeCodecMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;


final class ObjectCodec implements Codec<Object> {
    private final CodecRegistry registry;
    private final BsonTypeCodecMap bsonTypeCodecMap;

    ObjectCodec(final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap) {
        this.registry = registry;
        this.bsonTypeCodecMap = new BsonTypeCodecMap(bsonTypeClassMap, registry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
        ((Codec<Object>) registry.get(value.getClass())).encode(writer, value, encoderContext);
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext decoderContext) {
        return bsonTypeCodecMap.get(reader.getCurrentBsonType()).decode(reader, decoderContext);
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }

}
