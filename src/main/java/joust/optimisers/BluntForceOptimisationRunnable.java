package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.BaseTranslator;
import joust.optimisers.utils.OptimisationRunnable;
import joust.utils.LogUtils;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

public @Log4j2 @AllArgsConstructor class BluntForceOptimisationRunnable implements OptimisationRunnable {
    private Class<? extends BaseTranslator> mTranslatorClass;

    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            // While constant folding is making a change, continue applying it.
            BaseTranslator translator;

            do {
                try {
                    translator = mTranslatorClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    LogUtils.raiseCompilerError("Exception thrown from BluntForceOptimisationRunnable while instantiating translator: " + e);
                    return;
                }

                tree.accept(translator);
            } while (translator.makingChanges());
        }
    }
}
