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

import com.mongodb.DBRefCodecProvider
import com.mongodb.DocumentToDBRefTransformer
import com.mongodb.binding.EmbeddedServerBinding
import com.mongodb.client.gridfs.codecs.GridFSFileCodecProvider
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider
import com.mongodb.connection.EmbeddedServer
import com.mongodb.connection.EmbeddedServerFactory
import com.mongodb.operation.ReadOperation
import com.mongodb.operation.WriteOperation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.pojo.PojoCodecProvider

internal class MongoClientImpl(argv: List<String>) : MongoClient {
    private val embeddedServer : EmbeddedServer = EmbeddedServerFactory.create(argv, listOf())

    private val codecRegistry = fromProviders(listOf(
            ValueCodecProvider(),
            BsonValueCodecProvider(),
            DBRefCodecProvider(),
            DocumentCodecProvider(DocumentToDBRefTransformer()),
            IterableCodecProvider(DocumentToDBRefTransformer()),
            MapCodecProvider(DocumentToDBRefTransformer()),
            GeoJsonCodecProvider(),
            GridFSFileCodecProvider(),
            PojoCodecProvider.builder().automatic(true).build()))   // TODO: should we...?

    override fun pump() {
        embeddedServer.pump()
    }

    override fun getDatabase(name: String): MongoDatabase {
        return MongoDatabaseImpl(name, codecRegistry, OperationExecutorImpl())
    }

    override fun close() {
        embeddedServer.close()
    }

    inner class OperationExecutorImpl : OperationExecutor {
        override fun <T> execute(operation: ReadOperation<T>): T {
            val binding = EmbeddedServerBinding(embeddedServer)

            try {
                return operation.execute(binding)
            } finally {
                binding.release()
            }
        }

        override fun <T> execute(operation: WriteOperation<T>): T {
            val binding = EmbeddedServerBinding(embeddedServer)

            try {
                return operation.execute(binding)
            } finally {
                binding.release()
            }
        }
    }
}