/*
 * Copyright 2018 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.embedded.client;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.JsonPoweredTestHelper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

// See https://github.com/mongodb/specifications/tree/master/source/crud/tests
@RunWith(Parameterized.class)
public class EmbeddedCrudTest {

    private static final String DEFAULT_DATABASE_NAME = "JavaDriverTest";
    private final static String EMBEDDED_OPTION_PREFIX = "org.mongodb.test.embedded";
    private final String filename;
    private final String description;
    private final BsonArray data;
    private final BsonDocument definition;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<BsonDocument> collection;
    private JsonPoweredCrudTestHelper helper;

    public EmbeddedCrudTest(final String filename, final String description, final BsonArray data, final BsonDocument definition) {
        this.filename = filename;
        this.description = description;
        this.data = data;
        this.definition = definition;
    }
//
//    @BeforeClass
//    public static void classSetUp() {
//        System.setProperty("java.class.path", format("%s%s%s", System.getProperty("jna.library.path"), File.pathSeparatorChar,
//                System.getProperty("java.class.path")));
//        System.setProperty("java.library.path", format("%s%s%s", System.getProperty("jna.library.path"), File.pathSeparatorChar,
//                System.getProperty("java.library.path")));
//
//        for (Map.Entry<Object, Object> objectObjectEntry : System.getProperties().entrySet()) {
//            System.out.println(format("> %s : %s", objectObjectEntry.getKey(), objectObjectEntry.getValue()));
//        }
//    }

    @Before
    public void setUp() {
        List<String> embeddedServerOptions = getEmbeddedServerOptions();
        assumeTrue("Requires `org.mongodb.test.embedded.` settings to be configured.", !embeddedServerOptions.isEmpty());
        mongoClient = MongoClients.create(embeddedServerOptions);
        database = mongoClient.getDatabase(DEFAULT_DATABASE_NAME);
        collection = database.getCollection(getClass().getName(), BsonDocument.class);
        List<BsonDocument> documents = new ArrayList<BsonDocument>();
        for (BsonValue document: data) {
            documents.add(document.asDocument());
        }
        collection.insertMany(documents);
        helper = new JsonPoweredCrudTestHelper(description, collection);
    }

    private List<String> getEmbeddedServerOptions() {
        List<String> embeddedServerOptions = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(EMBEDDED_OPTION_PREFIX)) {
                key = key.substring(EMBEDDED_OPTION_PREFIX.length(), key.length());
                String value = entry.getValue().toString();
                embeddedServerOptions.add(format("--%s=%s", key, value));
            }

        }
        return embeddedServerOptions;
    }

    @Test
    public void shouldPassAllOutcomes() {
        BsonDocument outcome = helper.getOperationResults(definition.getDocument("operation"));
        BsonDocument expectedOutcome = definition.getDocument("outcome");

        // Hack to workaround the lack of upsertedCount
        BsonValue expectedResult = expectedOutcome.get("result");
        BsonValue actualResult = outcome.get("result");
        if (actualResult.isDocument()
                && actualResult.asDocument().containsKey("upsertedCount")
                && actualResult.asDocument().getNumber("upsertedCount").intValue() == 0
                && !expectedResult.asDocument().containsKey("upsertedCount")) {
            expectedResult.asDocument().append("upsertedCount", actualResult.asDocument().get("upsertedCount"));
        }

        // Remove insertCount
        if (actualResult.isDocument() && actualResult.asDocument().containsKey("insertedCount")) {
            actualResult.asDocument().remove("insertedCount");
        }

        assertEquals(description, expectedResult, actualResult);

        if (expectedOutcome.containsKey("collection")) {
            assertCollectionEquals(expectedOutcome.getDocument("collection"));
        }
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        for (File file : JsonPoweredTestHelper.getTestFiles("/crud")) {
            BsonDocument testDocument = JsonPoweredTestHelper.getTestDocument(file);
//            if (testDocument.containsKey("minServerVersion")
//                    && serverVersionLessThan(testDocument.getString("minServerVersion").getValue())) {
//                continue;
//            }
//            if (testDocument.containsKey("maxServerVersion")
//                        && serverVersionGreaterThan(testDocument.getString("maxServerVersion").getValue())) {
//                continue;
//            }
            for (BsonValue test: testDocument.getArray("tests")) {
                data.add(new Object[]{file.getName(), test.asDocument().getString("description").getValue(),
                        testDocument.getArray("data"), test.asDocument()});
            }
        }
        return data;
    }

    private void assertCollectionEquals(final BsonDocument expectedCollection) {
        MongoCollection<BsonDocument> collectionToCompare = collection;
        if (expectedCollection.containsKey("name")) {
            collectionToCompare = database.getCollection(expectedCollection.getString("name").getValue(), BsonDocument.class);
        }
        assertEquals(description, expectedCollection.getArray("data"), collectionToCompare.find().into(new BsonArray()));
    }
}
