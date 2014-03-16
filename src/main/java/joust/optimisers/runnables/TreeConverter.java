package joust.optimisers.runnables;

import joust.Optimiser;
import joust.tree.annotatedtree.AJCForest;

public class TreeConverter extends OptimisationRunnable {
    @Override
    public void run() {
        // Build the enhanced tree... (Hopefully they didn't sneakily copy the tree since we stashed the pointer...)
        AJCForest.init(Optimiser.conventionalTrees);
    }
}
