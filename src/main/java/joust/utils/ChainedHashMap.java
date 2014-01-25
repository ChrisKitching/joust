package joust.utils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A chain of interconnected hashmaps, allowing one logical map to span a series of HashMaps.
 * This allows numerous maps that share many keys to be compactly represented.
 */
public class ChainedHashMap<K, V> extends AbstractMap<K, V> {
    // The next map in the chain.
    private ChainedHashMap<K, V> next;

    private HashMap<K, V> backingMap;

    // Constructors matching HashMap...
    public ChainedHashMap() {
        backingMap = new HashMap<>();
    }

    public ChainedHashMap(int initialCapacity) {
        backingMap = new HashMap<>(initialCapacity);
    }

    public ChainedHashMap(int initialCapacity, float loadFactor) {
        backingMap = new HashMap<>(initialCapacity, loadFactor);
    }

    public ChainedHashMap(Map<? extends K, ? extends V> m) {
        backingMap = new HashMap<>(m);
    }

    /**
     * Get the value for the given key from the map, exploring down the chain of maps as needed.
     * @param key The key to get the value for.
     * @return The value for the given key, or null if none exists.
     */
    @Override
    public V get(Object key) {
        V foundHere = backingMap.get(key);
        if (foundHere != null) {
            return foundHere;
        }

        if (next != null) {
            return next.get(key);
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
        if (backingMap.get(key) != null) {
            return backingMap.put(key, value);
        }

        // Otherwise, continue the search for an existing key down the entire chain.
        if (next != null) {
            return next.get(key);
        }

        // After failing to find an existing key, place the element locally.
        return backingMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        // Attempt to find an existing key here.
        if (backingMap.get(key) != null) {
            return backingMap.remove(key);
        }

        // Otherwise, continue the search for an existing key down the entire chain.
        if (next != null) {
            return next.remove(key);
        }

        // After failing to find an existing key, remove the element locally.
        return backingMap.remove(key);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("EntrySet not supported on ChainedHashMap.");
    }

    /**
     * Get a new ChainedHashMap connected to the wrapee.
     * @param wrapee The ChainedHashMap to which the new map shall connect.
     * @param <A> The key-type for the new hashmap (The same as that of the wrapee)
     * @param <B> The value-type for the new hashmap (The same as that of the wrapee)
     * @return The newly-created ChainedHashMap.
     */
    public static<A, B> ChainedHashMap<A, B> wrap(ChainedHashMap<A, B> wrapee) {
        ChainedHashMap<A, B> ret = new ChainedHashMap<>();
        ret.next = wrapee;
        return ret;
    }
}
