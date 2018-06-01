/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.mongodb.benchmark.benchmarks;

import com.mongodb.benchmark.framework.BenchmarkRunner;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;

import java.util.ArrayList;

public class FindNBenchmark<T> extends AbstractFindBenchmark<T> {

    private final int n;

    public FindNBenchmark(final String resourcePath, Class<T> clazz, final int n) {
        super(String.format("Find %d by ID", n), resourcePath, clazz);
        this.n = n;
    }

    @Override
    public void run() {
        ArrayList<T> target = new ArrayList<T>(n);
        for (int i = 0; i < (NUM_INTERNAL_ITERATIONS / n); i++) {
            for (int id = 0; i < NUM_INTERNAL_ITERATIONS; i += n) {
                collection.find(Filters.and(Filters.gte("_id", id), Filters.lt("_id", id + n))).into(target);
                target.clear();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new BenchmarkRunner(new FindNBenchmark<BsonDocument>("/benchmarks/TWEET.json", BsonDocument.class, 1), 0, 1).run();
    }
}
