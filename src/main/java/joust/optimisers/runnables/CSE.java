package joust.optimisers.runnables;

import joust.optimisers.cse.CommonSubExpressionTranslator;

/**
 * Common subexpression elimination.
 */
public class CSE extends OptimisationRunnable.BluntForce {
    public CSE() {
        super(new CommonSubExpressionTranslator());
    }
}
