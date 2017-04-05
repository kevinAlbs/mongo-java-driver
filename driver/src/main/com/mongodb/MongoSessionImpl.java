/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.mongodb;

import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.MongoSession;
import org.bson.Document;

class MongoSessionImpl implements MongoSession {
    private final MongoSession wrappedSession;

    public MongoSessionImpl(final MongoClient wrappedSession) {
        this.wrappedSession = wrappedSession;
    }

    @Override
    public MongoIterable<String> listDatabaseNames() {
        return wrappedSession.listDatabaseNames();
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases() {
        return wrappedSession.listDatabases();
    }

    @Override
    public <T> ListDatabasesIterable<T> listDatabases(final Class<T> clazz) {
        return wrappedSession.listDatabases(clazz);
    }

    @Override
    public MongoDatabase getDatabase(final String databaseName) {
        return wrappedSession.getDatabase(databaseName);
    }

    @Override
    public void close() {
        // do nothing
    }
}
