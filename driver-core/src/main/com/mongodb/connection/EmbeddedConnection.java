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

package com.mongodb.connection;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;

/**
 *
 * @since 3.8
 * @mongo
 */
public interface EmbeddedConnection extends Closeable {

    /**
     * Send a message and return the response message
     *
     * @param message the message
     * @return the response message
     */
    ByteBuffer sendAndReceive(List<ByteBuffer> message);

    @Override
    void close();
}
