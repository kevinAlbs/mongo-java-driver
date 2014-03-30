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

package org.mongodb.retry;

import org.mongodb.MongoException;

/**
 * A retry policy which does not allow any retries.
 *
 * @since 3.0
 */
public class NoRetriesRetryPolicy extends AbstractRetryPolicy {
    @Override
    public boolean allowRetry(final MongoException e) {
        return false;
    }

    @Override
    public RetryPolicy duplicate() {
        return this;
    }

    @Override
    public String toString() {
        return "NoRetriesRetryPolicy{}";
    }
}
