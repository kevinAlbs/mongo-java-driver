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

package com.mongodb.benchmark.framework;

import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

import java.io.StringWriter;

public class EvergreenBenchmarkResultWriter implements BenchmarkResultWriter {

    private final StringWriter stringWriter = new StringWriter();
    private final JsonWriter jsonWriter = new JsonWriter(stringWriter,
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).indent(true).build());

    public EvergreenBenchmarkResultWriter() {
        jsonWriter.writeStartDocument();
        jsonWriter.writeStartArray("results");
    }

    @Override
    public void write(final BenchmarkResult benchmarkResult) {
        jsonWriter.writeStartDocument();
        jsonWriter.writeString("name", benchmarkResult.getName());
        jsonWriter.writeStartDocument("results");
        jsonWriter.writeStartDocument("1");

        jsonWriter.writeDouble("ops_per_sec",
                (benchmarkResult.getBytesPerIteration() / 1000000d) /
                        (benchmarkResult.getElapsedTimeNanosAtPercentile(50) / 1000000000d));

        jsonWriter.writeEndDocument();
        jsonWriter.writeEndDocument();
        jsonWriter.writeEndDocument();
    }

    @Override
    public void close() {
        jsonWriter.writeEndArray();
        jsonWriter.writeEndDocument();
        jsonWriter.close();
    }

    public String getResults() {
        return stringWriter.toString();
    }
}
