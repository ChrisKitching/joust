package joust.optimisers;

import joust.optimisers.translators.ConstFoldTranslator;

/**
 * An optimisation to run after annotation processing that strips all assertions from the trees.
 */
public class ConstFold extends BluntForceOptimisationRunnable {
    public ConstFold() {
        super(ConstFoldTranslator.class);
    }
}
