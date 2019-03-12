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

import com.mongodb.KeyVaultEncryptionOptions
import com.mongodb.MongoNamespace
import com.mongodb.client.model.vault.DataKeyOptions
import com.mongodb.client.model.vault.EncryptOptions
import com.mongodb.client.vault.KeyVaults
import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import org.bson.BsonDocument
import org.bson.BsonString

import static com.mongodb.client.Fixture.getMongoClient
import static com.mongodb.client.Fixture.getMongoClientSettings
import static org.junit.Assume.assumeTrue

class KeyVaultSpecification extends FunctionalSpecification {

    private final MongoNamespace keyVaultNamespace = new MongoNamespace('test.datakeys')
    private final MongoCollection dataKeyCollection = getMongoClient()
            .getDatabase(keyVaultNamespace.databaseName).getCollection(keyVaultNamespace.collectionName, BsonDocument)
    private final Map<String, Map<String, ? extends Object>> localProviderProperties =
            ['local':
                     ['key': new byte[96]]]
    private final Map<String, Map<String, Object>> awsProviderProperties =
            ['aws': ['accessKeyId'    : System.getProperty('org.mongodb.test.awsAccessKeyId'),
                     'secretAccessKey': System.getProperty('org.mongodb.test.awsSecretAccessKey')]]
    private final BsonDocument awsMasterKey = new BsonDocument('region', new BsonString('us-east-1'))
            .append('key', new BsonString('arn:aws:kms:us-east-1:579766882180:key/89fcc2c4-08b0-4bd9-9f25-e30687b580d0'))

    private final BsonDocument localDataKeyDocument = BsonDocument.parse(
    '''
{
  "_id": {
    "$binary": {
        "base64": "YWFhYWFhYWFhYWFhYWFhYQ==",
        "subType": "04"
    }
},
  "keyMaterial": {
    "$binary": {
      "base64": "db27rshiqK4Jqhb2xnwK4RfdFb9JuKeUe6xt5aYQF4o62tS75b7B4wxVN499gND9UVLUbpVKoyUoaZAeA895OENP335b8n8OwchcTFqS44t+P3zmhteYUQLIWQXaIgon7gEgLeJbaDHmSXS6/7NbfDDFlB37N7BP/2hx1yCOTN6NG/8M1ppw3LYT3CfP6EfXVEttDYtPbJpbb7nBVlxD7w==",
      "subType": "00"
    }
  },
  "creationDate": { "$date": { "$numberLong": "1232739599082000" } },
  "updateDate": { "$date": { "$numberLong": "1232739599082000" } },
  "status": { "$numberInt": "0" },
  "masterKey": { "provider": "local" }
}
''')

    private final BsonDocument awsDataKeyDocument = BsonDocument.parse(
 '''
{
    "_id": {
        "$binary": {
            "base64": "AAAAAAAAAAAAAAAAAAAAAA==",
            "subType": "04"
        }
    },
    "version": {
        "$numberLong": "0"
    },
    "keyAltNames": [ "altname1", "altname2" ],
    "keyMaterial": {
        "$binary": {
            "base64": "AQICAHhQNmWG2CzOm1dq3kWLM+iDUZhEqnhJwH9wZVpuZ94A8gG2Qei6UQdbOR5RWhPSrNwnAAAAwjCBvwYJKoZIhvcNAQcGoIGxMIGuAgEAMIGoBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDMJ2xcv8wKZzqTIX/gIBEIB7dNUvInthJHEd55QaEyVYSacoPvMlx2wzhxW+E6MBcfP+nCrzByLkqyHRhWs5NEvrOBT2nuc87iZIuK/WNR/pl5eK1xQ/8Cy0GrMfD3GIjYDlZ6aWc06cJvwvZd3Cgqx0pQnunwNr2EfStTGj7gHW23kzkfpxDiphqPnH",
            "subType": "00"
        }
    },
    "creationDate": {
        "$date": {
            "$numberLong": "1553026537755"
        }
    },
    "updateDate": {
        "$date": {
            "$numberLong": "1553026537755"
        }
    },
    "status": {
        "$numberInt": "1"
    },
    "masterKey": {
        "key": "arn:aws:kms:us-east-1:579766882180:key/89fcc2c4-08b0-4bd9-9f25-e30687b580d0",
        "region": "us-east-1",
        "provider": "aws"
    }
}'''
    )

    def setup() {
        assumeTrue('Key vault tests disabled',
                System.getProperty('org.mongodb.test.awsAccessKeyId') != null
                        && !System.getProperty('org.mongodb.test.awsAccessKeyId').isEmpty())
        dataKeyCollection.drop()
    }

