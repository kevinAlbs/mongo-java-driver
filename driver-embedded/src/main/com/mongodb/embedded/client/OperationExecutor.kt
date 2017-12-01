/*
 * Copyright 2017 MongoDB, Inc.
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

package com.mongodb.embedded.client

import com.mongodb.operation.ReadOperation
import com.mongodb.operation.WriteOperation

internal interface OperationExecutor {
    /**
     * Execute the read operation with the given read preference.
     *
     * @param <T> the operations result type.
     * @param operation the read operation.
     * @param readPreference the read preference.
     * @return the result of executing the operation.
     */
    fun <T> execute(operation: ReadOperation<T>): T

    /**
     * Execute the write operation.
     *
     * @param operation the write operation.
     * @param <T> the operations result type.
     * @return the result of executing the operation.
     */
    fun <T> execute(operation: WriteOperation<T>): T
}