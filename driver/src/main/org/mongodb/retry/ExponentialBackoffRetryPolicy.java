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

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A retry policy that sleeps for an exponentially-increasing interval before allowing a retry.
 *
 * @since 3.0
 */
public class ExponentialBackoffRetryPolicy extends SleepingRetryPolicy {

    private final long initialSleepTimeMS;
    private int numAttempts;

    /**
     * Constructs a new instance.
     *
     * @param initialSleepTime the initial sleep time
     * @param timeUnit the time unit
     */
    public ExponentialBackoffRetryPolicy(final long initialSleepTime, final TimeUnit timeUnit) {
        this.initialSleepTimeMS = MILLISECONDS.convert(initialSleepTime, timeUnit);
    }

    /**
     * Gets the initial sleep time in the requested time unit.
     *
     * @param timeUnit the time unit to get the initial sleep time in
     * @return the initial sleep time in the requested time unit.
     */
    public long getInitialSleepTime(final TimeUnit timeUnit) {
        return timeUnit.convert(initialSleepTimeMS, MILLISECONDS);
    }

    /**
     * Gets the sleep time.  The amount returned increased exponentially with each call.
     *
     * @return the sleep time
     */
    @Override
    protected long getSleepTimeMS() {
        numAttempts++;
        return initialSleepTimeMS * numAttempts;
    }

    /**
     * Duplicates this policy.
     *
     * @return a duplicate policy
     */
    @Override
    public RetryPolicy duplicate() {
        return new ExponentialBackoffRetryPolicy(initialSleepTimeMS, MILLISECONDS);
    }

    @Override
    public String toString() {
        return "ExponentialBackoffRetryPolicy{"
               + "initialSleepTimeMS=" + initialSleepTimeMS
               + '}';
    }
}
