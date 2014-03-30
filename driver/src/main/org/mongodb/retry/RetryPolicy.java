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
 * A retry policy for MongoDB operations.  A single instance of this policy will be used per operation.
 *
 * @since 3.0
 */
public interface RetryPolicy {

    /**
     * Called before the first attempt is made to execute the operation.
     */
    void begin();

    /**
     * Called after the last attempt is made to execute the operation.
     */
    void end();

    /**
     * Determine whether the policy allows the operation to be retried.  This method is allowed to block for long periods of time,
     * if required by the policy.  For example, this method may sleep for some period of time before returning.
     *
     *
     * @return true if the policy allows the operation to be retried.
     */
    boolean allowRetry(MongoException e);

    /**
     * Return a fresh instance of this policy.
     *
     * @return a fresh instance
     */
    RetryPolicy duplicate();
}
