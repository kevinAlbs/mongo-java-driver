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

package com.mongodb;

import com.mongodb.annotations.Immutable;

import static com.mongodb.assertions.Assertions.isTrueArgument;
import static com.mongodb.assertions.Assertions.notNull;

/**
 * Options to apply to transactions.
 *
 * @see com.mongodb.session.ClientSession
 * @see ClientSessionOptions
 * @since 3.8
 * @mongodb.server.release 4.0
 */
@Immutable
public final class TransactionOptions {
    private final ReadConcern readConcern;
    private final WriteConcern writeConcern;

    /**
     * Gets the read concern.
     *
     * @return the read concern, which defaults to {@link ReadConcern#DEFAULT}
     */
    public ReadConcern getReadConcern() {
        return readConcern;
    }

    /**
     * Gets the write concern.
     *
     * @return the write concern, which defaults to {@link WriteConcern#ACKNOWLEDGED}
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * Gets an instance of a builder
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionOptions that = (TransactionOptions) o;

        if (readConcern != null ? !readConcern.equals(that.readConcern) : that.readConcern != null) {
            return false;
        }
        if (writeConcern != null ? !writeConcern.equals(that.writeConcern) : that.writeConcern != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = readConcern != null ? readConcern.hashCode() : 0;
        result = 31 * result + (writeConcern != null ? writeConcern.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TransactionOptions{readConcern="
                + readConcern + ", writeConcern="
                + writeConcern + '}';
    }

    /**
     * The builder for transaction options
     */
    public static final class Builder {
        private ReadConcern readConcern = ReadConcern.DEFAULT;
        private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

        /**
         * Sets the read concern.
         *
         * @param readConcern the read concern
         * @return this
         */
        public Builder readConcern(final ReadConcern readConcern) {
            this.readConcern = notNull("readConcern", readConcern);
            return this;
        }

        /**
         * Sets the write concern.
         *
         * @param writeConcern the write concern, which must be acknowledged
         * @return this
         */
        public Builder writeConcern(final WriteConcern writeConcern) {
            this.writeConcern = notNull("writeConcern", writeConcern);
            isTrueArgument("acknowleded write concern", writeConcern.isAcknowledged());
            return this;
        }

        /**
         * Build the transaction options instance.
         *
         * @return The {@code TransactionOptions}
         */
        public TransactionOptions build() {
            return new TransactionOptions(this);
        }

        private Builder() {
        }
    }


    private TransactionOptions(final Builder builder) {
        readConcern = builder.readConcern;
        writeConcern = builder.writeConcern;
    }
}
