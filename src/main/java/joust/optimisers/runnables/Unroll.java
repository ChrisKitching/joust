package joust.optimisers.runnables;

import joust.optimisers.translators.UnrollTranslator;

public class Unroll extends OptimisationRunnable.OneShot {
    public Unroll() {
        super(UnrollTranslator.class);
    }
}
