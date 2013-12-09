package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.utils.OptimisationRunnable;
import joust.optimisers.visitors.SideEffectVisitor;

/**
 * An optimisation to run after annotation processing that strips all assertions from the trees.
 * Default value of the option is to not strip assertions.
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
