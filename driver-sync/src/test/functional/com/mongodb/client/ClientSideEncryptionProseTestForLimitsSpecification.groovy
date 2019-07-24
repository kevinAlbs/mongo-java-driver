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
import org.bson.BsonDocument
import org.bson.BsonMaximumSizeExceededException
import org.bson.BsonString

import static com.mongodb.ClusterFixture.isNotAtLeastJava8
import static com.mongodb.ClusterFixture.serverVersionAtLeast
import static com.mongodb.client.Fixture.getDefaultDatabaseName
import static com.mongodb.client.Fixture.getMongoClient
import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder
import static java.util.Collections.singletonMap
import static org.junit.Assume.assumeFalse
import static org.junit.Assume.assumeTrue

class ClientSideEncryptionProseTestForLimitsSpecification extends FunctionalSpecification {

    private final MongoNamespace keyVaultNamespace = new MongoNamespace('test.datakeys')
    private final MongoNamespace autoEncryptingCollectionNamespace = new MongoNamespace(getDefaultDatabaseName(),
            'ClientSideEncryptionProseTestSpecification')
    private final MongoCollection dataKeyCollection = getMongoClient()
            .getDatabase(keyVaultNamespace.databaseName).getCollection(keyVaultNamespace.collectionName, BsonDocument)
    private final MongoCollection<BsonDocument> dataCollection = getMongoClient()
            .getDatabase(autoEncryptingCollectionNamespace.databaseName).getCollection(autoEncryptingCollectionNamespace.collectionName,
            BsonDocument)

    private MongoClient autoEncryptingClient
    private MongoCollection<BsonDocument> autoEncryptingDataCollection

    def setup() {
        assumeFalse(isNotAtLeastJava8())
        assumeTrue(serverVersionAtLeast(4, 2))
        dataKeyCollection.drop()
        dataCollection.drop()

        dataKeyCollection.insertOne(
                BsonDocument.parse('''
{
    "status": {
        "$numberInt": "1"
    }, 
    "_id": {
        "$binary": {
            "base64": "LOCALAAAAAAAAAAAAAAAAA==", 
            "subType": "04"
        }
    }, 
    "masterKey": {
        "provider": "local"
    }, 
    "updateDate": {
        "$date": {
            "$numberLong": "1557827033449"
        }
    }, 
    "keyMaterial": {
        "$binary": {
            "base64": "Ce9HSz/HKKGkIt4uyy+jDuKGA+rLC2cycykMo6vc8jXxqa1UVDYHWq1r+vZKbnnSRBfB981akzRKZCFpC05CTyFqDhXv6OnMjpG97OZEREGIsHEYiJkBW0jJJvfLLgeLsEpBzsro9FztGGXASxyxFRZFhXvHxyiLOKrdWfs7X1O/iK3pEoHMx6uSNSfUOgbebLfIqW7TO++iQS5g1xovXA==", 
            "subType": "00"
        }
    }, 
    "creationDate": {
        "$date": {
            "$numberLong": "1557827033449"
        }
    },
    "keyAltNames": [ "local" ]
}'''))

        def providerProperties =
                ['local': ['key': Base64.getDecoder().decode('Mng0NCt4ZHVUYUJCa1kxNkVyNUR1QURhZ2h2UzR2d2RrZzh0cFBwM3R6NmdWMDFBMUN'
                        + '3YkQ5aXRRMkhGRGdQV09wOGVNYUMxT2k3NjZKelhaQmRCZGJkTXVyZG9uSjFk')]
                ]

        autoEncryptingClient = MongoClients.create(getMongoClientSettingsBuilder()
                .autoEncryptionSettings(AutoEncryptionSettings.builder()
                        .keyVaultNamespace(keyVaultNamespace.fullName)
                        .kmsProviders(providerProperties)
                        .schemaMap(singletonMap(autoEncryptingCollectionNamespace.fullName,
                                BsonDocument.parse(
                                        '''
   {
     "bsonType": "object",
     "properties": {
       "encrypted": {
         "encrypt": {
           "keyId": [
             {
               "$binary": {
                 "base64": "LOCALAAAAAAAAAAAAAAAAA==",
                 "subType": "04"
               }
             }
           ],
           "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic",
           "bsonType": "string"
         }
       }
     }
   }''')))
                        .build())
                .build())

        autoEncryptingDataCollection = autoEncryptingClient.getDatabase(autoEncryptingCollectionNamespace.databaseName)
                .getCollection(autoEncryptingCollectionNamespace.collectionName, BsonDocument)
   }

    def 'client encryption limits prose test'() {
        when:
        autoEncryptingDataCollection.insertOne(
                new BsonDocument('_id', new BsonString('no_encryption_equal_2mib'))
                .append('unencrypted', new BsonString('a'.multiply(2097006)))
        )

        then:
        noExceptionThrown()

        when:
        autoEncryptingDataCollection.insertOne(
                new BsonDocument('_id', new BsonString('no_encryption_over_2mib'))
                        .append('unencrypted', new BsonString('a'.multiply(2097097)))
        )

        then:
        thrown(BsonMaximumSizeExceededException)
    }
}
