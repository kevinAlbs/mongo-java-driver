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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A retry policy that is a composite of other retry policies.
 *
 * @since 3.0
 */
public class CompositeRetryPolicy implements RetryPolicy {
    private final List<RetryPolicy> retryPolicies;

    /**
     * Construct a new instance.
     *
     * @param retryPolicies the list of retry policies to compose
     */
    public CompositeRetryPolicy(final List<RetryPolicy> retryPolicies) {
        this.retryPolicies = Collections.unmodifiableList(new ArrayList<RetryPolicy>(retryPolicies));
    }

    /**
     * Call {@code begin()} on each composed retry policy.
     */
    @Override
    public void begin() {
        for (RetryPolicy cur : retryPolicies) {
            cur.begin();
        }
    }

    /**
     * Call {@code end()} on each composed retry policy.
     */
    @Override
    public void end() {
        for (RetryPolicy cur : retryPolicies) {
            cur.end();
        }
    }

    /**
     * This will allow a retry if all composed retry policies allow it.
     * @param e the exception
     * @return true if all composed retry policies allow it
     */
    @Override
    public boolean allowRetry(final MongoException e) {
        for (RetryPolicy cur : retryPolicies) {
            if (!cur.allowRetry(e)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Duplicates itself constructing a new instance of itself with a list of duplicates of every composed retry policy.
     * @return a duplicate retry policy
     */
    @Override
    public RetryPolicy duplicate() {
        List<RetryPolicy> duplicateRetryPolicies = new ArrayList<RetryPolicy>(retryPolicies.size());
        for (RetryPolicy cur : retryPolicies) {
            duplicateRetryPolicies.add(cur.duplicate());
        }
        return new CompositeRetryPolicy(duplicateRetryPolicies);
    }

    /**
     * Gets the list of composed retry policies.
     *
     * @return an unmodifiable list of composed retry policies
     */
    public List<RetryPolicy> getRetryPolicies() {
        return retryPolicies;
    }

    @Override
    public String toString() {
        return "CompositeRetryPolicy{"
               + "retryPolicies=" + retryPolicies
               + '}';
    }
}
