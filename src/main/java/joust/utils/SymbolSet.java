package joust.utils;

import java.util.Collection;
import java.util.HashSet;

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
}
