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
import java.util.List;

/**
 * An Embedded Server
 *
 * @since 3.8
 * @mongodb.server.release 3.8
 */
public interface EmbeddedServer extends Closeable {

    /**
     * Create a connection to the server.
     *
     * @return the connection
     */
    EmbeddedConnection createConnection();

    /**
     * Pump the message queue.
     */
    void pump();

    @Override
    void close();
}
