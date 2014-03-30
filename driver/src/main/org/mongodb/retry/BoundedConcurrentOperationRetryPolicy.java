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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A retry policy that allows a bounded number of concurrent operations to be retried
 *
 * @since 3.0
 */
public class BoundedConcurrentOperationRetryPolicy implements RetryPolicy {
    private final int maxConcurrentOperations;
    private final AtomicInteger concurrentOperationCount = new AtomicInteger();

    /**
     * Construct a new instance.
     *
     * @param maxConcurrentOperations the maximum number of concurrent operations that can be retried
     */
    public BoundedConcurrentOperationRetryPolicy(final int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    /**
     * Increments the number of concurrent operations
     */
    @Override
    public void begin() {
        concurrentOperationCount.incrementAndGet();
    }

    /**
     * Decrements the number of concurrent operations.
     */
    @Override
    public void end() {
        concurrentOperationCount.decrementAndGet();
    }

    /**
     * Allow retry of the number of concurrent operations is less than or equal to the maximum allowed.
     * @param e the exception (ignored)
     * @return true if the number of concurrent operations is less than or equal to the maximum allowed
     */
    @Override
    public boolean allowRetry(final MongoException e) {
        return concurrentOperationCount.get() <= maxConcurrentOperations;
    }

    /**
     * Return the same instance, so that the concurrent operation count is shared by all instances duplicated from the same initial
     * instance.
     * @return this
     */
    @Override
    public RetryPolicy duplicate() {
        return this;
    }

    @Override
    public String toString() {
        return "BoundedConcurrentOperationRetryPolicy{"
               + "maxConcurrentOperations=" + maxConcurrentOperations
               + '}';
    }
}
