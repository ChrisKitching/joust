package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.LoopInvarTranslator;
import lombok.extern.log4j.Log4j2;

/**
 * Loop invariant code motion.
 */
public @Log4j2
class LoopInvar extends BluntForceOptimisationRunnable {
    public LoopInvar() {
        super(LoopInvarTranslator.class);
    }
}
