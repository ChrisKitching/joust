package joust.optimisers.runnables;

import joust.optimisers.unroll.UnrollTranslator;

/**
 * Loop unrolling.
 */
public class Unroll extends OptimisationRunnable.BluntForce {
    public Unroll() {
        super(UnrollTranslator.class);
    }
}
