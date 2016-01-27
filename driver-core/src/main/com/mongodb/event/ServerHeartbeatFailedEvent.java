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
 *
 */

package com.mongodb.event;

import com.mongodb.connection.ConnectionId;

import java.util.concurrent.TimeUnit;

public class ServerHeartbeatFailedEvent extends ServerEvent {
    private final ConnectionId connectionId;
    private final long elapsedTimeNanos;
    private final Throwable throwable;

    public ServerHeartbeatFailedEvent(final ConnectionId connectionId, final long elapsedTimeNanos, final Throwable throwable) {
        super(connectionId.getServerId());
        this.connectionId = connectionId;
        this.elapsedTimeNanos = elapsedTimeNanos;
        this.throwable = throwable;
    }

    public ConnectionId getConnectionId() {
        return connectionId;
    }

    public long getElapsedTimeNanos(final TimeUnit timeUnit) {
        return timeUnit.convert(elapsedTimeNanos, TimeUnit.NANOSECONDS);
    }

    public Throwable getThrowable() {
        return throwable;
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

        ServerHeartbeatFailedEvent that = (ServerHeartbeatFailedEvent) o;

        if (elapsedTimeNanos != that.elapsedTimeNanos) {
            return false;
        }
        if (!connectionId.equals(that.connectionId)) {
            return false;
        }
        if (!throwable.equals(that.throwable)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + connectionId.hashCode();
        result = 31 * result + (int) (elapsedTimeNanos ^ (elapsedTimeNanos >>> 32));
        result = 31 * result + throwable.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServerHeartbeatFailedEvent{"
                + "connectionId=" + connectionId
                + ", elapsedTimeNanos=" + elapsedTimeNanos
                + ", throwable=" + throwable
                + "} " + super.toString();
    }
}
