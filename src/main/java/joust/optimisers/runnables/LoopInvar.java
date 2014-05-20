package joust.optimisers.runnables;

import joust.optimisers.invar.LoopInvarTranslator;

/**
 * Loop invariant code motion.
 */
public class LoopInvar extends OptimisationRunnable.BluntForce {
    public LoopInvar() {
        super(new LoopInvarTranslator());
    }
}
