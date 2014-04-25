package joust.tree.annotatedtree.treeinfo;

import static com.sun.tools.javac.code.Symbol.*;

import joust.utils.data.SymbolSet;
import joust.utils.tree.TreeUtils;
import lombok.extern.java.Log;

/**
 * A class for representing the side effects of a particular tree node.
 */
@Log
public class EffectSet {
    public static final EffectSet NO_EFFECTS = new EffectSet(EffectType.NONE);
    public static final EffectSet ALL_EFFECTS = new EffectSet(EffectType.getAllEffects()) {
        {
            readInternal = SymbolSet.UNIVERSAL_SET;
            writeInternal = SymbolSet.UNIVERSAL_SET;
            readEscaping = SymbolSet.UNIVERSAL_SET;
            writeEscaping = SymbolSet.UNIVERSAL_SET;
        }
    };

    public static final int ESCAPING_ONLY = EffectType.READ_ESCAPING.maskValue
                                          | EffectType.WRITE_ESCAPING.maskValue
                                          | EffectType.EXCEPTION.maskValue
                                          | EffectType.IO.maskValue;

    // The summary of all effect types this EffectSet represents.
    public int effectTypes;

    // The symbols read/written to by this EffectSet.
    public SymbolSet readInternal = new SymbolSet();
    public SymbolSet writeInternal = new SymbolSet();

    public SymbolSet readEscaping = new SymbolSet();
    public SymbolSet writeEscaping = new SymbolSet();

    public enum EffectType {
        NONE(0),
        READ_INTERNAL(1),
        WRITE_INTERNAL(2),
        READ_ESCAPING(4),
        WRITE_ESCAPING(8),
        EXCEPTION(16),
        IO(32);

        private static int ALL_EFFECTS;

        public static int getAllEffects() {
            if (ALL_EFFECTS == 0) {
                for (EffectType e : values()) {
                    ALL_EFFECTS |= e.maskValue;
                }
            }

            return ALL_EFFECTS;
        }

        public final int maskValue;
        EffectType(int mask) {
            maskValue = mask;
        }
    }

    /**
     * Return the effect set representing the current effect set unioned with the given effect set.
     * Provided in addition to the varargs one for performance reasons.
     *
     * @param unionee An EffectSet to add to this one.
     * @return The effect set representing the union of the effects of this set with the argument.
     */
    public EffectSet union(EffectSet unionee) {
        if (unionee == null) {
            return this;
        }

        // Union the effect summary...
        EffectSet unioned = new EffectSet(effectTypes | unionee.effectTypes);

        unioned.readInternal = SymbolSet.union(readInternal, unionee.readInternal);
        unioned.writeInternal = SymbolSet.union(writeInternal, unionee.writeInternal);
        unioned.readEscaping = SymbolSet.union(readEscaping, unionee.readEscaping);
        unioned.writeEscaping = SymbolSet.union(writeEscaping, unionee.writeEscaping);

        return unioned;
    }

    /**
     * Creates a new EffectSet containing all the escaping/non escaping read/write effects of this, plus all
     * the escaping read/write effects of unionee, plus all other effects.
     */
    public EffectSet unionEscaping(EffectSet unionee) {
        if (unionee == null) {
            log.warning("Unionee was null.");
            return this;
        }

        EffectSet unioned = new EffectSet(effectTypes | (unionee.effectTypes & ESCAPING_ONLY));

        unioned.readInternal = new SymbolSet(readInternal);
        unioned.writeInternal = new SymbolSet(writeInternal);
        unioned.readEscaping = SymbolSet.union(readEscaping, unionee.readEscaping);
        unioned.writeEscaping = SymbolSet.union(writeEscaping, unionee.writeEscaping);

        return unioned;
    }

    public EffectSet dropUnescaping() {
        EffectSet unioned = new EffectSet(effectTypes & ESCAPING_ONLY);

        unioned.readInternal = new SymbolSet();
        unioned.writeInternal = new SymbolSet();
        unioned.readEscaping = new SymbolSet(readEscaping);
        unioned.writeEscaping = new SymbolSet(writeEscaping);

        return unioned;
    }

    /**
     * Convenience varargs method for unioning a collection of EffectSets together.
     *
     * @param effectSets Effect sets to union with this one.
     * @return The result of the union.
     */
    public EffectSet union(EffectSet... effectSets) {
        if (effectSets == null || effectSets.length == 0) {
            return this;
        }

        int newMask = effectTypes;
        SymbolSet riSyms = new SymbolSet(readInternal);
        SymbolSet wiSyms = new SymbolSet(writeInternal);
        SymbolSet reSyms = new SymbolSet(readEscaping);
        SymbolSet weSyms = new SymbolSet(writeEscaping);


        for (int i = 0; i < effectSets.length; i++) {
            EffectSet unionee = effectSets[i];

            newMask |= unionee.effectTypes;

            // Add the symbols from the new unionee...
            riSyms.addAll(unionee.readInternal);
            wiSyms.addAll(unionee.writeInternal);
            reSyms.addAll(unionee.readEscaping);
            weSyms.addAll(unionee.writeEscaping);
        }

        EffectSet unioned = new EffectSet(newMask);

        unioned.readInternal = riSyms;
        unioned.writeInternal = wiSyms;
        unioned.readEscaping = reSyms;
        unioned.writeEscaping = weSyms;

        return unioned;
    }

