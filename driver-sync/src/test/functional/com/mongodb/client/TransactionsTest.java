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

import com.mongodb.Block;
import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoNamespace;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.test.CollectionHelper;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.TestCommandListener;
import com.mongodb.event.CommandEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.lang.Nullable;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
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
import java.util.concurrent.TimeUnit;

import static com.mongodb.ClusterFixture.isDiscoverableReplicaSet;
import static com.mongodb.ClusterFixture.serverVersionAtLeast;
import static com.mongodb.client.CommandMonitoringTestHelper.getExpectedEvents;
import static com.mongodb.client.Fixture.getDefaultDatabaseName;
import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

// See https://github.com/mongodb/specifications/tree/master/source/transactions/tests
@RunWith(Parameterized.class)
public class TransactionsTest {
    private final String filename;
    private final String description;
    private final String databaseName;
    private final BsonArray data;
    private final BsonDocument definition;
    private JsonPoweredCrudTestHelper helper;
    private final TestCommandListener commandListener;
    private MongoClient mongoClient;
    private CollectionHelper<Document> collectionHelper;
    private Map<String, ClientSession> sessionsMap;

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
        this.data = data;
        this.definition = definition;
        this.commandListener = new TestCommandListener();
    }

    @Before
    public void setUp() {
        assumeTrue(canRunTests());
        assumeTrue(!definition.containsKey("skipReason"));

        // TODO: make these pass
//        assumeTrue(!filename.startsWith("auto-start"));
        assumeTrue(!filename.startsWith("delete"));
        assumeTrue(!filename.startsWith("update"));
        assumeTrue(!filename.startsWith("reads"));
        assumeTrue(!filename.startsWith("snapshot-reads"));
        assumeTrue(!filename.startsWith("read-pref"));
        assumeTrue(!filename.startsWith("write-concern"));

        String collectionName = "test";
        collectionHelper = new CollectionHelper<Document>(new DocumentCodec(), new MongoNamespace(databaseName, collectionName));

        collectionHelper.create(collectionName, new CreateCollectionOptions(), WriteConcern.MAJORITY);

        if (!data.isEmpty()) {
            List<BsonDocument> documents = new ArrayList<BsonDocument>();
            for (BsonValue document : data) {
                documents.add(document.asDocument());
            }

            collectionHelper.insertDocuments(documents, WriteConcern.MAJORITY);
        }

        mongoClient = MongoClients.create(getMongoClientSettingsBuilder()
                .addCommandListener(commandListener)
                .applyToSocketSettings(new Block<SocketSettings.Builder>() {
                    @Override
                    public void apply(final SocketSettings.Builder builder) {
                        builder.readTimeout(5, TimeUnit.SECONDS);
                    }
                })
                .build());

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        helper = new JsonPoweredCrudTestHelper(description, database.getCollection(collectionName, BsonDocument.class));

        ClientSession sessionZero = createSession("session0");
        ClientSession sessionOne = createSession("session1");

        sessionsMap = new HashMap<String, ClientSession>();
        sessionsMap.put("session0", sessionZero);
        sessionsMap.put("session1", sessionOne);
    }

    private ClientSession createSession(final String sessionName) {
        BsonDocument optionsDocument = definition.getDocument("sessionOptions", new BsonDocument())
                .getDocument(sessionName, new BsonDocument());
        ClientSessionOptions options = ClientSessionOptions.builder().causallyConsistent(false)
                .autoStartTransaction(optionsDocument.getBoolean("autoStartTransaction", BsonBoolean.FALSE).getValue())
                .build();
        return mongoClient.startSession(options);
    }

    @After
    public void cleanUp() {
        if (mongoClient != null) {
            // TODO: remove this once it's added back to #shouldPassAllOutcomes
            closeAllSessions();
            mongoClient.close();
        }
    }

    private void closeAllSessions() {
        for (ClientSession cur : sessionsMap.values()) {
            if (cur.hasActiveTransaction()) {
                // TODO: remove this once ClientSession#close implicitly abort an open transaction
                try {
                    cur.abortTransaction();
                } catch (Exception e) {
                    // TODO: fail on this?
                }
            }
            cur.close();
        }
    }

    @Test
    public void shouldPassAllOutcomes() {
        try {
            for (BsonValue cur : definition.getArray("operations")) {
                BsonDocument operation = cur.asDocument();
                String operationName = operation.getString("name").getValue();
                BsonValue expectedResult = operation.get("result", null);
                ClientSession clientSession = operation.getDocument("arguments").containsKey("session")
                        ? sessionsMap.get(operation.getDocument("arguments").getString("session").getValue()) : null;
                try {
                    if (operationName.equals("startTransaction")) {
                        TransactionOptions.Builder builder = TransactionOptions.builder();
                        if (operation.containsKey("writeConcern")) {
                            builder.writeConcern(helper.getWriteConcern(operation));
                        }
                        nonNullClientSession(clientSession).startTransaction(builder.build());
                    } else if (operationName.equals("commitTransaction")) {
                        nonNullClientSession(clientSession).commitTransaction();
                    } else if (operationName.equals("abortTransaction")) {
                        nonNullClientSession(clientSession).abortTransaction();
                    } else {
                        BsonDocument actualOutcome = helper.getOperationResults(operation, clientSession);
                        BsonValue actualResult = actualOutcome.get("result");

                        assertEquals("Expected operation result differs from actual", expectedResult, actualResult);
                    }
                } catch (RuntimeException e) {
//                    for (CommandEvent curEvent : getCommandStartedEvents()) {
//                        System.out.println(((CommandStartedEvent) curEvent).getCommand());
//                    }
                    if (expectedResult == null
                            || !expectedResult.isDocument()
                            || !expectedResult.asDocument().containsKey("errorContains")) {
                        throw e;
                    }
                    assertTrue(e.getMessage().toLowerCase().contains(expectedResult.asDocument()
                            .getString("errorContains").getValue().toLowerCase()));
                }
            }
        } finally {
            // TODO: request spec change for this
            closeAllSessions();
        }

        if (definition.containsKey("expectations")) {
            // TODO: null operation may cause test failures, since it's used to grab the read preference
            // TODO: though read-pref.json doesn't declare expectations, so maybe not
            List<CommandEvent> expectedEvents = getExpectedEvents(definition.getArray("expectations"), databaseName, null);
            List<CommandEvent> events = getCommandStartedEvents();

            // TODO: enable this
//            assertEventsEquality(expectedEvents, events);
        }

        BsonDocument expectedOutcome = definition.getDocument("outcome", new BsonDocument());
        if (expectedOutcome.containsKey("collection")) {
            List<BsonDocument> collectionData = collectionHelper.find(new BsonDocumentCodec());
            assertEquals(expectedOutcome.getDocument("collection").getArray("data").getValues(), collectionData);
        }
    }

    private ClientSession nonNullClientSession(@Nullable final ClientSession clientSession) {
        if (clientSession == null) {
            throw new IllegalArgumentException("clientSession can't be null in this context");
        }
        return clientSession;
    }

    private List<CommandEvent> getCommandStartedEvents() {
        List<CommandEvent> commandStartedEvents = new ArrayList<CommandEvent>();
        for (CommandEvent cur : commandListener.getEvents()) {
            if (cur instanceof CommandStartedEvent) {
                commandStartedEvents.add(cur);
            }
        }
        return commandStartedEvents;
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
