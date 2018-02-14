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

package org.bson.internal;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderedMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable {

    private static final long serialVersionUID = 362498820763181265L;

    // the indexes dictionary will not be created until the document grows to contain 8 elements
    private static final int INDEXES_THRESHOLD = 8;

    private final List<Map.Entry<K, V>> elements = new ArrayList<Map.Entry<K, V>>(INDEXES_THRESHOLD);
    private Map<K, Integer> indexes;

    public OrderedMap() {
    }

    public OrderedMap(final Map<K, V> from) {
        for (Map.Entry<K, V> cur : from.entrySet()) {
            put(cur.getKey(), cur.getValue());
        }
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean containsKey(final Object key) {
        if (indexes != null) {
            return indexes.get(key) != null;
        } else {
            for (Map.Entry<K, V> cur : elements) {
                if (cur.getKey().equals(key)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public V remove(final Object key) {
        V retVal = null;
        if (indexes != null) {
            Integer index = indexes.get(key);
            if (index != null) {
                retVal = elements.get(index).getValue();
                elements.remove((int) index);
                rebuildIndexes();
            }
        } else {
            for (Iterator<Map.Entry<K, V>> it = elements.iterator(); it.hasNext();) {
                Map.Entry<K, V> cur = it.next();
                if (cur.getKey().equals(key)) {
                    retVal = cur.getValue();
                    it.remove();
                    break;
                }
            }
        }
        return retVal;

    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    @Override
    public V put(final K key, final V value) {
        V retVal = null;
        if (indexes != null) {
            Integer index = indexes.get(key);
            if (index != null) {
                retVal = elements.get(index).getValue();
                elements.get(index).setValue(value);
            } else {
                elements.add(new Entry<K, V>(key, value));
                indexes.put(key, elements.size() - 1);
            }
        } else {
            Map.Entry<K, V> entry = getEntry(key);
            if (entry != null) {
                retVal = entry.setValue(value);
            } else {
                elements.add(new Entry<K, V>(key, value));
                rebuildIndexes();
            }
        }
        return retVal;
    }

    @Override
    public V get(final Object key) {
        if (indexes != null) {
            Integer index = indexes.get(key);
            if (index != null) {
                return elements.get(index).getValue();
            }
        } else {
            for (Map.Entry<K, V> cur : elements) {
                if (cur.getKey().equals(key)) {
                    return cur.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void clear() {
        elements.clear();
        indexes = null;
    }

    private Map.Entry<K, V> getEntry(final Object key) {
        if (indexes != null) {
            Integer index = indexes.get(key);
            if (index != null) {
                return elements.get(index);
            }
        } else {
            for (Map.Entry<K, V> cur : elements) {
                if (cur.getKey().equals(key)) {
                    return cur;
                }
            }
        }
        return null;
    }

    private void rebuildIndexes() {
        if (elements.size() <= INDEXES_THRESHOLD) {
            indexes = null;
        } else {
            if (indexes == null) {
                indexes = new HashMap<K, Integer>(INDEXES_THRESHOLD * 2);
            } else {
                indexes.clear();
            }
            for (int i = 0; i < elements.size(); i++) {
                indexes.put(elements.get(i).getKey(), i);
            }
        }
    }

    private static final class Entry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private V value;

        Entry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry<?, ?> entry = (Entry<?, ?>) o;

            if (!key.equals(entry.key)) {
                return false;
            }
            if (value != null ? !value.equals(entry.value) : entry.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new Iterator<Map.Entry<K, V>>() {
                private final Iterator<Map.Entry<K, V>> listIterator = elements.iterator();
                private Map.Entry<K, V> lastReturned;

                @Override
                public boolean hasNext() {
                    return listIterator.hasNext();
                }

                @Override
                public Map.Entry<K, V> next() {
                    lastReturned = listIterator.next();
                    return lastReturned;
                }

                @Override
                public void remove() {
                    listIterator.remove();
                    OrderedMap.this.rebuildIndexes();
                }
            };
        }

        @Override
        public void clear() {
            OrderedMap.this.clear();
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Map.Entry<K, V> candidate = getEntry(entry.getKey());
            return candidate != null && candidate.equals(entry);
        }

        @Override
        public boolean remove(final Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                Map.Entry<K, V> candidate = getEntry(entry.getKey());
                if (candidate == null) {
                    return false;
                }
                if (candidate.equals(entry)) {
                    OrderedMap.this.remove(candidate.getKey());
                }
            }
            return false;
        }
    }
}
