package joust.optimisers.runnables;

import joust.optimisers.translators.ConstFoldTranslator;
import joust.optimisers.unroll.UnrollTranslator;

/**
 * Loop unrolling.
 */
public class Unroll extends OptimisationRunnable.OneTwo {
    public Unroll() {
        super(UnrollTranslator.class, ConstFoldTranslator.class);
    }
}
