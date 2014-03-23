package joust.optimisers.runnables;

import joust.optimisers.translators.LoopInvarTranslator;
import joust.optimisers.translators.UnusedAssignmentStripper;
import joust.tree.annotatedtree.AJCTree;

/**
 * Loop invariant code motion.
 */
public class LoopInvar extends OptimisationRunnable.OneTwo {
    public LoopInvar() {
        super(LoopInvarTranslator.class, UnusedAssignmentStripper.class);
    }
}
