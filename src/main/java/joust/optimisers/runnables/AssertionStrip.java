package joust.optimisers.runnables;

import com.sun.tools.javac.tree.JCTree;
import joust.optimisers.translators.AssertionStrippingTranslator;
import lombok.extern.java.Log;

import static joust.JOUST.conventionalTrees;

/**
 * Assertion stripping.
 */
@Log
public class AssertionStrip extends OptimisationRunnable {
    private final AssertionStrippingTranslator stripper = new AssertionStrippingTranslator();

    @Override
    public void run() {
        for (JCTree.JCCompilationUnit tree : conventionalTrees) {
            log.info("Running AssertionStrip.");
            tree.accept(stripper);
        }
    }

    @Override
    public String getName() {
        return "AssertionStrip";
    }
}
