package joust.optimisers.runnables;

import joust.optimisers.illegaloverride.IllegalOverrideVisitor;

public class IllegalOverrideDetector extends OptimisationRunnable.OneShot {
    public IllegalOverrideDetector() {
        super(new IllegalOverrideVisitor());
    }
}
