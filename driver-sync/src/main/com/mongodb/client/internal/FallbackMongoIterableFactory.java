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

package com.mongodb.client.internal;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.model.changestream.ChangeStreamLevel;
import com.mongodb.client.model.AggregationLevel;
import com.mongodb.lang.Nullable;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;

class FallbackMongoIterableFactory implements MongoIterableFactory {
    @Override
    public <TDocument, TResult>
    FindIterable<TResult> findOf(final @Nullable ClientSession clientSession, final MongoNamespace namespace,
                                 final Class<TDocument> documentClass, final Class<TResult> resultClass, final CodecRegistry codecRegistry,
                                 final ReadPreference readPreference, final ReadConcern readConcern, final OperationExecutor executor,
                                 final Bson filter) {
        return new FindIterableImpl<TDocument, TResult>(clientSession, namespace, documentClass, resultClass, codecRegistry,
                readPreference, readConcern, executor, filter);
    }

    @Override
    public <TDocument, TResult> AggregateIterable<TResult> aggregateOf(final @Nullable ClientSession clientSession,
                                                                       final MongoNamespace namespace, final Class<TDocument> documentClass,
                                                                       final Class<TResult> resultClass, final CodecRegistry codecRegistry,
                                                                       final ReadPreference readPreference, final ReadConcern readConcern,
                                                                       final WriteConcern writeConcern, final OperationExecutor executor,
                                                                       final List<? extends Bson> pipeline,
                                                                       final AggregationLevel aggregationLevel) {
        return new AggregateIterableImpl<TDocument, TResult>(clientSession, namespace, documentClass, resultClass, codecRegistry,
                readPreference, readConcern, writeConcern, executor, pipeline, aggregationLevel);
    }

    @Override
    public <TDocument, TResult> AggregateIterable<TResult> aggregateOf(final @Nullable ClientSession clientSession,
                                                                       final String databaseName, final Class<TDocument> documentClass,
                                                                       final Class<TResult> resultClass, final CodecRegistry codecRegistry,
                                                                       final ReadPreference readPreference, final ReadConcern readConcern,
                                                                       final WriteConcern writeConcern, final OperationExecutor executor,
                                                                       final List<? extends Bson> pipeline,
                                                                       final AggregationLevel aggregationLevel) {
        return new AggregateIterableImpl<TDocument, TResult>(clientSession, databaseName, documentClass, resultClass, codecRegistry,
                readPreference, readConcern, writeConcern, executor, pipeline, aggregationLevel);
    }

    @Override
    public <TResult>
    ChangeStreamIterable<TResult> changeStreamOf(final @Nullable ClientSession clientSession, final String databaseName,
                                                 final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                                 final ReadConcern readConcern, final OperationExecutor executor,
                                                 final List<? extends Bson> pipeline, final Class<TResult> resultClass,
                                                 final ChangeStreamLevel changeStreamLevel) {
        return new ChangeStreamIterableImpl<TResult>(clientSession, databaseName, codecRegistry, readPreference, readConcern, executor,
                pipeline, resultClass, changeStreamLevel);
    }

    @Override
    public <TResult>
    ChangeStreamIterable<TResult> changeStreamOf(final @Nullable ClientSession clientSession, final MongoNamespace namespace,
                                                 final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                                 final ReadConcern readConcern, final OperationExecutor executor,
                                                 final List<? extends Bson> pipeline, final Class<TResult> resultClass,
                                                 final ChangeStreamLevel changeStreamLevel) {
        return new ChangeStreamIterableImpl<TResult>(clientSession, namespace, codecRegistry, readPreference, readConcern, executor,
                pipeline, resultClass, changeStreamLevel);
    }


    @Override
    public <TDocument, TResult>
    DistinctIterable<TResult> distinctOf(final @Nullable ClientSession clientSession, final MongoNamespace namespace,
                                         final Class<TDocument> documentClass, final Class<TResult> resultClass,
                                         final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                         final ReadConcern readConcern, final OperationExecutor executor, final String fieldName,
                                         final Bson filter) {
        return new DistinctIterableImpl<TDocument, TResult>(clientSession, namespace, documentClass, resultClass, codecRegistry,
                readPreference, readConcern, executor, fieldName, filter);
    }

    @Override
    public <TResult>
    ListDatabasesIterable<TResult> listDatabasesOf(final @Nullable ClientSession clientSession, final Class<TResult> resultClass,
                                                   final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                                   final OperationExecutor executor) {
        return new ListDatabasesIterableImpl<TResult>(clientSession, resultClass, codecRegistry, readPreference, executor);
    }

    @Override
    public <TResult>
    ListCollectionsIterable<TResult> listCollectionsOf(final @Nullable ClientSession clientSession, final String databaseName,
                                                       final boolean collectionNamesOnly, final Class<TResult> resultClass,
                                                       final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                                       final OperationExecutor executor) {
        return new ListCollectionsIterableImpl<TResult>(clientSession, databaseName, collectionNamesOnly, resultClass, codecRegistry,
                readPreference, executor);
    }

    @Override
    public <TResult>
    ListIndexesIterable<TResult> listIndexesOf(final @Nullable ClientSession clientSession, final MongoNamespace namespace,
                                               final Class<TResult> resultClass, final CodecRegistry codecRegistry,
                                               final ReadPreference readPreference, final OperationExecutor executor) {
        return new ListIndexesIterableImpl<TResult>(clientSession, namespace, resultClass, codecRegistry, readPreference, executor);
    }

    @Override
    public <TDocument, TResult>
    MapReduceIterable<TResult> mapReduceOf(final @Nullable ClientSession clientSession, final MongoNamespace namespace,
                                           final Class<TDocument> documentClass, final Class<TResult> resultClass,
                                           final CodecRegistry codecRegistry, final ReadPreference readPreference,
                                           final ReadConcern readConcern, final WriteConcern writeConcern, final OperationExecutor executor,
                                           final String mapFunction, final String reduceFunction) {
        return new MapReduceIterableImpl<TDocument, TResult>(clientSession, namespace, documentClass, resultClass, codecRegistry,
                readPreference, readConcern, writeConcern, executor, mapFunction, reduceFunction);
    }
}
