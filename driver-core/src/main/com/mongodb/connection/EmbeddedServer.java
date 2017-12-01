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

package com.mongodb.connection;

import com.mongodb.MongoCompressor;
import com.mongodb.ServerAddress;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.session.SessionContext;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

import java.io.Closeable;
import java.util.Collections;

import static com.mongodb.connection.ClientMetadataHelper.createClientMetadataDocument;
import static com.mongodb.connection.CommandHelper.executeCommand;

/**
 * An embedded server.
 *
 * @since 3.8
 */
public class EmbeddedServer implements Closeable {

    private final ClusterId clusterId = new ClusterId();
    private final ClusterClock clusterClock = new ClusterClock();
    private final ServerAddress serverAddress = new ServerAddress();
    private final MongoDBEmbeddedServer embeddedServer;
    private final ServerDescription serverDescription;

    /**
     * Construct an instance.
     *
     * @param embeddedServer the wrapped server
     */
    public EmbeddedServer(final MongoDBEmbeddedServer embeddedServer) {
        this.embeddedServer = embeddedServer;
        this.serverDescription = createServerDescription();
    }

    private ServerDescription createServerDescription() {
        InternalConnection connection = getInternalConnection();
        try {
            connection.open();
            long start = System.nanoTime();
            BsonDocument isMasterResult = executeCommand("admin", new BsonDocument("ismaster", new BsonInt32(1)),
                    clusterClock, connection);

            return DescriptionHelper.createServerDescription(serverAddress, isMasterResult, connection.getDescription().getServerVersion(),
                    System.nanoTime() - start);
        } finally {
            connection.close();
        }

    }

    /**
     * Gets the description of this server
     *
     * @return the server description
     */
    public ServerDescription getDescription() {
        return serverDescription;
    }

    /**
     * Gets a connection to this server.
     *
     * @return the connection
     */
    public Connection getConnection() {
        InternalConnection internalConnection = getInternalConnection();
        internalConnection.open();
        return new DefaultServerConnection(internalConnection, new DefaultServerProtocolExecutor(), ClusterConnectionMode.SINGLE);
    }

    /**
     * Pump the message queue.
     */
    public void pump() {
        embeddedServer.pump();
    }

    @Override
    public void close() {
        embeddedServer.close();
    }

    private InternalConnection getInternalConnection() {
        return new InternalStreamConnection(new ServerId(clusterId, new ServerAddress()),
                new StreamFactory() {
                    @Override
                    public Stream create(final ServerAddress serverAddress) {
                        return new EmbeddedStream(embeddedServer.createConnection());
                    }
                }, Collections.<MongoCompressor>emptyList(), null,
                new InternalStreamConnectionInitializer(Collections.<Authenticator>emptyList(), createClientMetadataDocument(null),
                        Collections.<MongoCompressor>emptyList()));
    }

    private static class DefaultServerProtocolExecutor implements ProtocolExecutor {
        @Override
        public <T> T execute(final LegacyProtocol<T> protocol, final InternalConnection connection) {
            return protocol.execute(connection);
        }

        @Override
        public <T> void executeAsync(final LegacyProtocol<T> protocol, final InternalConnection connection,
                                     final SingleResultCallback<T> callback) {
            protocol.executeAsync(connection, callback);
        }

        @Override
        public <T> T execute(final CommandProtocol<T> protocol, final InternalConnection connection,
                             final SessionContext sessionContext) {
            protocol.sessionContext(sessionContext);
            return protocol.execute(connection);
        }

        @Override
        public <T> void executeAsync(final CommandProtocol<T> protocol, final InternalConnection connection,
                                     final SessionContext sessionContext, final SingleResultCallback<T> callback) {
            protocol.sessionContext(sessionContext);
            protocol.executeAsync(connection, callback);
        }
    }
}
