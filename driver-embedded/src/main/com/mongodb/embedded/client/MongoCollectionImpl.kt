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
import com.mongodb.bulk.InsertRequest
import com.mongodb.operation.*
import org.bson.BsonDocument
import org.bson.BsonDocumentWrapper
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import java.util.*

internal class MongoCollectionImpl<TDocument>(private val namespace: MongoNamespace,
                                              private val documentClass: Class<TDocument>,
                                              private val codecRegistry: CodecRegistry,
                                              private val operationExecutor: OperationExecutor) :
        MongoCollection<TDocument> {
    override fun insertOne(document: TDocument) {
        operationExecutor.execute(MixedBulkWriteOperation(namespace, listOf(InsertRequest(documentToBsonDocument(document))), true,
                WriteConcern.ACKNOWLEDGED, false))
    }

    override fun count(): Long = operationExecutor.execute(CountOperation(namespace))

    override fun aggregate(pipeline: List<Bson>): AggregateIterable<TDocument> {
        return AggregateIterableImpl(namespace, documentClass, codecRegistry, operationExecutor, pipeline)
    }

    private fun documentToBsonDocument(document: TDocument): BsonDocument = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
}

class AggregateIterableImpl<TResult> internal constructor(private val namespace: MongoNamespace,
                                                          private val documentClass: Class<TResult>,
                                                          private val codecRegistry: CodecRegistry,
                                                          operationExecutor: OperationExecutor,
                                                          private val pipeline: List<Bson>) :
        MongoIterableImpl<TResult>(operationExecutor), AggregateIterable<TResult> {

    override fun asReadOperation(): ReadOperation<BatchCursor<TResult>> {
        return AggregateOperation(namespace, createBsonDocumentList(pipeline), codecRegistry.get(documentClass))
    }

    // TODO: annoying to have to convert to MutableList. Is there a way to avoid it?
    private fun createBsonDocumentList(pipeline: List<Bson>) =
            MutableList(pipeline.size, {
                pipeline[it].toBsonDocument(documentClass, codecRegistry)
            })
}

abstract class MongoIterableImpl<TResult> internal constructor(private val operationExecutor: OperationExecutor) : Iterable<TResult> {
    override fun iterator(): Iterator<TResult> {
        val batchCursor = operationExecutor.execute(asReadOperation())
        return IteratorImpl(batchCursor)
    }

    abstract fun asReadOperation(): ReadOperation<BatchCursor<TResult>>

}

class IteratorImpl<TResult>(private val batchCursor: BatchCursor<TResult>) : Iterator<TResult> {
    private var curBatch: List<TResult>? = null
    private var curPos: Int = 0

    override fun hasNext(): Boolean =
            curBatch != null || batchCursor.hasNext()

    override fun next(): TResult {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        if (curBatch == null) {
            curBatch = batchCursor.next()
        }

        return getNextInBatch()
    }

    private fun getNextInBatch(): TResult {
        val nextInBatch = curBatch!![curPos]
        if (curPos < curBatch!!.size - 1) {
            curPos++
        } else {
            curBatch = null
            curPos = 0
        }
        return nextInBatch
    }

}
