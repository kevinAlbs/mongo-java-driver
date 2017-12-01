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

import com.sun.jna.Native;

import java.util.List;

/**
 *
 * @since 3.8
 * @mongodb.server.release 3.8
 */
public final class EmbeddedServerFactory {

    static {
        // TODO: this should be removed for production
        Native.setProtected(true);
    }

    /**
     * Create an instance of EmbeddedServer.
     *
     * @param argv the arguments to mongod
     * @param envp the environment for mongod
     * @return the embedded server
     */
    public static EmbeddedServer create(final List<String> argv, final List<String> envp) {
        MongoDBEmbeddedServer mongoDBEmbeddedServer = new MongoDBEmbeddedServerImpl(MongoDBCAPI.INSTANCE);
        mongoDBEmbeddedServer.init(argv, envp);
        return new EmbeddedServer(mongoDBEmbeddedServer);
    }

    private EmbeddedServerFactory() {
    }
}
