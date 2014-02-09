package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.utils.OptimisationRunnable;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;

/**
 * Runnable for the side effect annotator.
 */
public class SideEffects implements OptimisationRunnable {
    @Override
    public void run() {
        SideEffectVisitor visitor = new SideEffectVisitor();
        for (JCTree tree : Optimiser.elementTrees) {
            tree.accept(visitor);
            visitor.clearMarked();
        }

        visitor.finaliseIncompleteEffectSets();
    }
}
