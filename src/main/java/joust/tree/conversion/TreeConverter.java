package joust.tree.conversion;

import joust.JOUST;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.tree.annotatedtree.AJCForest;

/**
 * An OptimisationRunnable used to invoke the tree converter.
 */
public class TreeConverter extends OptimisationRunnable {
    @Override
    public void run() {
        // Build the enhanced tree... (Hopefully they didn't sneakily copy the tree since we stashed the pointer...)
        AJCForest.init(JOUST.environmentsToProcess);
    }
}
