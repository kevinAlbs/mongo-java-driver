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

import com.mongodb.DuplicateKeyException
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.bulk.DeleteRequest
import com.mongodb.bulk.InsertRequest
import com.mongodb.bulk.UpdateRequest
import com.mongodb.connection.netty.NettyStreamFactory
import com.mongodb.event.CommandCompletedEvent
import com.mongodb.event.CommandFailedEvent
import com.mongodb.event.CommandStartedEvent
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import spock.lang.Ignore

import static com.mongodb.ClusterFixture.getCredentialList
import static com.mongodb.ClusterFixture.getPrimary
import static com.mongodb.ClusterFixture.getSslSettings
import static com.mongodb.WriteConcern.ACKNOWLEDGED
import static com.mongodb.WriteConcern.UNACKNOWLEDGED
import static com.mongodb.bulk.WriteRequest.Type.UPDATE

class WriteProtocolCommandEventSpecification extends OperationFunctionalSpecification {
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

    def 'should deliver start and completed command events for a single unacknowleded insert'() {
        given:
        def document = new BsonDocument('_id', new BsonInt32(1))

        def insertRequest = [new InsertRequest(document)]
        def protocol = new InsertProtocol(getNamespace(), true, UNACKNOWLEDGED, insertRequest)
        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'insert',
                                                                     new BsonDocument('insert', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('writeConcern',
                                                                                     new BsonDocument('w', new BsonInt32(0)))
                                                                             .append('documents', new BsonArray(
                                                                             [new BsonDocument('_id', new BsonInt32(1))]))),
                                             new CommandCompletedEvent(1, connection.getDescription(), 'insert', null, 0)])
    }

    def 'should deliver start and failed command events'() {
        given:
        def document = new BsonDocument('_id', new BsonInt32(1))

        def insertRequest = [new InsertRequest(document)]
        def protocol = new InsertProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)
        protocol.execute(connection)  // insert here, to force a duplicate key error on the second time

        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        def e = thrown(DuplicateKeyException)
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'insert',
                                                                     new BsonDocument('insert', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('documents', new BsonArray(
                                                                             [new BsonDocument('_id', new BsonInt32(1))]))),
                                             new CommandFailedEvent(1, connection.getDescription(), 'insert', 0, e)])
    }

    def 'should deliver start and completed command events for a single unacknowleded update'() {
        given:
        def filter = new BsonDocument('_id', new BsonInt32(1))
        def update = new BsonDocument('$set', new BsonDocument('x', new BsonInt32(1)))
        def updateRequest = [new UpdateRequest(filter, update, UPDATE).multi(true).upsert(true)]
        def protocol = new UpdateProtocol(getNamespace(), true, UNACKNOWLEDGED, updateRequest)
        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'update',
                                                                     new BsonDocument('update', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('writeConcern',
                                                                                     new BsonDocument('w', new BsonInt32(0)))
                                                                             .append('updates', new BsonArray(
                                                                             [new BsonDocument('q', filter)
                                                                                      .append('u', update)
                                                                                      .append('multi', BsonBoolean.TRUE)
                                                                                      .append('upsert', BsonBoolean.TRUE)]))),
                                             new CommandCompletedEvent(1, connection.getDescription(), 'update', null, 0)])
    }

    def 'should deliver start and completed command events for a single unacknowleded delete'() {
        given:
        def filter = new BsonDocument('_id', new BsonInt32(1))
        def deleteRequest = [new DeleteRequest(filter).multi(true)]
        def protocol = new DeleteProtocol(getNamespace(), true, UNACKNOWLEDGED, deleteRequest)
        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'delete',
                                                                     new BsonDocument('delete', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('writeConcern',
                                                                                     new BsonDocument('w', new BsonInt32(0)))
                                                                             .append('deletes', new BsonArray(
                                                                             [new BsonDocument('q', filter)
                                                                                      .append('limit', new BsonInt32(0))]))),
                                             new CommandCompletedEvent(1, connection.getDescription(), 'delete', null, 0)])
    }

    // TODO: implement this
    @Ignore
    def 'should deliver start and completed command events for a single acknowleded insert'() {
        given:
        def document = new BsonDocument('_id', new BsonInt32(1))

        def insertRequest = [new InsertRequest(document)]
        def protocol = new InsertProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)
        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'insert',
                                                                     new BsonDocument('insert', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('documents', new BsonArray(
                                                                             [new BsonDocument('_id', new BsonInt32(1))]))),
                                             new CommandCompletedEvent(1, connection.getDescription(), 'insert',
                                                                       new BsonDocument('ok', new BsonInt32(1))
                                                                       , 0)])
    }
}
