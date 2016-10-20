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

package com.mongodb;

import com.mongodb.annotations.Immutable;

import static com.mongodb.assertions.Assertions.isTrueArgument;
import static com.mongodb.assertions.Assertions.notNull;
import static java.util.Arrays.asList;

/**
 * A MongoDB namespace, which includes a database name and collection name.
 *
 * @since 3.0
 */
@Immutable
public final class MongoNamespace {
    public static final String COMMAND_COLLECTION_NAME = "$cmd";

    private final String databaseName;
    private final String collectionName;
    private final String fullName;  // cache to avoid repeated string building

    /**
     * Check the validity of the given database name. A valid database name is non-null, non-empty, and does not contain any of the
     * following strings: {@code " ", ".", "/", "\\", "\u0000", "\""}
     *
     * @param databaseName the database name
     * @throws IllegalArgumentException if the database name is invalid
     * @since 3.4
     */
    public static void checkDatabaseNameValidity(final String databaseName) {
        notNull("databaseName", databaseName);
        isTrueArgument("databaseName is not empty", !databaseName.isEmpty());
        for (String cur : asList(" ", ".", "/", "\\", "\u0000", "\"")) {
            isTrueArgument("databaseName does not contain '" + cur + "'", !databaseName.contains(cur));
        }
    }

    /**
     * Check the validity of the given collection name.   A valid collection name is non-null, non-empty, does not start or end with a
     * {@code "."} character, and does not contain any of the following strings: {@code "..", "\u0000"}
     *
     * @param collectionName the collection name
     * @throws IllegalArgumentException if the collection name is invalid
     * @since 3.4
     */
    public static void checkCollectionNameValidity(final String collectionName) {
        notNull("collectionName", collectionName);
        isTrueArgument("collectionName is not empty", !collectionName.isEmpty());
        isTrueArgument("collectionName doesn't start with a '.", !collectionName.startsWith("."));
        isTrueArgument("collectionName doesn't end with a '.", !collectionName.endsWith("."));
        for (String cur : asList("..", "\u0000")) {
            isTrueArgument("databaseName does not contain '" + cur + "'", !collectionName.contains(cur));
        }
    }

    /**
     * Construct an instance for the given full name.  The database name is the string preceding the first {@code "."} character.
     *
     * @param fullName the non-null full namespace
     * @see #checkDatabaseNameValidity(String)
     * @see #checkCollectionNameValidity(String)
     */
    public MongoNamespace(final String fullName) {
        notNull("fullName", fullName);
        this.fullName = fullName;
        this.databaseName = getDatatabaseNameFromFullName(fullName);
        this.collectionName = getCollectionNameFullName(fullName);
        checkDatabaseNameValidity(databaseName);
        checkCollectionNameValidity(collectionName);
    }

    /**
     * Construct an instance from the given database name and collection name.
     *
     * @param databaseName   the valid database name
     * @param collectionName the valid collection name
     * @see #checkDatabaseNameValidity(String)
     * @see #checkCollectionNameValidity(String)
     */
    public MongoNamespace(final String databaseName, final String collectionName) {
        checkDatabaseNameValidity(databaseName);
        checkCollectionNameValidity(collectionName);
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.fullName = databaseName + '.' + collectionName;
    }

    /**
     * Gets the database name.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Gets the collection name.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the full name, which is the database name and the collection name, separated by a period.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MongoNamespace that = (MongoNamespace) o;

        if (!collectionName.equals(that.collectionName)) {
            return false;
        }
        if (!databaseName.equals(that.databaseName)) {
            return false;
        }

        return true;
    }

    /**
     * Returns the standard MongoDB representation of a namespace, which is {@code &lt;database&gt;.&lt;collection&gt;}.
     *
     * @return string representation of the namespace.
     */
    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public int hashCode() {
        int result = databaseName.hashCode();
        result = 31 * result + (collectionName.hashCode());
        return result;
    }

    private static String getCollectionNameFullName(final String namespace) {
        if (namespace == null) {
            return null;
        }
        int firstDot = namespace.indexOf('.');
        if (firstDot == -1) {
            return namespace;
        }
        return namespace.substring(firstDot + 1);
    }

    private static String getDatatabaseNameFromFullName(final String namespace) {
        if (namespace == null) {
            return null;
        }
        int firstDot = namespace.indexOf('.');
        if (firstDot == -1) {
            return "";
        }
        return namespace.substring(0, firstDot);
    }
}
