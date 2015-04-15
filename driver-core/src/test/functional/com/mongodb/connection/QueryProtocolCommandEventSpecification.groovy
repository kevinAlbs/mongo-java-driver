/*
 * Copyright 2015 MongoDB, Inc.
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
 *
 *
 */

package com.mongodb.connection

import com.mongodb.MongoQueryException
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.connection.netty.NettyStreamFactory
import com.mongodb.event.CommandCompletedEvent
import com.mongodb.event.CommandFailedEvent
import com.mongodb.event.CommandStartedEvent
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonString
import org.bson.codecs.BsonDocumentCodec

import static com.mongodb.ClusterFixture.getCredentialList
import static com.mongodb.ClusterFixture.getPrimary
import static com.mongodb.ClusterFixture.getSslSettings
import static org.bson.BsonDocument.parse

class QueryProtocolCommandEventSpecification extends OperationFunctionalSpecification {
    static InternalStreamConnection connection;

    def setupSpec() {
        connection = new InternalStreamConnectionFactory(new NettyStreamFactory(SocketSettings.builder().build(), getSslSettings()),
                                                         getCredentialList(), new NoOpConnectionListener())
                .create(new ServerId(new ClusterId(), getPrimary()))
        connection.open();
    }

    def cleanupSpec() {
        connection?.close()
    }

    def 'should deliver start and completed command events'() {
        given:
        def documents = [new BsonDocument('_id', new BsonInt32(1)),
                         new BsonDocument('_id', new BsonInt32(2)),
                         new BsonDocument('_id', new BsonInt32(3)),
                         new BsonDocument('_id', new BsonInt32(4)),
                         new BsonDocument('_id', new BsonInt32(5))]
        collectionHelper.insertDocuments(documents)

        def filter = parse('{_id : {$gt : 0}}')
        def projection = parse('{_id : 1}')
        def skip = 1
        def protocol = new QueryProtocol(getNamespace(), skip, 2, filter, projection, new BsonDocumentCodec())

        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        def result = protocol.execute(connection)

        then:
        def response = new BsonDocument('cursor',
                                        new BsonDocument('id', new BsonInt64(result.cursor.id))
                                                .append('ns', new BsonString(getNamespace().getFullName()))
                                                .append('firstBatch', new BsonArray(documents.subList(1, 3))))
                .append('ok', new BsonDouble(1.0))
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'find',
                                                                     new BsonDocument('find', new BsonString(getCollectionName()))
                                                                             .append('filter', filter)
                                                                             .append('projection', projection)
                                                                             .append('skip', new BsonInt32(skip))),
                                             new CommandCompletedEvent(1, connection.getDescription(), 'find', response, 0)])
    }

    def 'should deliver start and failed command events'() {
        given:
        def filter = parse('{_id : {$fakeOp : 1}}')
        def projection = parse('{_id : 1}')
        def skip = 5
        def protocol = new QueryProtocol(getNamespace(), skip, 5, filter, projection, new BsonDocumentCodec())

        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        def e = thrown(MongoQueryException)
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'find',
                                                                     new BsonDocument('find', new BsonString(getCollectionName()))
                                                                             .append('filter', filter)
                                                                             .append('projection', projection)
                                                                             .append('skip', new BsonInt32(skip))),
                                             new CommandFailedEvent(1, connection.getDescription(), 'find', 0, e)])
    }
}
