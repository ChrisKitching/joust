package joust.optimisers.runnables;

import joust.optimisers.cse.CommonSubExpressionTranslator;

public class CSE extends OptimisationRunnable.BluntForce {
    public CSE() {
        super(CommonSubExpressionTranslator.class);
    }
}
