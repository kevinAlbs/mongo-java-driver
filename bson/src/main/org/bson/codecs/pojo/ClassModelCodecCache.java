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

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

final class ClassModelCodecCache {
    private final Map<String, Map<Class<?>, Codec<?>>> codecCache;

    ClassModelCodecCache() {
        this.codecCache = new HashMap<String, Map<Class<?>, Codec<?>>>();
    }

    synchronized <T> void put(final String fieldName, final Class<T> clazz, final Codec<T> codec) {
        if (codecCache.containsKey(fieldName)) {
            codecCache.get(fieldName).put(clazz, codec);
        } else {
            HashMap<Class<?>, Codec<?>> map = new HashMap<Class<?>, Codec<?>>();
            map.put(clazz, codec);
            codecCache.put(fieldName, map);
        }
    }

    @SuppressWarnings("unchecked")
    <T> Codec<T> get(final String fieldName, final Class<T> clazz) {
        if (!codecCache.containsKey(fieldName)) {
            throw new CodecConfigurationException(format("Missing codec for field: %s", fieldName));
        }
        return (Codec<T>) codecCache.get(fieldName).get(clazz);
    }

}
