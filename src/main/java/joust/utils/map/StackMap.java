package joust.utils.map;

import java.util.AbstractMap;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * A stack of Maps providing aggregate map operations.
 */
public class StackMap<K, V> extends AbstractMap<K, V> {
    // The stack of associated maps.
    private final Deque<Map<K, V>> mapStack = new LinkedList<>();

    /**
     * Get the value for the given key from the map, exploring down the chain of maps as needed.
     * @param key The key to get the value for.
     * @return The value for the given key, or null if none exists.
     */
    @Override
    public V get(Object key) {
        // Run down the stack looking for this object...
        for (Map<K, V> map : mapStack) {
            V foundHere = map.get(key);
            if (foundHere != null) {
                return foundHere;
            }
        }

        return null;
    }

    /**
     * Add the given key-value mapping to the hashmap, either updating the existing mapping or adding a new one
     * to the local hashmap.
     * @param key Key to associate with value.
     * @param value Value to associate with key.
     * @return The previous value for the given key, if any, else null.
     */
    @Override
    public V put(K key, V value) {
        // Attempt to find an existing key to replace here. If so, replace it and return.
        for (Map<K, V> map : mapStack) {
            if (map.containsKey(key)) {
                return map.put(key, value);
            }
        }

        // If it's a new element, add it to the top of the stack.
        return mapStack.peek().put(key, value);
    }

    @Override
    public V remove(Object key) {
        for (Map<K, V> map : mapStack) {
            if (map.containsKey(key)) {
                return map.remove(key);
            }
        }

        throw new NoSuchElementException("No such element in this StackMap!");
    }

    @Override
    public boolean containsKey(Object key) {
        for (Map<K, V> map : mapStack) {
            if (map.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map<K, V> map : mapStack) {
            if (map.containsValue(value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();

        for (Map<K, V> map : mapStack) {
            keys.addAll(map.keySet());
        }

        return keys;
    }

    @Override
    public boolean isEmpty() {
        for (Map<K, V> map : mapStack) {
            if (!map.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int size() {
        int size = 0;

        for (Map<K, V> map : mapStack) {
            size += map.size();
        }

        return size;
    }

    @Override
    public void clear() {
        mapStack.clear();
    }

    public void pushMap() {
        mapStack.push(new HashMap<K, V>());
    }

    public void pushMap(int initialCapacity) {
        mapStack.push(new HashMap<K, V>(initialCapacity));
    }

    public void pushMap(int initialCapacity, float loadFactor) {
        mapStack.push(new HashMap<K, V>(initialCapacity, loadFactor));
    }

    public void pushMap(Map<? extends K, ? extends V> m) {
        pushMap(m.size());
        putAll(m);
    }

    public Map<K, V> popMap() {
        return mapStack.pop();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("EntrySet not supported on StackMap.");
    }
}
