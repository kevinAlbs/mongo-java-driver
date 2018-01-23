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

import com.mongodb.client.MongoClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.Assume.assumeTrue;

class EmbeddedServerFixture {

    private static final String DEFAULT_DATABASE_NAME = "JavaDriverTest";
    private final static String EMBEDDED_OPTION_PREFIX = "org.mongodb.test.embedded";


    static String getDefaultDatabaseName() {
        return DEFAULT_DATABASE_NAME;
    }

    static MongoClient getMongoClient() {
        List<String> embeddedServerOptions = getEmbeddedServerOptions();
        assumeTrue("Requires `org.mongodb.test.embedded.` settings to be configured.", !embeddedServerOptions.isEmpty());
        return MongoClients.create(embeddedServerOptions);
    }

    private static List<String> getEmbeddedServerOptions() {
        List<String> embeddedServerOptions = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(EMBEDDED_OPTION_PREFIX)) {
                key = key.substring(EMBEDDED_OPTION_PREFIX.length(), key.length());
                String value = entry.getValue().toString();
                embeddedServerOptions.add(format("--%s=%s", key, value));
            }

        }
        return embeddedServerOptions;
    }

    private EmbeddedServerFixture(){
    }
}
