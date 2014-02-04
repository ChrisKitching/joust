package joust.utils;

import java.util.Collection;
import java.util.HashSet;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * A set which may logically contain *all* the things.
 */
public class SymbolSet<T> extends HashSet<T> {
    public static final SymbolSet UNIVERSAL_SET = new SymbolSet();

    public SymbolSet() { }
    public SymbolSet(Collection s) {
        super(s);
    }
    public SymbolSet(int initialCapacity) {
        super(initialCapacity);
    }
    public SymbolSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    public boolean isEmpty() {
        if (this == UNIVERSAL_SET) {
            return false;
        }

        return super.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (this == UNIVERSAL_SET) {
            return true;
        }

        return super.contains(o);
    }

    /**
     * Computes the intersection of this SymbolSet with the input one, returning the result as a new set.
     */
    public SymbolSet<T> intersect(SymbolSet<T> other) {

        SymbolSet<T> ret = new SymbolSet<>(this);
        ret.retainAll(other);

        return ret;
    }

    /**
     * Create and return a new SymbolSet containing all the elements from the input SymbolSets.
     */
    public static<V> SymbolSet<V> union(SymbolSet<V> a, SymbolSet<V> b) {
        SymbolSet<V> ret = new SymbolSet<>();
        ret.addAll(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Both functions provided so the first case is a tad faster when usable...
     */
    public static<V> SymbolSet<V> union(SymbolSet<V>... a) {
        SymbolSet<V> ret = new SymbolSet<>();
        for (int i = 0; i < a.length; i++) {
            ret.addAll(a[i]);
        }

        return ret;
    }
}
