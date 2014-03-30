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
 * A retry policy that allows a retry if the exception is an instance of one of the white-listed exceptions.
 */
public class ExceptionWhiteListRetryPolicy extends AbstractRetryPolicy {

    private final List<Class<? extends MongoException>> whiteList;

    /**
     * Constructs a new instance.
     *
     * @param whiteList the white-list of of allowed exceptions.
     */
    public ExceptionWhiteListRetryPolicy(final List<Class<? extends MongoException>> whiteList) {
        this.whiteList = Collections.unmodifiableList(new ArrayList<Class<? extends MongoException>>(whiteList));
    }

    /**
     * A retry is allowed if the exception is an instance of one of the white-listed exception classes.
     * @param e the exception
     * @return true if the exception is an instance of one of the white-listed exception classes
     */
    @Override
    public boolean allowRetry(final MongoException e) {
        for (Class<?> clazz : whiteList) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * As this class is immutable, it just returns itself.
     *
     * @return this
     */
    @Override
    public RetryPolicy duplicate() {
        return this;
    }

    /**
     * Gets the exception white-list.
     *
     * @return an unmodifiable list of the white-listed exceptions
     */
    public List<Class<? extends MongoException>> getWhiteList() {
        return whiteList;
    }

    @Override
    public String toString() {
        return "ExceptionWhiteListRetryPolicy{"
               + "whiteList=" + whiteList
               + '}';
    }
}
