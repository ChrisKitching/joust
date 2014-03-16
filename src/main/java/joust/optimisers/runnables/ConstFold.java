package joust.optimisers.runnables;

import joust.optimisers.translators.ConstFoldTranslator;

/**
 * Constant folding.
 */
public class ConstFold extends OptimisationRunnable.BluntForce {
    public ConstFold() {
        super(ConstFoldTranslator.class);
    }
}
