/*
 * Copyright 2008-present MongoDB, Inc.
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

package com.mongodb.client;

import com.mongodb.TransactionOptions;

/**
 *
 * @since 3.8
 */
public interface ClientSession extends com.mongodb.session.ClientSession {
    /**
     * Returns true if there is an active transaction on this session, and false otherwise
     *
     * @return true if there is an active transaction on this session
     * @mongodb.server.release 4.0
     */
    boolean hasActiveTransaction();

    /**
     * Gets the transaction options.  Only call this method of the session has an active transaction
     *
     * @return the transaction options
     */
    TransactionOptions getTransactionOptions();

    /**
     *
     * @mongodb.server.release 4.0
     */
    void startTransaction();

    /**
     *
     * @param transactionOptions the options to apply to the transaction
     *
     * @mongodb.server.release 4.0
     */
    void startTransaction(TransactionOptions transactionOptions);

    /**
     *
     * @mongodb.server.release 4.0
     */
    void commitTransaction();

    /**
     *
     * @mongodb.server.release 4.0
     */
    void abortTransaction();
}
