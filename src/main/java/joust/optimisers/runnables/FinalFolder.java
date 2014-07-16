package joust.optimisers.runnables;


import joust.optimisers.finalfold.FinalValueFinder;
import joust.optimisers.unroll.ContextInliningTranslator;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class FinalFolder extends OptimisationRunnable {
    @Override
    public void run() {
        FinalValueFinder finder = new FinalValueFinder();

        for (AJCTree tree : AJCForest.getInstance().rootNodes) {
            finder.visitTree(tree);
        }

        ContextInliningTranslator inliner = new ContextInliningTranslator(finder.values);

        for (AJCTree tree : AJCForest.getInstance().rootNodes) {
            inliner.visitTree(tree);
        }

        if (inliner.makingChanges()) {
            AJCForest.getInstance().initialAnalysis();
        }
    }

    @Override
    public String getName() {
        return "FinalFolder";
    }
}
