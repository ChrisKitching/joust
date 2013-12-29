package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.AssertionStrippingTranslator;
import joust.optimisers.translators.ExpressionNormalisingTranslator;
import joust.optimisers.utils.OptimisationRunnable;

public class ExpressionNormaliser implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            ExpressionNormalisingTranslator stripper = new ExpressionNormalisingTranslator();
            tree.accept(stripper);
        }
    }
}
