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
 * A retry policy that sleeps for an upper-bounded exponentially-increasing interval before allowing a retry.
 *
 * @since 3.0
 */
public class BoundedExponentialBackoffRetryPolicy extends ExponentialBackoffRetryPolicy {
    private final long maxSleepTimeMS;

    /**
     * Constructs a new instance.
     *
     * @param startingSleepTime the starting sleep time
     * @param maxSleepTime the upper bound on the sleep time
     * @param timeUnit the time unit
     */
    public BoundedExponentialBackoffRetryPolicy(final long startingSleepTime, final long maxSleepTime, final TimeUnit timeUnit) {
        super(startingSleepTime, timeUnit);
        this.maxSleepTimeMS = MILLISECONDS.convert(maxSleepTime, timeUnit);
    }

    /**
     * Returns the minimum of the upper bound on the sleep time and the sleep time requested by its superclass.
     *
     * @return the sleep time
     */
    @Override
    protected long getSleepTimeMS() {
       return Math.min(maxSleepTimeMS, super.getSleepTimeMS());
    }

    /**
     * Gets the upper bound on the sleep time in the requested time unit.
     *
     * @param timeUnit the time unit to get the upper bound in
     * @return the upper bound on the sleep time in the requested time unit.
     */
    public long getMaxSleepTime(final TimeUnit timeUnit) {
        return timeUnit.convert(maxSleepTimeMS, MILLISECONDS);
    }

    @Override
    public String toString() {
        return "BoundedExponentialBackoffRetryPolicy{"
               + "initialSleepTimeMS=" + getInitialSleepTime(MILLISECONDS)
               + "maxSleepTimeMS=" + maxSleepTimeMS
               + '}';
    }
}
