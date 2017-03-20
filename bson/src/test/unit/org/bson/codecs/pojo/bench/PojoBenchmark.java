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

package org.bson.codecs.pojo.bench;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonWriter;
import org.bson.ByteBufNIO;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;
import org.bson.io.OutputBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

public final class PojoBenchmark {
    private static final double ONE_BILLION = 1000000000.0; // To convert nanoseconds to seconds
    private static final int NUM_WARMUP_ITERATIONS = 10;
    private static final int NUM_ITERATIONS = 1000;

    public static void main(final String[] args) throws URISyntaxException, IOException {
        CodecRegistry registry = fromProviders(new DocumentCodecProvider(), new ValueCodecProvider(),
                PojoCodecProvider.builder().register(Restaurant.class, Address.class, Grade.class).build());

        final List<Document> documents = getDocuments();
        final Codec<Document> documentCodec = registry.get(Document.class);
        final List<OutputBuffer> buffers = getOutputBuffers(documents, documentCodec);
        final Codec<Restaurant> restaurantCodec = registry.get(Restaurant.class);
        final List<Restaurant> restaurants = getRestaurants(documents, documentCodec, restaurantCodec);

        System.out.println(format("Testing: %s documents/restaurants", documents.size()));

        report("Documents encoding ", timeIt(new Block() {
            @Override
            public void run() {
                encodeAll(documentCodec, documents);
            }
        }));
        report("Documents decoding ", timeIt(new Block() {
            @Override
            public void run() {
                decodeAll(documentCodec, buffers);
            }
        }));
        report("Documents roundtrip", timeIt(new Block() {
            @Override
            public void run() {
                roundTripAll(documentCodec, documents);
            }
        }));

        System.out.println("----------");
        report("Restaurants encoding ", timeIt(new Block() {
            @Override
            public void run() {
                encodeAll(restaurantCodec, restaurants);
            }
        }));
        report("Restaurants decoding ", timeIt(new Block() {
            @Override
            public void run() {
                decodeAll(restaurantCodec, buffers);
            }
        }));
        report("Restaurants roundtrip", timeIt(new Block() {
            @Override
            public void run() {
                roundTripAll(restaurantCodec, restaurants);
            }
        }));
    }

    static List<Long> timeIt(final Block block) {
        for (int i = 0; i < NUM_WARMUP_ITERATIONS; i++) {
            block.run();
        }

        List<Long> elapsedTimeNanosList = new ArrayList<Long>(NUM_ITERATIONS);
        for (int i = 0; i < NUM_ITERATIONS; i++) {

            long startTimeNanos = System.nanoTime();
            block.run();
            long elapsedTimeNanos = System.nanoTime() - startTimeNanos;
            elapsedTimeNanosList.add(elapsedTimeNanos);
        }
        Collections.sort(elapsedTimeNanosList);
        return elapsedTimeNanosList;
    }

    static List<Document> getDocuments() throws URISyntaxException, IOException {
        List<Document> documents = new ArrayList<Document>();
        URI uri = PojoBenchmark.class.getResource("/restaurants.json").toURI();

        BufferedReader br = new BufferedReader(new FileReader(new File(uri)));
        String line;
        while ((line = br.readLine()) != null) {
            documents.add(Document.parse(line));
        }
        return documents;
    }

    static List<OutputBuffer>  getOutputBuffers(final List<Document> documents, final Codec<Document> documentCodec) {
        List<OutputBuffer> buffers = new ArrayList<OutputBuffer>();
        for (Document document : documents) {
            buffers.add(encode(documentCodec, document));
        }
        return buffers;
    }

    static List<Restaurant>  getRestaurants(final List<Document> documents, final Codec<Document> documentCodec,
                                           final Codec<Restaurant> restaurantCodec) {
        List<Restaurant> restaurants = new ArrayList<Restaurant>();
        for (OutputBuffer outputBuffer : getOutputBuffers(documents, documentCodec)) {
            restaurants.add(decode(restaurantCodec, outputBuffer));
        }
        return restaurants;
    }

    static <T> void roundTripAll(final Codec<T> codec, final List<T> values) {
        for (T value : values) {
            decode(codec, encode(codec, value));
        }
    }

    static <T> void encodeAll(final Codec<T> codec, final List<T> values) {
        for (T value : values) {
            encode(codec, value);
        }
    }

    static <T> OutputBuffer encode(final Codec<T> codec, final T value) {
        OutputBuffer buffer = new BasicOutputBuffer();
        BsonWriter writer = new BsonBinaryWriter(buffer);
        codec.encode(writer, value, EncoderContext.builder().build());
        return buffer;
    }

    static <T> void decodeAll(final Codec<T> codec, final List<OutputBuffer> buffers) {
        for (OutputBuffer buffer : buffers) {
            decode(codec, buffer);
        }
    }

    static <T> T decode(final Codec<T> codec, final OutputBuffer buffer) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(new ByteBufNIO(ByteBuffer.wrap(buffer.toByteArray()))));
        return codec.decode(reader, DecoderContext.builder().build());
    }

    static void report(final String testType, final List<Long> elapsedTimeNanosList) {
        Double fiftiethPercentile = elapsedTimeNanosList.get(Math.max(0, ((int) (NUM_ITERATIONS * 50 / 100.0)) - 1)) / ONE_BILLION;
        Double seventyFifthPercentile = elapsedTimeNanosList.get(Math.max(0, ((int) (NUM_ITERATIONS * 75 / 100.0)) - 1)) / ONE_BILLION;
        Double niniethPercentile = elapsedTimeNanosList.get(Math.max(0, ((int) (NUM_ITERATIONS * 90 / 100.0)) - 1)) / ONE_BILLION;

        System.out.print(" >> " + testType);
        System.out.print(format(": %.3f  :  %.3f  :  %.3f \n", fiftiethPercentile, seventyFifthPercentile, niniethPercentile));
    }

    private interface Block {
        void run();
    }

    private PojoBenchmark() {
    }
}
