/*
 * Copyright 2018 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.embedded.client;

import com.mongodb.connection.EmbeddedServer;
import com.mongodb.connection.EmbeddedServerFactory;
import com.sun.jna.Native;

import java.util.List;

final class EmbeddedServerFactoryImpl implements EmbeddedServerFactory {

    static {
        // TODO: this should be removed for production
        Native.setProtected(true);
    }

    @Override
    public EmbeddedServer create(final List<String> argv, final List<String> envp) {
        EmbeddedServerImpl embeddedServer = new EmbeddedServerImpl(MongoDBCAPI.INSTANCE);
        embeddedServer.init(argv, envp);
        return embeddedServer;
    }

}