    /**
     * Return the effect set representing the current effect set plus the given summary fields.
     *
     * @param effect An effect to add to this EffectSet.
     * @return The effect set representing the effects of this EffectSet plus the given extra one.
     */
    public EffectSet union(EffectType effect) {
        if (effect == null) {
            return this;
        }

        EffectSet newEffectSet = new EffectSet(effectTypes | effect.maskValue);
        newEffectSet.readInternal = new SymbolSet(readInternal);
        newEffectSet.writeInternal = new SymbolSet(writeInternal);
        newEffectSet.readEscaping = new SymbolSet(readEscaping);
        newEffectSet.writeEscaping = new SymbolSet(writeEscaping);

        return newEffectSet;
    }

    public EffectSet(final int mask) {
        effectTypes = mask;
    }

    public EffectSet(final EffectType type) {
        effectTypes = type.maskValue;
    }

    public static EffectSet write(VarSymbol sym) {
        EffectSet ret;
        if (TreeUtils.isLocalVariable(sym)) {
            ret = new EffectSet(EffectType.WRITE_INTERNAL);
            ret.writeInternal.add(sym);
        } else {
            ret = new EffectSet(EffectType.WRITE_ESCAPING);
            ret.writeEscaping.add(sym);
        }

        return ret;
    }

    public static EffectSet read(VarSymbol sym) {
        EffectSet ret;
        if (TreeUtils.isLocalVariable(sym)) {
            ret = new EffectSet(EffectType.READ_INTERNAL);
            ret.readInternal.add(sym);
        } else {
            ret = new EffectSet(EffectType.READ_ESCAPING);
            ret.readEscaping.add(sym);
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(Integer.toString(effectTypes, 2));
        if (!readInternal.isEmpty()) {
            str.append(":RI(")
               .append(readInternal.toString())
               .append(")");
        }

        if (!readEscaping.isEmpty()) {
            str.append(":RE(")
               .append(readEscaping.toString())
               .append(")");
        }

        if (!writeInternal.isEmpty()) {
            str.append(":WI(")
               .append(writeInternal.toString())
               .append(")");
        }

        if (!writeEscaping.isEmpty()) {
            str.append(":WE(")
               .append(writeEscaping.toString())
               .append(")");
        }

        return str.toString();
    }

    /**
     * Returns true if this effect set contains every effect described by the input set, false otherwise.
     */
    public boolean contains(EffectSet effectSet) {
        // If any of the bits not set on both masks are set on effectSet.effectTypes, it contains an effect that
        // isn't present in this EffectSet, so we don't contain it.
        if (((effectSet.effectTypes | effectTypes) ^ effectTypes) != 0) {
            return false;
        }

        // Check appropriate symbol sets...
        if (effectSet.contains(EffectType.READ_ESCAPING)) {
            if (!effectSet.readEscaping.subsetOf(readEscaping)) {
                return false;
            }
        }

        if (effectSet.contains(EffectType.WRITE_ESCAPING)) {
            if (!effectSet.writeEscaping.subsetOf(writeEscaping)) {
                return false;
            }
        }

        if (effectSet.contains(EffectType.WRITE_INTERNAL)) {
            if (!effectSet.readInternal.subsetOf(readInternal)) {
                return false;
            }
        }

        if (effectSet.contains(EffectType.WRITE_INTERNAL)) {
            if (!effectSet.writeInternal.subsetOf(writeInternal)) {
                return false;
            }
        }

        return true;
    }

    public boolean contains(EffectType effect) {
        return (effectTypes & effect.maskValue) != 0;
    }

    public boolean contains(int effectMask) {
        return (effectTypes & effectMask) != 0;
    }

    public boolean containsAny(EffectType... effects) {
        int unifiedMask = 0;
        for (int i = 0; i < effects.length; i++) {
            unifiedMask |= effects[i].maskValue;
        }

        return (effectTypes & unifiedMask) != 0;
    }

    public boolean writesSymbol(VarSymbol sym) {
        if (writeInternal != null) {
            if (writeInternal.contains(sym)) {
                return true;
            }
        }

        if (writeEscaping != null) {
            return writeEscaping.contains(sym);
        }

        return false;
    }

    public boolean readsSymbol(VarSymbol sym) {
        if (readInternal != null) {
            if (readInternal.contains(sym)) {
                return true;
            }
        }

        if (readEscaping != null) {
            return readEscaping.contains(sym);
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EffectSet)) {
            return false;
        }

        EffectSet cast = (EffectSet) obj;

        return contains(cast) && cast.contains(this);
    }

    // Kryo constructor
    private EffectSet() {}
}
