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

import com.mongodb.MongoException;
import com.mongodb.connection.EmbeddedConnection;
import com.mongodb.connection.EmbeddedServer;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

final class EmbeddedServerImpl implements EmbeddedServer {

    private final MongoDBCAPI mongoDBCAPI;
    private volatile Pointer databasePointer;
    private volatile boolean closed;


    EmbeddedServerImpl(final MongoDBCAPI mongoDBCAPI) {
        this.mongoDBCAPI = mongoDBCAPI;
    }

    void init(final List<String> argc, final List<String> envp) {
        List<String> allArgc = new ArrayList<String>(argc.size() + 1);
        allArgc.add("mongod");
        allArgc.addAll(argc);
        databasePointer = mongoDBCAPI.libmongodbcapi_db_new(allArgc.size(), allArgc.toArray(new String[argc.size()]),
                envp.toArray(new String[envp.size()]));
    }

    @Override
    public EmbeddedConnection createConnection() {
        return new EmbeddedConnectionImpl(mongoDBCAPI, databasePointer);
    }

    @Override
    public void pump() {
        int errorCode = mongoDBCAPI.libmongodbcapi_db_pump(databasePointer);
        if (errorCode != 0) {
            throw new MongoException(errorCode, "Error from embedded server: + " + errorCode);
        }
    }

    @Override
    public void close() {
        mongoDBCAPI.libmongodbcapi_db_destroy(databasePointer);
        databasePointer = null;
    }
}
