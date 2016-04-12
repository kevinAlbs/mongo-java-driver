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
 * @since 3.3
 */
public final class ServerDescriptionChangedEvent {

    private final ServerId serverId;
    private final ServerDescription newDescription;
    private final ServerDescription oldDescription;

    /**
     * Construct an instance.
     *
     * @param serverId the non-null serverId
     * @param newDescription the non-null new description
     * @param oldDescription the non-null old description
     */
    public ServerDescriptionChangedEvent(final ServerId serverId, final ServerDescription newDescription,
                                         final ServerDescription oldDescription) {
        this.serverId = notNull("serverId", serverId);
        this.newDescription = notNull("newDescription", newDescription);
        this.oldDescription = notNull("oldDescription", oldDescription);
    }


    /**
     * Gets the serverId.
     *
     * @return the serverId
     */
    public ServerId getServerId() {
        return serverId;
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
    public String toString() {
        return "ServerDescriptionChangedEvent{"
                       + "serverId=" + serverId
                       + ", newDescription=" + newDescription
                       + ", oldDescription=" + oldDescription
                       + '}';
    }
}
