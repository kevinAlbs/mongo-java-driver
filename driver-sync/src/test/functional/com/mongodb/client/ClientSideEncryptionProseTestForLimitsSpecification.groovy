/*
 * Copyright 2008-present MongoDB, Inc.
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

package com.mongodb.client

import com.mongodb.AutoEncryptionSettings
import com.mongodb.MongoNamespace
import com.mongodb.event.CommandFailedEvent
import com.mongodb.event.CommandListener
import com.mongodb.event.CommandStartedEvent
import com.mongodb.event.CommandSucceededEvent
import org.bson.BsonDocument
import org.bson.BsonMaximumSizeExceededException
import org.bson.BsonString
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.DecoderContext
import org.bson.json.JsonReader

import java.nio.charset.Charset

import static com.mongodb.ClusterFixture.isNotAtLeastJava8
import static com.mongodb.ClusterFixture.serverVersionAtLeast
import static com.mongodb.client.Fixture.getDefaultDatabaseName
import static com.mongodb.client.Fixture.getMongoClient
import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder
import static java.util.Collections.singletonMap
import static org.junit.Assume.assumeFalse
import static org.junit.Assume.assumeTrue

public class TestCommandListener implements CommandListener {
    public int numInserts = 0;

    @Override
    public void commandStarted(final CommandStartedEvent event) {
        if (event.commandName.equals("insert")) {
            numInserts++;
        }
    }

    @Override
    public void commandSucceeded(final CommandSucceededEvent event) {

    }

    @Override
    public void commandFailed(final CommandFailedEvent event) {

    }
}

class ClientSideEncryptionProseTestForLimitsSpecification extends FunctionalSpecification {
    /* Copied from JSON powered test runner code. TODO: clean up, update paths */
    def getFileAsString(final File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        String ls = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } finally {
            reader.close();
        }
        return stringBuilder.toString();
    }

    def bsonDocumentFromPath(final String path) throws IOException, URISyntaxException {
        File file = new File("/Users/kevinalbertson/code/specifications/source/client-side-encryption/limits/" + path)
        return new BsonDocumentCodec().decode(new JsonReader(getFileAsString(file)), DecoderContext.builder().build())
    }

    private final MongoNamespace keyVaultNamespace = new MongoNamespace('test.datakeys')
    private final MongoNamespace autoEncryptingCollectionNamespace = new MongoNamespace(getDefaultDatabaseName(),
            'ClientSideEncryptionProseTestSpecification')
    private final MongoCollection dataKeyCollection = getMongoClient()
            .getDatabase(keyVaultNamespace.databaseName).getCollection(keyVaultNamespace.collectionName, BsonDocument)
    private final MongoCollection<BsonDocument> dataCollection = getMongoClient()
            .getDatabase(autoEncryptingCollectionNamespace.databaseName).getCollection(autoEncryptingCollectionNamespace.collectionName,
            BsonDocument)
    private TestCommandListener commandListener = new TestCommandListener()

    private MongoClient autoEncryptingClient
    private MongoCollection<BsonDocument> autoEncryptingDataCollection

    def setup() {
        assumeFalse(isNotAtLeastJava8())
        assumeTrue(serverVersionAtLeast(4, 2))
        dataKeyCollection.drop()
        dataCollection.drop()

        dataKeyCollection.insertOne(bsonDocumentFromPath ("limits-key.json"))

        def providerProperties =
                ['local': ['key': Base64.getDecoder().decode('Mng0NCt4ZHVUYUJCa1kxNkVyNUR1QURhZ2h2UzR2d2RrZzh0cFBwM3R6NmdWMDFBMUN'
                        + '3YkQ5aXRRMkhGRGdQV09wOGVNYUMxT2k3NjZKelhaQmRCZGJkTXVyZG9uSjFk')]
                ]

        autoEncryptingClient = MongoClients.create(getMongoClientSettingsBuilder()
                .addCommandListener(commandListener)
                .autoEncryptionSettings(AutoEncryptionSettings.builder()
                        .keyVaultNamespace(keyVaultNamespace.fullName)
                        .kmsProviders(providerProperties)
                        .schemaMap(singletonMap(autoEncryptingCollectionNamespace.fullName, bsonDocumentFromPath("limits-schema.json")))
                        .build())
                .build())

        autoEncryptingDataCollection = autoEncryptingClient.getDatabase(autoEncryptingCollectionNamespace.databaseName)
                .getCollection(autoEncryptingCollectionNamespace.collectionName, BsonDocument)
   }

    def 'client encryption limits prose test'() {
        when:
        autoEncryptingDataCollection.insertOne(
                new BsonDocument('_id', new BsonString('no_encryption_under_2mib'))
                .append('unencrypted', new BsonString('a'.multiply(2097152 - 1000)))
        )

        then:
        noExceptionThrown()

        when:
        autoEncryptingDataCollection.insertOne(
                new BsonDocument('_id', new BsonString('no_encryption_over_2mib'))
                        .append('unencrypted', new BsonString('a'.multiply(2097152)))
        )

        then:
        thrown(BsonMaximumSizeExceededException)

        when:
        autoEncryptingDataCollection.insertOne(
                bsonDocumentFromPath("limits-doc.json").append('_id', new BsonString('encryption_exceeds_2mib')).append('unencrypted', new BsonString('a'.multiply(2097152 - 2000)))
        )

        then:
        noExceptionThrown()

        when:
        LinkedList<BsonDocument> list = new LinkedList<BsonDocument>()
        list.add (new BsonDocument().append("_id", new BsonString("no_encryption_under_2mib_1")).append("unencrypted", new BsonString('a'.multiply(2097152 - 1000))));
        list.add (new BsonDocument().append("_id", new BsonString("no_encryption_under_2mib_2")).append("unencrypted", new BsonString('a'.multiply(2097152 - 1000))));
        commandListener.numInserts = 0; /* reset */
        autoEncryptingDataCollection.insertMany(list)

        then:
        assert (commandListener.numInserts == 2)


        when:
        list = new LinkedList<BsonDocument>()
        list.add (bsonDocumentFromPath("limits-doc.json").append('_id', new BsonString('encryption_exceeds_2mib_1')).append('unencrypted', new BsonString('a'.multiply(2097152 - 2000))))
        list.add (bsonDocumentFromPath("limits-doc.json").append('_id', new BsonString('encryption_exceeds_2mib_2')).append('unencrypted', new BsonString('a'.multiply(2097152 - 2000))))
        commandListener.numInserts = 0; /* reset */
        autoEncryptingDataCollection.insertMany(list)

        then:
        assert (commandListener.numInserts == 2)

    }
}
