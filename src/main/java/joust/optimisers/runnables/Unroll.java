package joust.optimisers.runnables;

import joust.optimisers.translators.UnrollTranslator;
import joust.optimisers.translators.UnusedAssignmentStripper;

public class Unroll extends OptimisationRunnable.OneTwo {
    public Unroll() {
        super(UnrollTranslator.class, UnusedAssignmentStripper.class);
    }
}
