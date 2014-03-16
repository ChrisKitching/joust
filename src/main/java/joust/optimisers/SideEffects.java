package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.utils.OptimisationRunnable;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.tree.annotatedtree.AJCTree;

import static joust.Optimiser.inputTrees;

/**
 * Runnable for the side effect annotator.
 */
public class SideEffects implements OptimisationRunnable {
    @Override
    public void run() {
        SideEffectVisitor visitor = new SideEffectVisitor();
        for (AJCTree tree : inputTrees.rootNodes) {
            visitor.visit(tree);
        }

        visitor.finaliseIncompleteEffectSets();
    }
}
