/*
 * Copyright 2008-present MongoDB, Inc.
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

package com.mongodb.internal.connection;

import org.bson.BsonBinaryReader;
import org.bson.ByteBuf;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.io.BsonInput;
import org.bson.io.ByteBufferBsonInput;

import java.util.ArrayList;
import java.util.List;

class Message<D, S> {
    private ByteBuf documentByteBuf;
    private D document;
    private List<S> documentSequence;
    private String documentSequenceName;

    Message(final ResponseBuffers responseBuffers, final Decoder<D> decoder, final Decoder<S> documentSequenceDecoder) {
        ByteBuf sectionBuffer = responseBuffers.getBodyByteBuffer();
        byte sectionType = sectionBuffer.get();
        if (sectionType == 0) {
            document = initCommandDocument(decoder, sectionBuffer);
        } else {
            documentSequence = initDocumentSequence(documentSequenceDecoder, sectionBuffer);
        }
        if (sectionBuffer.hasRemaining()) {
            sectionType = sectionBuffer.get();
            if (sectionType == 0) {
                document = initCommandDocument(decoder, sectionBuffer);
            } else {
                documentSequence = initDocumentSequence(documentSequenceDecoder, sectionBuffer);
            }
        }
    }

    private List<S> initDocumentSequence(final Decoder<S> documentSequenceDecoder, final ByteBuf sectionBuffer) {
        ByteBufferBsonInput input = new ByteBufferBsonInput(sectionBuffer);
        int startPosition = input.getPosition();
        int size = input.readInt32();
        documentSequenceName = input.readCString();
        List<S> documentSequence = new ArrayList<S>();

        BsonInput bsonInput = new ByteBufferBsonInput(sectionBuffer);
        while (startPosition + size > sectionBuffer.position()) {
            BsonBinaryReader reader = new BsonBinaryReader(bsonInput);
            try {
                documentSequence.add(documentSequenceDecoder.decode(reader, DecoderContext.builder().build()));
            } finally {
                reader.close();
            }
        }

        return documentSequence;
    }

    private D initCommandDocument(final Decoder<D> decoder, final ByteBuf sectionBuffer) {
        documentByteBuf = sectionBuffer.duplicate();
        BsonInput bsonInput = new ByteBufferBsonInput(sectionBuffer);
        BsonBinaryReader reader = new BsonBinaryReader(bsonInput);
        try {
            return decoder.decode(reader, DecoderContext.builder().build());
        } finally {
            reader.close();
        }
    }

    public ByteBuf getDocumentByteBuf() {
        return documentByteBuf.duplicate();
    }

    public D getDocument() {
        return document;
    }

    public List<S> getDocumentSequence() {
        return documentSequence;
    }

    public String getSequenceName() {
        return documentSequenceName;
    }
}
