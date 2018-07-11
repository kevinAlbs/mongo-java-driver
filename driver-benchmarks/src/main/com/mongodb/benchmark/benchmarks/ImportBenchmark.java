/*
 * Copyright 2016 MongoDB, Inc.
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
 *
 */

package com.mongodb.benchmark.benchmarks;

import com.mongodb.benchmark.framework.BenchmarkResult;
import com.mongodb.benchmark.framework.BenchmarkRunner;
import com.mongodb.benchmark.framework.TextBasedBenchmarkResultWriter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import org.bson.RawBsonDocument;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.RawBsonDocumentCodec;
import org.bson.json.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImportBenchmark extends AbstractMongoBenchmark {
    private MongoDatabase database;

    private MongoCollection<RawBsonDocument> collection;

    private ExecutorService fileReadingService;
    private ExecutorService documentWritingService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        database = client.getDatabase("perftest");
        collection = database.getCollection("corpus", RawBsonDocument.class);

        database.drop();

        fileReadingService = Executors.newFixedThreadPool(4);
        documentWritingService = Executors.newFixedThreadPool(4);
    }

    @Override
    public void before() throws Exception {
        super.before();

        collection.drop();
        database.createCollection(collection.getNamespace().getCollectionName());

    }

    @Override
    public void tearDown() throws Exception {
        fileReadingService.shutdown();
        documentWritingService.shutdown();
        fileReadingService.awaitTermination(1, TimeUnit.MINUTES);
        documentWritingService.awaitTermination(1, TimeUnit.MINUTES);

        super.tearDown();
    }

    @Override
    public String getName() {
        return "LDJSON multi-file import";
    }


    @Override
    public void run() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(500);

        for (int i = 0; i < 50; i++) {
            fileReadingService.submit(importJsonFile(latch, i));
        }

        latch.await(1, TimeUnit.MINUTES);
    }

    private Runnable importJsonFile(final CountDownLatch latch, final int fileId) {
        final RawBsonDocumentCodec codec = new RawBsonDocumentCodec();
        return new Runnable() {
            @Override
            public void run() {
                try {
                    String resourcePath = "parallel/gridfs_multi/file" + String.format("%02d", fileId) + ".txt";
                    BufferedReader reader = new BufferedReader(readFromRelativePath(resourcePath), 1024 * 64);
                    try {
                        String json;
                        List<RawBsonDocument> documents = new ArrayList<RawBsonDocument>(1000);
                        while ((json = reader.readLine()) != null) {
                            RawBsonDocument document = codec.decode(new JsonReader(json), DecoderContext.builder().build());
                            documents.add(document);
                            if (documents.size() == 1000) {
                                final List<RawBsonDocument> documentsToInsert = documents;
                                documentWritingService.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        collection.insertMany(documentsToInsert, new InsertManyOptions().ordered(false));
                                        latch.countDown();
                                    }
                                });
                                documents = new ArrayList<RawBsonDocument>(1000);
                            }
                        }
                        if (!documents.isEmpty()) {
                            throw new IllegalStateException("Document count not a multiple of 1000");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        reader.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public int getBytesPerRun() {
        return 557610482;
    }

    public static void main(String[] args) throws Exception {
        BenchmarkResult benchmarkResult = new BenchmarkRunner(new ImportBenchmark(), 10, 100).run();
        new TextBasedBenchmarkResultWriter(System.out, true, true).write(benchmarkResult);
    }
}
