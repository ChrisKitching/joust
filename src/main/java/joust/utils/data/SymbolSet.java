package joust.utils.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * A set which may logically contain *all* the things.
 */
public class SymbolSet extends HashSet<VarSymbol> {
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
    public SymbolSet intersect(SymbolSet other) {
        // Intersection with the universal set is the other set...
        if (other == UNIVERSAL_SET && this == UNIVERSAL_SET) {
            return this;
        }

        if (other == UNIVERSAL_SET) {
            return new SymbolSet(this);
        }

        if (this == UNIVERSAL_SET) {
            return new SymbolSet(other);
        }

        SymbolSet ret = new SymbolSet(this);
        ret.retainAll(other);

        return ret;
    }

    /**
     * Create and return a new SymbolSet containing all the elements from the input SymbolSets.
     */
    public static SymbolSet union(SymbolSet a, SymbolSet b) {
        // Unions with the universal set are... The universal set.
        if (a == UNIVERSAL_SET || b == UNIVERSAL_SET) {
            return UNIVERSAL_SET;
        }

        SymbolSet ret = new SymbolSet();
        ret.addAll(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Both functions provided so the first case is a tad faster when usable...
     */
    public static SymbolSet union(SymbolSet... a) {
        SymbolSet ret = new SymbolSet();
        for (int i = 0; i < a.length; i++) {
            if (a[i] == UNIVERSAL_SET) {
                return UNIVERSAL_SET;
            }
            ret.addAll(a[i]);
        }

        return ret;
    }

    public boolean subsetOf(SymbolSet s) {
        // If the intersection is the same size as this set, this set is a subset of that set.
        SymbolSet intersection = intersect(s);
        return intersection.size() == size();
    }

    @Override
    public String toString() {
        if (this == UNIVERSAL_SET) {
            return "#U";
        }
        return Arrays.toString(toArray());
    }

    @Override
    public int size() {
        if (this == UNIVERSAL_SET) {
            return Integer.MAX_VALUE;
        }

        return super.size();
    }
}
