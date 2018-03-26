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

package com.mongodb.client.internal;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.internal.session.BaseClientSessionImpl;
import com.mongodb.internal.session.ServerSessionPool;
import com.mongodb.operation.AbortTransactionOperation;
import com.mongodb.operation.CommitTransactionOperation;

final class ClientSessionImpl extends BaseClientSessionImpl implements ClientSession {

    private final MongoClientDelegate delegate;
    private boolean inTransaction;

    ClientSessionImpl(final ServerSessionPool serverSessionPool, final Object originator, final ClientSessionOptions options,
                      final MongoClientDelegate delegate) {
        super(serverSessionPool, originator, options);
        this.delegate = delegate;
        if (options.getAutoStartTransaction()) {
            startTransaction(options.getDefaultTransactionOptions());
        }
    }

    @Override
    public boolean hasActiveTransaction() {
        return inTransaction;
    }

    @Override
    public void startTransaction() {
        startTransaction(TransactionOptions.builder().build());
    }

    @Override
    public void startTransaction(final TransactionOptions transactionOptions) {
        if (inTransaction) {
            throw new IllegalStateException("Transaction already in progress");
        }
        inTransaction = true;
        // TODO: save the transaction options
        getServerSession().advanceTransactionNumber();
    }

    @Override
    public void commitTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("There is no transaction in progress");
        }
        try {
            // TODO: use proper write concern from ClientSession
            delegate.getOperationExecutor().execute(new CommitTransactionOperation(WriteConcern.ACKNOWLEDGED),
                    getTransactionReadPreferenceOrPrimary(), this);
        } finally {
            cleanupTransaction();
        }
    }

    @Override
    public void abortTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("There is no transaction in progress");
        }
        try {
            // TODO: use proper write concern from ClientSession
            delegate.getOperationExecutor().execute(new AbortTransactionOperation(WriteConcern.ACKNOWLEDGED),
                    getTransactionReadPreferenceOrPrimary(), this);
        } finally {
            cleanupTransaction();
        }
    }

    private void cleanupTransaction() {
        inTransaction = false;
        setTransactionReadPreference(null);
        if (getOptions().getAutoStartTransaction()) {
            startTransaction(getOptions().getDefaultTransactionOptions());
        }
    }

    private ReadPreference getTransactionReadPreferenceOrPrimary() {
        ReadPreference readPreference = getTransactionReadPreference();
        return readPreference == null ? ReadPreference.primary() : readPreference;
    }
}
