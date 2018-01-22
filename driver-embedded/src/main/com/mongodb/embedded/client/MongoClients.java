/*
 * Copyright 2018 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.embedded.client;

import com.mongodb.connection.EmbeddedCluster;

import java.util.Collections;
import java.util.List;

public class MongoClients {

    // TODO: figure out how to configure
    public static EmbeddedMongoClient create(final List<String> embeddedClusterArguments) {
        return new EmbeddedMongoClientImpl(new EmbeddedCluster(embeddedClusterArguments, Collections.<String>emptyList(),
                new EmbeddedServerFactoryImpl()));
    }

    // static methods here...

    public static void lowBatteryWarning() {
        // do something...
    }

    private MongoClients() {
    }
}
