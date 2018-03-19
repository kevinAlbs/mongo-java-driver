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
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.internal.session.ServerSessionPool;
import com.mongodb.operation.CommitTransactionOperation;
import org.bson.types.ObjectId;

final class ClientSessionImpl extends com.mongodb.internal.session.ClientSessionImpl implements ClientSession {

    private final MongoClientDelegate delegate;
    private boolean inTransaction;

    public ClientSessionImpl(final ServerSessionPool serverSessionPool, final Object originator, final ClientSessionOptions options,
                             final MongoClientDelegate delegate) {
        super(serverSessionPool, originator, options);
        this.delegate = delegate;
    }

    @Override
    public boolean hasActiveTransaction() {
        return inTransaction;
    }

    @Override
    public void startTransaction() {
        if (inTransaction) {
            throw new IllegalStateException("A transaction is already in progress");
        }
        inTransaction = true;
        getServerSession().advanceTransactionNumber();
    }

    @Override
    public void commitTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("There is no transaction in progress");
        }
        try {
            // TODO: use proper write concern from ClientSession
            delegate.getOperationExecutor().execute(new CommitTransactionOperation(WriteConcern.ACKNOWLEDGED), this);
        } finally {
            inTransaction = false;
        }
    }

    @Override
    public void abortTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("There is no transaction in progress");
        }
        try {
            // TODO: use proper write concern from ClientSession
            delegate.getOperationExecutor().execute(new CommitTransactionOperation(WriteConcern.ACKNOWLEDGED), this);
        } finally {
            inTransaction = false;
        }
    }
}
