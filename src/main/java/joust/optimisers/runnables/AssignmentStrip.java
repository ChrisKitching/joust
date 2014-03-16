package joust.optimisers.runnables;

import joust.optimisers.translators.UnusedAssignmentStripper;

/**
 * Stripping unncessary assignments (via LVA).
 */
public class AssignmentStrip extends OptimisationRunnable.OneShot {
    public AssignmentStrip() {
        super(UnusedAssignmentStripper.class);
    }
}
