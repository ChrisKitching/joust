package joust.optimisers.runnables;

import com.sun.tools.javac.tree.JCTree;
import joust.optimisers.translators.AssertionStrippingTranslator;

import static joust.JOUST.conventionalTrees;

/**
 * Assertion stripping.
 */
public class AssertionStrip extends OptimisationRunnable {
    private final AssertionStrippingTranslator stripper = new AssertionStrippingTranslator();

    @Override
    public void run() {
        for (JCTree.JCCompilationUnit tree : conventionalTrees) {
            tree.accept(stripper);
        }
    }
}
