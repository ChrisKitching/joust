package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.UnrollTranslator;
import joust.optimisers.utils.OptimisationRunnable;
import lombok.extern.log4j.Log4j2;

public @Log4j2 class Unroll implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            UnrollTranslator translator = new UnrollTranslator();
            tree.accept(translator);
        }
    }
}
