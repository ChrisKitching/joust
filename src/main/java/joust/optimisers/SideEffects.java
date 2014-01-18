package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.utils.OptimisationRunnable;
import joust.optimisers.visitors.SideEffectVisitor;

/**
 * Runnable for the side effect annotator.
 */
public class SideEffects implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            SideEffectVisitor visitor = new SideEffectVisitor();
            tree.accept(visitor);
        }
    }
}
