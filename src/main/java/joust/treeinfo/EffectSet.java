package joust.treeinfo;

/**
 * A class for representing the side effects of a particular tree node.
 */
public class EffectSet {
    public static enum Effects {
        NONE (0),
        READ_LOCAL(1),
        READ_GLOBAL(2),
        WRITE_LOCAL(4),
        WRITE_GLOBAL(8),
        EXCEPTION (16),
        IO (32);

        public final int maskValue;
        Effects(int mask) {
            maskValue = mask;
        }
    }

    // Array containing all effect sets indexed by mask.
    private static EffectSet[] sEffectSets;
    public static void init() {
        // Prevent multiple-initialisation.
        if (sEffectSets != null) {
            return;
        }

        // Find the largest effect bit.
        int highestEffectBit = 0;

        Effects[] effects = Effects.values();
        for (int i = 0; i < effects.length; i++) {
            if (effects[i].maskValue > highestEffectBit) {
                highestEffectBit = effects[i].maskValue;
            }
        }

        // We'll need to construct EffectSet singletons to contain every representable combination.
        highestEffectBit = (highestEffectBit * 2) - 1;

        // highestEffectBit now holds the maximum EffectSet value representable. Build them all.
        sEffectSets = new EffectSet[highestEffectBit];
        for (int i = 0; i < highestEffectBit; i++) {
            sEffectSets[i] = new EffectSet(i);
        }
    }

    /**
     * Gets an effect set for a given mask.
     *
     * @param mask Effect mask for the desired effect set.
     * @return The EffectSet with the given mask.
     */
    public static EffectSet getEffectSet(int mask) {
        return sEffectSets[mask];
    }

    public static EffectSet getEffectSet(Effects effect) {
        return sEffectSets[effect.maskValue];
    }

    public static EffectSet getEffectSet(Effects... effects) {
        int mask = effects[0].maskValue;

        for (int i = 1; i < effects.length; i++) {
            mask |= effects[i].maskValue;
        }

        return sEffectSets[mask];
    }

    public final int effectMask;

    /**
     * Return the effect set representing the current effect set unioned with the given effect set.
     * Provided in addition to the varargs one for performance reasons.
     *
     * @param effectSet An EffectSet to add to this one.
     * @return The effect set representing the union of the effects of this set with the argument.
     */
    public EffectSet union(EffectSet effectSet) {
        if (effectSet == null) {
            return this;
        }

        return sEffectSets[effectMask | effectSet.effectMask];
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

        int mask = effectMask;
        for (int i = 0; i < effectSets.length; i++) {
            mask |= effectSets[i].effectMask;
        }

        return sEffectSets[mask];
    }

    /**
     * Return the effect set representing the current effect set plus the given effect.
     *
     * @param effect An effect to add to this EffectSet.
     * @return The effect set representing the effects of this EffectSet plus the given extra one.
     */
    public EffectSet union(Effects effect) {
        if (effect == null) {
            return this;
        }

        return sEffectSets[effectMask | effect.maskValue];
    }

    /**
     * Return the EffectSet resulting from removing the specified Effect from this EffectSet.
     *
     * @param effect The Effect to remove from this EffectSet
     * @return The resulting EffectSet
     */
    public EffectSet subtract(Effects effect) {
        return sEffectSets[effectMask ^ effect.maskValue];
    }

    public EffectSet subtract(EffectSet effectSet) {
        return sEffectSets[effectMask ^ effectSet.effectMask];
    }

    private EffectSet(final int mask) {
        effectMask = mask;
    }
}
