/*
 * Copyright 2016 MongoDB, Inc.
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
import org.bson.BsonDocument;

import java.util.concurrent.TimeUnit;

public class ServerHeartbeatSucceededEvent extends ServerEvent{
    private final ConnectionId connectionId;
    private final BsonDocument reply;
    private final long elapsedTimeNanos;

    public ServerHeartbeatSucceededEvent(final ConnectionId connectionId, final BsonDocument reply, final long elapsedTimeNanos) {
        super(connectionId.getServerId());

        this.connectionId = connectionId;
        this.reply = reply;
        this.elapsedTimeNanos = elapsedTimeNanos;
    }

    public ConnectionId getConnectionId() {
        return connectionId;
    }

    public BsonDocument getReply() {
        return reply;
    }

    public long getElapsedTime(final TimeUnit timeUnit) {
        return timeUnit.convert(elapsedTimeNanos, TimeUnit.NANOSECONDS);
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

        ServerHeartbeatSucceededEvent that = (ServerHeartbeatSucceededEvent) o;

        if (elapsedTimeNanos != that.elapsedTimeNanos) {
            return false;
        }
        if (!connectionId.equals(that.connectionId)) {
            return false;
        }
        if (!reply.equals(that.reply)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + connectionId.hashCode();
        result = 31 * result + reply.hashCode();
        result = 31 * result + (int) (elapsedTimeNanos ^ (elapsedTimeNanos >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ServerHeartbeatSucceededEvent{"
                + "connectionId=" + connectionId
                + ", reply=" + reply
                + ", elapsedTimeNanos=" + elapsedTimeNanos
                + "} " + super.toString();
    }
}
