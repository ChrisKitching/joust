package joust.optimisers;

import joust.optimisers.translators.UnusedAssignmentStripper;
import lombok.extern.log4j.Log4j2;

/**
 * Loop invariant code motion.
 */
public @Log4j2
class AssignmentStrip extends OneShotOptimisationRunnable {
    public AssignmentStrip() {
        super(UnusedAssignmentStripper.class);
    }
}
