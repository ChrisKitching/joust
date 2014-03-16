package joust.optimisers;

import joust.optimisers.translators.ExpressionNormalisingTranslator;

public class ExpressionNormalise extends BluntForceOptimisationRunnable {
    public ExpressionNormalise() {
        super(ExpressionNormalisingTranslator.class);
    }
}