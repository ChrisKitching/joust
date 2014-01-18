package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.TreeSanityInducingTranslator;
import joust.optimisers.utils.OptimisationRunnable;

public class ExpressionNormaliser implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            // While constant folding is making a change, continue applying it.
            TreeSanityInducingTranslator stripper;
            do {
                stripper = new TreeSanityInducingTranslator();
                tree.accept(stripper);
            } while (stripper.makingChanges());
        }
    }
}
