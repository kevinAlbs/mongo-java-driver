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
 * An abstract retry policy that sleeps for a period of time on calls to {@code allowRetry}.  The amount of time for each sleep is
 * determined by the concrete subclass.
 *
 * @since 3.0
 */
public abstract class SleepingRetryPolicy extends AbstractRetryPolicy {
    /**
     * Allows the retry after successfully sleeping.
     *
     * @param e the exception
     * @return return true if the sleep completed successfully, and false if the sleep was interrupted.
     */
    @Override
    public boolean allowRetry(final MongoException e) {
        try {
            Thread.sleep(getSleepTimeMS());
            return true;
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gets the amount of time to sleep for.  Subclasses are free to return a different amount of time on each call.  This method is
     * guaranteed to be called just once for each call to {@code allowRetry()}.
     *
     * @return the number of milliseconds to sleep before allowing the retry
     */
    protected abstract long getSleepTimeMS();
}
