package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.ConstFoldTranslator;
import joust.optimisers.utils.OptimisationRunnable;

/**
 * An optimisation to run after annotation processing that strips all assertions from the trees.
 */
public class ConstFold implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            // While constant folding is making a change, continue applying it.
            ConstFoldTranslator constFold;
            do {
                constFold = new ConstFoldTranslator();
                tree.accept(constFold);
            } while (constFold.makingChanges());
        }
    }
}
