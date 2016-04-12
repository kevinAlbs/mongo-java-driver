/*
 * Copyright 2008-2016 MongoDB, Inc.
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

package com.mongodb.event;

import com.mongodb.connection.ConnectionId;

import static org.bson.assertions.Assertions.notNull;

/**
 * A connection-related event.
 *
 * @since 3.3
 */
public class ConnectionEvent extends ClusterEvent {
    private final ConnectionId connectionId;

    /**
     * Constructs a new instance of the event.
     *
     * @param connectionId the connection id
     */
    public ConnectionEvent(final ConnectionId connectionId) {
        super(connectionId.getServerId().getClusterId());
        this.connectionId = notNull("connectionId", connectionId);
    }

    /**
     * Gets the identifier for this connection.
     *
     * @return the connection id
     */
    public ConnectionId getConnectionId() {
        return connectionId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ConnectionEvent that = (ConnectionEvent) o;

        if (!connectionId.equals(that.connectionId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + connectionId.hashCode();
        return result;
    }
}
