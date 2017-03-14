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

import org.bson.BsonType;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * A low level Object codec provider that utilises a {@link BsonTypeClassMap} to map bson types to classes when encoding data.
 *
 * @since 3.5
 */
public final class ObjectCodecProvider implements CodecProvider {
    private final BsonTypeClassMap bsonTypeClassMap;
    private final List<Class<?>> classes;

    /**
     * Constructs a new instance with the default {@code BsonTypeClassMap }
     */
    public ObjectCodecProvider() {
        this(new BsonTypeClassMap());
    }

    /**
     * Constructs a new instance
     *
     * @param bsonTypeClassMap the bsonTypeClassMap to use.
     */
    public ObjectCodecProvider(final BsonTypeClassMap bsonTypeClassMap) {
        this.bsonTypeClassMap = bsonTypeClassMap;
        this.classes = new ArrayList<Class<?>>();
        for (BsonType bsonType : bsonTypeClassMap.keys()) {
            Class<?> clazz = bsonTypeClassMap.get(bsonType);
            if (clazz != null) {
                classes.add(clazz);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        boolean useCodec = clazz == Object.class;
        if (!useCodec) {
            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(clazz)) {
                    useCodec = true;
                    break;
                }
            }
        }

        if (useCodec) {
            return (Codec<T>) new ObjectCodec(this, registry, bsonTypeClassMap);
        }
        return null;
    }

    <T> Codec<T> getNestedCodec(final Class<T> clazz, final CodecRegistry registry) {
        Codec<T> codec = registry.get(clazz);
        if (codec instanceof ObjectCodec) {
            return null;
        }
        return codec;
    }

}
