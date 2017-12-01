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

package com.mongodb.embedded.client

import com.mongodb.MongoNamespace
import com.mongodb.WriteConcern
import com.mongodb.operation.CommandReadOperation
import com.mongodb.operation.DropDatabaseOperation
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson

internal class MongoDatabaseImpl(private val name: String,
                                 private val codecRegistry: CodecRegistry,
                                 private val operationExecutor: OperationExecutor) : MongoDatabase {
    override fun <TDocument> getCollection(collectionName: String, documentClass: Class<TDocument>): MongoCollection<TDocument> {
        return MongoCollectionImpl(MongoNamespace(name, collectionName), documentClass, codecRegistry, operationExecutor)
    }

    override fun runCommand(command: Bson): Document {
        return operationExecutor.execute(CommandReadOperation<Document>(name, toBsonDocument(command),
                codecRegistry.get(Document::class.java)))
    }

    override fun drop() {
       operationExecutor.execute(DropDatabaseOperation(name, WriteConcern.ACKNOWLEDGED))
    }

    private fun toBsonDocument(document: Bson): BsonDocument {
        return document.toBsonDocument(BsonDocument::class.java, codecRegistry)
    }
}