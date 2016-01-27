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

import com.mongodb.connection.ServerDescription;
import com.mongodb.connection.ServerId;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * An event for changes to the description of a server.
 *
 * @since 3.4
 */
public class ServerDescriptionChangedEvent extends ServerEvent {

    private final ServerDescription newDescription;
    private final ServerDescription oldDescription;

    public ServerDescriptionChangedEvent(final ServerId serverId, final ServerDescription newDescription,
                                         final ServerDescription oldDescription) {
        super(serverId);
        this.newDescription = notNull("newDescription", newDescription);
        this.oldDescription = notNull("oldDescription", oldDescription);
    }

    /**
     * Gets the new server description.
     *
     * @return the new server description
     */
    public ServerDescription getNewDescription() {
        return newDescription;
    }

    /**
     * Gets the old server description.
     *
     * @return the old server description
     */
    public ServerDescription getOldDescription() {
        return oldDescription;
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

        ServerDescriptionChangedEvent that = (ServerDescriptionChangedEvent) o;

        if (!newDescription.equals(that.newDescription)) {
            return false;
        }
        if (!oldDescription.equals(that.oldDescription)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + newDescription.hashCode();
        result = 31 * result + oldDescription.hashCode();
        return result;
    }
}
