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

package org.bson.codecs

import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonType
import org.bson.codecs.configuration.CodecConfigurationException
import spock.lang.Specification

import static org.bson.codecs.configuration.CodecRegistries.fromProviders

class ObjectCodecSpecification extends Specification {

    def 'should encode and decode values'() {
        given:
        def provider = new ObjectCodecProvider()
        def registry = fromProviders(new ValueCodecProvider(), provider)
        def codec = provider.get(Boolean, registry)

        def bool = true
        def document = new BsonDocument()

        when:
        def writer = new BsonDocumentWriter(document)
        writer.writeStartDocument()
        writer.writeName('b')
        codec.encode(writer, bool, EncoderContext.builder().build())
        writer.writeEndDocument()

        then:
        document == new BsonDocument('b', BsonBoolean.TRUE)

        when:
        def reader = new BsonDocumentReader(document)
        reader.readStartDocument()
        reader.readName('b')
        def value = codec.decode(reader, DecoderContext.builder().build())

        then:
        value == bool
    }

    def 'should error when encoding a value that has a missing codec'() {
        given:
        def provider = new ObjectCodecProvider()
        def registry = fromProviders(provider)
        def codec = provider.get(Boolean, registry)

        def bool = true
        def document = new BsonDocument()

        when:
        def writer = new BsonDocumentWriter(document)
        writer.writeStartDocument()
        writer.writeName('b')
        codec.encode(writer, bool, EncoderContext.builder().build())
        writer.writeEndDocument()

        then:
        thrown(CodecConfigurationException)
    }

    def 'should error when decoding and the value codec is missing'() {
        given:
        def provider = new ObjectCodecProvider()
        def registry = fromProviders(provider)
        def codec = provider.get(Boolean, registry)

        def document = new BsonDocument('b', BsonBoolean.TRUE)

        when:
        def reader = new BsonDocumentReader(document)
        reader.readStartDocument()
        reader.readName('b')
        codec.decode(reader, DecoderContext.builder().build())

        then:
        thrown(CodecConfigurationException)
    }

    def 'should error when decoding and the bsonType is not mapped'() {
        given:
        def bsonTypeClassMap = new BsonTypeClassMap([(BsonType.BOOLEAN): null])
        def provider = new ObjectCodecProvider(bsonTypeClassMap)
        def registry = fromProviders(provider)
        def codec = new ObjectCodec(provider, registry, bsonTypeClassMap)

        def document = new BsonDocument('b', BsonBoolean.TRUE)

        when:
        def reader = new BsonDocumentReader(document)
        reader.readStartDocument()
        reader.readName('b')
        codec.decode(reader, DecoderContext.builder().build())

        then:
        thrown(CodecConfigurationException)
    }
}
