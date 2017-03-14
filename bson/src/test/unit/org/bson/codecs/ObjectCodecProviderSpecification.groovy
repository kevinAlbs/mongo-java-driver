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

package org.bson.codecs

import org.bson.BsonDbPointer
import org.bson.BsonRegularExpression
import org.bson.BsonTimestamp
import org.bson.BsonType
import org.bson.BsonUndefined
import org.bson.Document
import org.bson.types.Binary
import org.bson.types.Code
import org.bson.types.CodeWithScope
import org.bson.types.Decimal128
import org.bson.types.MaxKey
import org.bson.types.MinKey
import org.bson.types.ObjectId
import org.bson.types.Symbol
import spock.lang.Specification

import static org.bson.codecs.configuration.CodecRegistries.fromProviders

class ObjectCodecProviderSpecification extends Specification {

    def 'should provide supported codecs'() {
        given:
        def provider = new ObjectCodecProvider()
        def registry = fromProviders(provider)

        expect:
        provider.get(List, registry) instanceof ObjectCodec
        provider.get(Binary, registry) instanceof ObjectCodec
        provider.get(Boolean, registry) instanceof ObjectCodec
        provider.get(Date, registry) instanceof ObjectCodec
        provider.get(BsonDbPointer, registry) instanceof ObjectCodec
        provider.get(Document, registry) instanceof ObjectCodec
        provider.get(Double, registry) instanceof ObjectCodec
        provider.get(Integer, registry) instanceof ObjectCodec
        provider.get(Long, registry) instanceof ObjectCodec
        provider.get(Decimal128, registry) instanceof ObjectCodec
        provider.get(MaxKey, registry) instanceof ObjectCodec
        provider.get(MinKey, registry) instanceof ObjectCodec
        provider.get(Code, registry) instanceof ObjectCodec
        provider.get(CodeWithScope, registry) instanceof ObjectCodec
        provider.get(ObjectId, registry) instanceof ObjectCodec
        provider.get(BsonRegularExpression, registry) instanceof ObjectCodec
        provider.get(String, registry) instanceof ObjectCodec
        provider.get(Symbol, registry) instanceof ObjectCodec
        provider.get(BsonTimestamp, registry) instanceof ObjectCodec
        provider.get(BsonUndefined, registry) instanceof ObjectCodec

        provider.get(BigInteger, registry) == null
    }

    def 'should provide assignable codecs in the BsonTypeClassMap'() {
        given:
        def provider = new ObjectCodecProvider()
        def registry = fromProviders(provider)

        expect:
        provider.get(ArrayList, registry) instanceof ObjectCodec
    }

    def 'should provide supported codecs in the BsonTypeClassMap'() {
        given:
        def provider = new ObjectCodecProvider(new BsonTypeClassMap([(BsonType.DATE_TIME): java.sql.Date]))
        def registry = fromProviders(provider)

        expect:
        provider.get(Date, registry) == null
        provider.get(java.sql.Date, registry) instanceof ObjectCodec
    }

}