    /*
    1. Should create a data key with the "local" KMS provider
       Given: a key vault with local KMS provider
       When:  a data key is created with the local KMS provider
       Then:  the returned id is a non-nil BSON binary with subtype 4
              a document with an _id equal to the returned id is created in the key vault namespace
     */
    def 'should create local data key'() {
        given:
        def keyVault = KeyVaults.create(new KeyVaultEncryptionOptions(getMongoClientSettings(), keyVaultNamespace.fullName,
                localProviderProperties))

        when:
        def id = keyVault.createDataKey('local')

        then:
        id != null
        id.type == BsonBinarySubType.UUID_STANDARD.value
        dataKeyCollection.find().first().getBinary('_id') == id
    }

    /*
    2. Should create a data key with the "aws" KMS provider
       Given: a key vault with aws KMS provider
       When:  a data key is created with the local KMS provider
       Then:  the returned id is a non-nil BSON binary with subtype 4
              a document with an _id equal to the returned id is created in the key vault namespace
     */
    def 'should create aws data key'() {
        given:
        def keyVault = KeyVaults.create(new KeyVaultEncryptionOptions(getMongoClientSettings(), keyVaultNamespace.fullName,
                awsProviderProperties))

        when:
        def id = keyVault.createDataKey('aws', new DataKeyOptions().masterKey(awsMasterKey))

        then:
        id != null
        id.type == BsonBinarySubType.UUID_STANDARD.value
        dataKeyCollection.find().first().getBinary('_id') == id
    }

    /*
    3. Should explicity encrypt and decrypt with "local" KMS provider
       Given: a key vault with local KMS provider
              a key vault populated with a known local key document
       When: the string 'hello' is encrypted with the known local key and known initialization vector using deterministic algorithm
       Then: the encrypted value is equal to a known BSON binary of subtype 6
       When: then encrypted value is decrypted
       Then: the decrypted value is equal to the string 'hello'
     */
    def 'should explicitly encrypt and decrypt with local provider'() {
        given:
        dataKeyCollection.insertOne(localDataKeyDocument)
        def keyVault = KeyVaults.create(new KeyVaultEncryptionOptions(getMongoClientSettings(), keyVaultNamespace.fullName,
                localProviderProperties))
        def value = new BsonString('hello')

        when:
        def encryptedValue = keyVault.encrypt(value, new EncryptOptions('AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic')
                .keyId(localDataKeyDocument.getBinary('_id')))

        then:
        encryptedValue == new BsonBinary((byte) 6,
                Base64.decoder.decode('AWFhYWFhYWFhYWFhYWFhYWEC7ubnsHvOUXvbE4406+XawIhcl+fsvNWO7moBSY7ABkPuCTzsitrqWWp1FbaaT05muIESiB8daggJPgwarTQ3cQ=='))

        when:
        def decryptedValue = keyVault.decrypt(encryptedValue)

        then:
        decryptedValue == value
    }

    /*
    4. Should explicity encrypt and decrypt with "aws" KMS provider
       Given: a key vault with aws KMS provider
              a key vault populated with a known aws key document
       When: the string 'hello' is encrypted with the known local key and known initialization vector using deterministic algorithm
       Then: the encrypted value is equal to a known BSON binary of subtype 6
       When: then encrypted value is decrypted
       Then: the decrypted value is equal to the string 'hello'
     */
    def 'should explicitly encrypt and decrypt with aws provider'() {
        given:
        dataKeyCollection.insertOne(awsDataKeyDocument)
        def keyVault = KeyVaults.create(new KeyVaultEncryptionOptions(getMongoClientSettings(), keyVaultNamespace.fullName,
                awsProviderProperties))
        def value = new BsonString('hello')

        when:
        def encryptedValue = keyVault.encrypt(value, new EncryptOptions('AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic')
                .keyId(awsDataKeyDocument.getBinary('_id')))

        then:
        encryptedValue == new BsonBinary((byte) 6,
                Base64.decoder.decode('AQAAAAAAAAAAAAAAAAAAAAACN0NwWlfe6YPGDEw+ObxEzbEtk45ewF3sIH2Oj7F0xd3GYoxCGCIp9gg0Q1uHTwdVWwG58SFhJyo4305IVoikEQ=='))

        when:
        def decryptedValue = keyVault.decrypt(encryptedValue)

        then:
        decryptedValue == value
    }
}
