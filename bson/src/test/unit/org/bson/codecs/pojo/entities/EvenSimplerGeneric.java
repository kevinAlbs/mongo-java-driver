/*
 * Copyright 2017 MongoDB, Inc.
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

package org.bson.codecs.pojo.entities;

public class EvenSimplerGeneric<T, V> {
    private T t;
    private V v;
    private EvenSimplerGeneric<V, T> child;

    public EvenSimplerGeneric() {
    }

    public EvenSimplerGeneric(final T t, final V v, final EvenSimplerGeneric<V, T> child) {
        this.t = t;
        this.v = v;
        this.child = child;
    }

    @Override
    public String toString() {
        return "EvenSimplerGeneric{" +
                       "t=" + t +
                       "v=" + v +
                       "child=" + child +
                       '}';
    }
}
