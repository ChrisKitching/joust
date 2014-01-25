package joust.treeinfo;

import static com.sun.tools.javac.code.Symbol.*;

import joust.utils.SymbolSet;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

/**
 * A class for representing the side effects of a particular tree node.
 */
public @Log4j2
class EffectSet {
    public static final EffectSet NO_EFFECTS = new EffectSet(EffectType.NONE);
    public static final EffectSet ALL_EFFECTS = new EffectSet(EffectType.getAllEffects());
    static {
        ALL_EFFECTS.readSymbols = SymbolSet.UNIVERSAL_SET;
        ALL_EFFECTS.writeSymbols = SymbolSet.UNIVERSAL_SET;
    }

    // The summary of all effect types this EffectSet represents.
    public int effectTypes;

    // The symbols read/written to by this EffectSet.
    SymbolSet<VarSymbol> readSymbols = new SymbolSet<>();
    SymbolSet<VarSymbol> writeSymbols = new SymbolSet<>();

    public static enum EffectType {
        NONE(0),
        READ(1),
        WRITE(2),
        EXCEPTION(4),
        IO(8);

        private static int ALL_EFFECTS = 0;

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

        // Union the affected symbol sets...
        SymbolSet<VarSymbol> rSyms = new SymbolSet<>();
        rSyms.addAll(readSymbols);
        rSyms.addAll(unionee.readSymbols);

        SymbolSet<VarSymbol> wSyms = new SymbolSet<>();
        wSyms.addAll(writeSymbols);
        wSyms.addAll(unionee.writeSymbols);

        unioned.readSymbols = rSyms;
        unioned.writeSymbols = wSyms;

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
        SymbolSet<VarSymbol> rSyms = new SymbolSet<>();
        SymbolSet<VarSymbol> wSyms = new SymbolSet<>();

        // Add the symbols from this set...
        rSyms.addAll(readSymbols);
        wSyms.addAll(writeSymbols);

        for (int i = 0; i < effectSets.length; i++) {
            EffectSet unionee = effectSets[i];

            newMask |= unionee.effectTypes;

            // Add the symbols from the new unionee...
            rSyms.addAll(unionee.readSymbols);
            wSyms.addAll(unionee.writeSymbols);
        }

        EffectSet unioned = new EffectSet(newMask);

        unioned.readSymbols = rSyms;
        unioned.writeSymbols = wSyms;

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
        newEffectSet.readSymbols = new SymbolSet<>(readSymbols);
        newEffectSet.writeSymbols = new SymbolSet<>(writeSymbols);

        return newEffectSet;
    }

    public EffectSet(final int mask) {
        effectTypes = mask;
    }

    public EffectSet(final EffectType type) {
        effectTypes = type.maskValue;
    }

    public static EffectSet write(VarSymbol sym) {
        EffectSet ret = new EffectSet(EffectType.WRITE);
        ret.writeSymbols.add(sym);

        return ret;
    }

    public static EffectSet read(VarSymbol sym) {
        EffectSet ret = new EffectSet(EffectType.READ);
        ret.readSymbols.add(sym);

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(Integer.toString(effectTypes, 2));
        if (!readSymbols.isEmpty()) {
            str.append(":R(")
               .append(Arrays.toString(readSymbols.toArray()))
               .append(")");
        }

        if (!readSymbols.isEmpty()) {
            str.append(":W(")
               .append(Arrays.toString(writeSymbols.toArray()))
               .append(")");
        }

        return str.toString();
    }
}
