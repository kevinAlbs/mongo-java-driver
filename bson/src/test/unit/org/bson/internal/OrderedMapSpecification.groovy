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

package org.bson.internal

import spock.lang.Specification

class OrderedMapSpecification extends Specification {
    def 'no args construcor should create empty map'() {
        when:
        def map = new OrderedMap()

        then:
        map.size() == 0
    }

    def 'should put and get below threshold'() {
        when:
        def map = new OrderedMap()

        then:
        map.get('a') == null

        when:
        def putVal = map.put('a', 1)

        then:
        map.size() == 1
        map.get('a') == 1
        putVal == null
        map.get('b') == null

        when:
        putVal = map.put('b', 2)

        then:
        map.size() == 2
        map.get('a') == 1
        map.get('b') == 2
        putVal == null

        when: // replace value of 'a'
        putVal = map.put('a', 3)

        then:
        map.size() == 2
        map.get('a') == 3
        map.get('b') == 2
        putVal == 1
    }

    def 'should put and get above threshold'() {
        given:
        def map = new OrderedMap()
        map.put('1', 1)
        map.put('2', 2)
        map.put('3', 3)
        map.put('4', 4)
        map.put('5', 5)
        map.put('6', 6)
        map.put('7', 7)
        map.put('8', 8)

        when:
        def putVal = map.put('9', 9)

        then:
        putVal == null
        map.get('1') == 1
        map.get('2') == 2
        map.get('3') == 3
        map.get('4') == 4
        map.get('5') == 5
        map.get('6') == 6
        map.get('7') == 7
        map.get('8') == 8
        map.get('9') == 9
        map.get('10') == null

        when:
        putVal = map.put('10', 10)

        then:
        putVal == null
        map.get('1') == 1
        map.get('2') == 2
        map.get('3') == 3
        map.get('4') == 4
        map.get('5') == 5
        map.get('6') == 6
        map.get('7') == 7
        map.get('8') == 8
        map.get('9') == 9
        map.get('10') == 10

        when:
        putVal = map.put('10', 11)

        then:
        putVal == 10
        map.get('10') == 11
    }

    

    def 'copy constructor should contain all elements of the copied map'() {
        given:
        def from = new HashMap()
        from.put('foo', 1)
        from.put('bar', 2)

        when:
        def map = new OrderedMap(from)

        then:
        map.size() == 2
        map.get('foo') == 1
        map.get('bar') == 2
    }
}
