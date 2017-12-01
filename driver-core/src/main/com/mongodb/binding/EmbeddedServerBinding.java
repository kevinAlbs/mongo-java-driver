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

package com.mongodb.binding;

import com.mongodb.ReadPreference;
import com.mongodb.connection.Connection;
import com.mongodb.connection.EmbeddedServer;
import com.mongodb.connection.ServerDescription;
import com.mongodb.internal.connection.NoOpSessionContext;
import com.mongodb.session.SessionContext;

/**
 * A binding to an embedded server.
 *
 * @since 3.8
 */
public class EmbeddedServerBinding extends AbstractReferenceCounted implements ReadWriteBinding {

    private final EmbeddedServer embeddedServer;

    /**
     * Construct an instance.
     *
     * @param embeddedServer the server to bind to
     */
    public EmbeddedServerBinding(final EmbeddedServer embeddedServer) {
        this.embeddedServer = embeddedServer;
    }

    @Override
    public ReadPreference getReadPreference() {
        return ReadPreference.primary();
    }

    @Override
    public ConnectionSource getReadConnectionSource() {
        return new EmbeddedServerBindingConnectionSource();
    }

    @Override
    public ConnectionSource getWriteConnectionSource() {
        return new EmbeddedServerBindingConnectionSource();
    }

    @Override
    public SessionContext getSessionContext() {
        return NoOpSessionContext.INSTANCE;
    }

    @Override
    public EmbeddedServerBinding retain() {
        super.retain();
        return this;
    }

    private final class EmbeddedServerBindingConnectionSource extends AbstractReferenceCounted implements ConnectionSource {

        private EmbeddedServerBindingConnectionSource() {
            EmbeddedServerBinding.this.retain();
        }

        @Override
        public ServerDescription getServerDescription() {
            return embeddedServer.getDescription();
        }

        @Override
        public SessionContext getSessionContext() {
            return NoOpSessionContext.INSTANCE;
        }

        @Override
        public Connection getConnection() {
            return embeddedServer.getConnection();
        }

        @Override
        public ConnectionSource retain() {
            super.retain();
            EmbeddedServerBinding.this.retain();
            return this;
        }


        @Override
        public void release() {
            super.release();
            EmbeddedServerBinding.this.release();
        }
    }
}
