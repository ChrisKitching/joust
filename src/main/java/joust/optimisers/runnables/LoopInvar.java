package joust.optimisers.runnables;

import joust.optimisers.translators.LoopInvarTranslator;
import joust.optimisers.translators.UnusedAssignmentStripper;
import joust.tree.annotatedtree.AJCTree;

/**
 * Loop invariant code motion.
 */
public class LoopInvar extends OptimisationRunnable.SingleTranslatorInstance {
    private UnusedAssignmentStripper stripper = new UnusedAssignmentStripper();

    public LoopInvar() {
        super(LoopInvarTranslator.class);
    }

    @Override
    protected void processRootNode(AJCTree node) {
        do {
            // Loop invariant code motion...
            translatorInstance.visit(node);

            // Unused assignment stripping... (Bins some of the junk Invar produces).
            // (Note that a single pass of this is applied before we get to this point anyway.)
            stripper.visit(node);
        } while (translatorInstance.makingChanges());
    }
}
