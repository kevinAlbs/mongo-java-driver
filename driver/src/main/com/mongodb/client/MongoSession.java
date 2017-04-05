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

package com.mongodb.client;

import org.bson.Document;

import java.io.Closeable;

/**
 * A session for MongoDB.
 */
public interface MongoSession extends Closeable {

    /**
     * @param databaseName the name of the database to retrieve
     * @return a {@code MongoDatabase} representing the specified database
     * @throws IllegalArgumentException if databaseName is invalid
     * @see com.mongodb.MongoNamespace#checkDatabaseNameValidity(String)
     */
    MongoDatabase getDatabase(final String databaseName);

    /**
     * Get a list of the database names
     *
     * @mongodb.driver.manual reference/command/listDatabases List Databases
     * @return an iterable containing all the names of all the databases
     * @since 3.0
     */
    MongoIterable<String> listDatabaseNames();

    /**
     * Gets the list of databases
     *
     * @return the list of databases
     * @since 3.0
     */
    ListDatabasesIterable<Document> listDatabases();

    /**
     * Gets the list of databases
     *
     * @param clazz the class to cast the database documents to
     * @param <T>   the type of the class to use instead of {@code Document}.
     * @return the list of databases
     * @since 3.0
     */
    <T> ListDatabasesIterable<T> listDatabases(final Class<T> clazz);

    /**
     * Close the session, releasing any resources underlying it
     */
    void close();
}
