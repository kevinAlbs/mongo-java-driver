/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
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

import com.mongodb.annotations.Beta;
import com.mongodb.connection.ServerId;

/**
 * An event related to the connection pool's wait queue..
 */
@Beta
public class ConnectionPoolWaitQueueEvent extends ConnectionPoolEvent {
    private final long threadId;

    /**
     * Constructs a new instance of the event.
     *
     * @param serverId the server id
     * @param threadId the identifier of the waiting thread
     */
    public ConnectionPoolWaitQueueEvent(final ServerId serverId, final long threadId) {
        super(serverId);
        this.threadId = threadId;
    }

    /**
     * Gets the identifier of the waiting thread.
     *
     * @return the thread id
     */
    public long getThreadId() {
        return threadId;
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

        ConnectionPoolWaitQueueEvent that = (ConnectionPoolWaitQueueEvent) o;

        if (threadId != that.threadId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        return result;
    }
}

