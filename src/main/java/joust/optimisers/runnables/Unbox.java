package joust.optimisers.runnables;

import joust.optimisers.unbox.UnboxingTranslator;

public class Unbox extends OptimisationRunnable.BluntForce {
    public Unbox() {
        super(UnboxingTranslator.class);
    }
}
