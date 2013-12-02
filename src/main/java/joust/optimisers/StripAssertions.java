package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.OptimiserOptions;
import joust.optimisers.translators.AssertionStrippingTranslator;
import joust.optimisers.utils.OptimisationRunnable;

/**
 * An optimisation to run after annotation processing that strips all assertions from the trees.
 * Default value of the option is to not strip assertions.
 */
public class StripAssertions implements OptimisationRunnable {
    @Override
    public void run() {
        if (!OptimiserOptions.stripAssertions) {
            return;
        }

        for (JCTree tree : Optimiser.elementTrees) {
            AssertionStrippingTranslator stripper = new AssertionStrippingTranslator();
            tree.accept(stripper);
        }
    }
}
