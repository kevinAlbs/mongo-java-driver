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

package com.mongodb.internal.connection;

import java.util.List;

import static com.mongodb.assertions.Assertions.notNull;

public class CommandResultWithSequence<T, D> {
    private final T result;
    private final String documentSequenceName;
    private final List<D> documentSequence;

    public CommandResultWithSequence(final T result, final String documentSequenceName, final List<D> documentSequence) {
        this.result = notNull("result", result);
        this.documentSequenceName = documentSequenceName;
        this.documentSequence = documentSequence;
    }

    public CommandResultWithSequence(final T result) {
        this(result, null, null);
    }

    public T getCommandResult() {
        return result;
    }

    // Returns null if there is no document sequence
    public String getDocumentSequenceName() {
        return documentSequenceName;
    }

    // Returns null if there is no document sequence
    public List<D> getDocumentSequence() {
        return documentSequence;
    }
}
