package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.AssertionStrippingTranslator;
import joust.optimisers.translators.ConstFoldTranslator;
import joust.optimisers.translators.ExpressionNormalisingTranslator;
import joust.optimisers.utils.OptimisationRunnable;

public class ExpressionNormaliser implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            // While constant folding is making a change, continue applying it.
            ExpressionNormalisingTranslator stripper;
            do {
                stripper = new ExpressionNormalisingTranslator();
                tree.accept(stripper);
            } while (stripper.makingChanges());
        }
    }
}
