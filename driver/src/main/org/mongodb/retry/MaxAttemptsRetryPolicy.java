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
 * A retry policy that places an upper bound on the number of retries.
 *
 * @since 3.0
 */
public class MaxAttemptsRetryPolicy extends AbstractRetryPolicy {
    private final int maxAttempts;
    private int numAttempts = 0;

    /**
     * Constructs a new instance.
     *
     * @param maxAttempts the upper bound on the number of retries.
     */
    public MaxAttemptsRetryPolicy(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Gets the upper bound on the number of retries allowed by this policy.
     *
     * @return the upper bound
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * The number of retries attempted so far on this instance of the policy.
     *
     * @return the number of attempts so far.
     */
    public int getNumAttempts() {
        return numAttempts;
    }

    /**
     * Allows retry if the number of attempts so far has not exceeded the upper bound.
     *
     * @param e the exception (ignored)
     * @return true if the number of attempts so far has not exceeded the upper bound
     */
    @Override
    public boolean allowRetry(final MongoException e) {
        numAttempts++;
        return numAttempts <= maxAttempts;
    }

    /**
     * Duplicates this policy.
     *
     * @return a duplicate of this policy
     */
    @Override
    public RetryPolicy duplicate() {
        return new MaxAttemptsRetryPolicy(maxAttempts);
    }

    @Override
    public String toString() {
        return "MaxAttemptsRetryPolicy{"
               + "maxAttempts=" + maxAttempts
               + '}';
    }
}
