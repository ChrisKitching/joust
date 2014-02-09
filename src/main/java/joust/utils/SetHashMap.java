package joust.utils;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A HashMap relating keys to sets of elements. Adds methods for adding/removing elements from the stored
 * sets without having to worry about the existence of the actual set itself.
 * @param <K> The type of the keys into the HashMap.
 * @param <V> The type of the values in the lists related to keys.
 */
public class SetHashMap<K, V> extends HashMap<K, HashSet<V>> {
    /**
     * Add value to the set associated with key.
     */
    public void listAdd(K key, V value) {
        HashSet<V> values = get(key);

        // Initialise the list if this is the first element.
        if (values == null) {
            values = new HashSet<>();
            put(key, values);
        }

        // Otherwise, add the new element to the list.
        values.add(value);
    }

    /**
     * Remove value from the set associated with key. If this makes the set empty, drop it.
     */
    public void listRemove(K key, V value) {
        HashSet<V> values = get(key);

        // Stop if we have no mapping for key.
        if (values == null) {
            return;
        }

        values.remove(value);

        // If the set is now empty, drop it.
        if (values.isEmpty()) {
            remove(key);
        }
    }
}
