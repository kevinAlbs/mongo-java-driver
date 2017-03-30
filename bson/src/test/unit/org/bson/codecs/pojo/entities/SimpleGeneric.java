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

public class SimpleGeneric<T, V, Z> {
    private T t;
    private V v;
    private Z z;
    private EvenSimplerGeneric<T, V> simplerOne;
    private EvenSimplerGeneric<T, Z> simplerTwo;

    public SimpleGeneric() {
    }

    public SimpleGeneric(final T t, final V v, final Z z, final EvenSimplerGeneric<T, V> simplerOne, final EvenSimplerGeneric<T, Z> simplerTwo) {
        this.simplerOne = simplerOne;
        this.t = t;
        this.v = v;
        this.z = z;
        this.simplerOne = simplerOne;
        this.simplerTwo = simplerTwo;
    }

    @Override
    public String toString() {
        return "SimpleGeneric{" +
                       "t=" + t +
                       ", v=" + v +
                       ", z=" + z +
                       ", simplerOne=" + simplerOne +
                       ", simplerTwo=" + simplerTwo +
                       '}';
    }
}
