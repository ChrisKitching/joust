package joust.optimisers.runnables;

import joust.optimisers.unbox.UnboxingFunctionTemplates;
import joust.optimisers.unbox.UnboxingTranslator;

public class Unbox extends OptimisationRunnable.BluntForce {
    public Unbox() {
        super(new UnboxingTranslator());
    }

    @Override
    public void run() {
        UnboxingFunctionTemplates.init();
        super.run();
    }
}
