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

package com.mongodb.client;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.client.test.CollectionHelper;
import com.mongodb.connection.TestCommandListener;
import com.mongodb.event.CommandEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.session.ClientSession;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.junit.After;
import org.junit.AfterClass;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.mongodb.ClusterFixture.isDiscoverableReplicaSet;
import static com.mongodb.ClusterFixture.serverVersionAtLeast;
import static com.mongodb.client.CommandMonitoringTestHelper.getExpectedEvents;
import static com.mongodb.client.Fixture.getDefaultDatabaseName;
import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// See https://github.com/mongodb/specifications/tree/master/source/transactions/tests
@RunWith(Parameterized.class)
public class TransactionsTest {
    private final String filename;
    private final String description;
    private final String databaseName;
    private final String collectionName;
    private final BsonArray data;
    private final BsonDocument definition;
    private MongoCollection<BsonDocument> collection;
    private JsonPoweredCrudTestHelper helper;
    private final TestCommandListener commandListener;
    private MongoClient mongoClient;

    @BeforeClass
    public static void beforeClass() {
    }

    @AfterClass
    public static void afterClass() {
    }

    public TransactionsTest(final String filename, final String description, final BsonArray data, final BsonDocument definition) {
        this.filename = filename;
        this.description = description;
        this.databaseName = getDefaultDatabaseName();
        this.collectionName = filename.substring(0, filename.lastIndexOf("."));
        this.data = data;
        this.definition = definition;
        this.commandListener = new TestCommandListener();
    }

    @Before
    public void setUp() {
        // TODO: re-enable this
//        assumeTrue(canRunTests());

        mongoClient = MongoClients.create(getMongoClientSettingsBuilder()
                .addCommandListener(commandListener)
                .build());

        List<BsonDocument> documents = new ArrayList<BsonDocument>();
        for (BsonValue document : data) {
            documents.add(document.asDocument());
        }
        CollectionHelper<Document> collectionHelper = new CollectionHelper<Document>(new DocumentCodec(),
                new MongoNamespace(databaseName, collectionName));

        collectionHelper.drop();
        if (!documents.isEmpty()) {
            collectionHelper.insertDocuments(documents, WriteConcern.MAJORITY);
        }

        collection = mongoClient.getDatabase(databaseName).getCollection(collectionName, BsonDocument.class);
        helper = new JsonPoweredCrudTestHelper(description, collection);
    }

    @After
    public void cleanUp() {
    }

    @Test
    public void shouldPassAllOutcomes() {
        // TODO: session options
        ClientSession sessionZero = mongoClient.startSession(ClientSessionOptions.builder().build());
        ClientSession sessionOne = mongoClient.startSession(ClientSessionOptions.builder().build());

        Map<String, ClientSession> sessionsMap = new HashMap<String, ClientSession>();
        sessionsMap.put("session0", sessionZero);
        sessionsMap.put("session1", sessionOne);

        BsonArray operations = definition.getArray("operations");

        for (BsonValue cur : operations) {
            BsonDocument operation = cur.asDocument();
            String operationName = operation.getString("name").getValue();
            BsonValue expectedResult = operation.get("result", null);
            ClientSession clientSession = operation.getDocument("arguments").containsKey("session")
                    ? sessionsMap.get(operation.getDocument("arguments").getString("session").getValue()) : null;
            try {
                if (operationName.equals("startTransaction")) {
                    // TODO: transaction options
                    nonNullClientSession(clientSession).startTransaction();
                } else if (operationName.equals("commitTransaction")) {
                    nonNullClientSession(clientSession).commitTransaction();
                } else if (operationName.equals("abortTransaction")) {
                    nonNullClientSession(clientSession).abortTransaction();
                } else {
                    BsonDocument actualOutcome = helper.getOperationResults(operation, clientSession);
                    BsonValue actualResult = actualOutcome.get("result");

                    assertEquals("Expected operation result differs from actual", expectedResult, actualResult);
                }
            } catch (Exception e) {
                if (expectedResult == null || !expectedResult.asDocument().containsKey("errorContains"))  {
                    fail("Unexpected operation failure: " + e);
                }
                assertTrue(e.getMessage().contains(expectedResult.asDocument().getString("errorContains").getValue()));
            }
        }

        if (definition.containsKey("expectations")) {
            // TODO: null operation may cause test failures, since it's used to grab the read preference
            // TODO: though read-pref.json doesn't declare expectations, so maybe not
            List<CommandEvent> expectedEvents = getExpectedEvents(definition.getArray("expectations"), databaseName, null);
            List<CommandEvent> events = getCommandStartedEvents();

            // TODO: re-enable
//            assertEventsEquality(expectedEvents, events);
        }

        BsonDocument expectedOutcome = definition.getDocument("outcome", new BsonDocument());
        if (expectedOutcome.containsKey("collection")) {
            List<BsonDocument> collectionData = collection.withDocumentClass(BsonDocument.class).find().into(new ArrayList<BsonDocument>());
            assertEquals(expectedOutcome.getDocument("collection").getArray("data").getValues(), collectionData);
        }

    }

    private ClientSession nonNullClientSession(final ClientSession clientSession) {
        if (clientSession == null) {
            throw new IllegalArgumentException("clientSession can't be null in this context");
        }
        return clientSession;
    }

    private List<CommandEvent> getCommandStartedEvents() {
        return commandListener.getEvents().stream().filter(new Predicate<CommandEvent>() {
            @Override
            public boolean test(final CommandEvent commandEvent) {
                return commandEvent instanceof CommandStartedEvent;
            }
        }).collect(Collectors.<CommandEvent>toList());
    }

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        for (File file : JsonPoweredTestHelper.getTestFiles("/transactions")) {
            BsonDocument testDocument = JsonPoweredTestHelper.getTestDocument(file);
            for (BsonValue test : testDocument.getArray("tests")) {
                data.add(new Object[]{file.getName(), test.asDocument().getString("description").getValue(),
                        testDocument.getArray("data"), test.asDocument()});
            }
        }
        return data;
    }

    private boolean canRunTests() {
        return serverVersionAtLeast(3, 7) && isDiscoverableReplicaSet();
    }
}
