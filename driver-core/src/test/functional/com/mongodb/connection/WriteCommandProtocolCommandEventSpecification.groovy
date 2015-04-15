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

import com.mongodb.MongoBulkWriteException
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.bulk.InsertRequest
import com.mongodb.connection.netty.NettyStreamFactory
import com.mongodb.event.CommandCompletedEvent
import com.mongodb.event.CommandFailedEvent
import com.mongodb.event.CommandStartedEvent
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import spock.lang.IgnoreIf

import static com.mongodb.ClusterFixture.getCredentialList
import static com.mongodb.ClusterFixture.getPrimary
import static com.mongodb.ClusterFixture.getSslSettings
import static com.mongodb.ClusterFixture.serverVersionAtLeast
import static com.mongodb.WriteConcern.ACKNOWLEDGED

@IgnoreIf({ !serverVersionAtLeast([2, 6, 0]) })
class WriteCommandProtocolCommandEventSpecification extends OperationFunctionalSpecification {
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
        def document = new BsonDocument('_id', new BsonInt32(1))

        def insertRequest = [new InsertRequest(document)]
        def protocol = new InsertCommandProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)

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
                                                                               .append('n', new BsonInt32(1)),
                                                                       0)])
    }

    def 'should deliver start and failed command events'() {
        given:
        def document = new BsonDocument('_id', new BsonInt32(1))

        def insertRequest = [new InsertRequest(document)]
        def protocol = new InsertCommandProtocol(getNamespace(), true, ACKNOWLEDGED, insertRequest)
        protocol.execute(connection)  // insert here, to force a duplicate key error on the second time

        def commandListener = new TestCommandListener()
        protocol.commandListener = commandListener

        when:
        protocol.execute(connection)

        then:
        def e = thrown(MongoBulkWriteException)
        commandListener.eventsWereDelivered([new CommandStartedEvent(1, connection.getDescription(), getDatabaseName(), 'insert',
                                                                     new BsonDocument('insert', new BsonString(getCollectionName()))
                                                                             .append('ordered', BsonBoolean.TRUE)
                                                                             .append('documents', new BsonArray(
                                                                             [new BsonDocument('_id', new BsonInt32(1))]))),
                                             new CommandFailedEvent(1, connection.getDescription(), 'insert', 0, e)])
    }
}
