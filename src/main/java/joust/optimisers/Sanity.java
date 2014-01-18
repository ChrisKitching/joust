package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.ConstFoldTranslator;
import joust.optimisers.translators.TreeSanityInducingTranslator;
import joust.optimisers.utils.OptimisationRunnable;

public class Sanity implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            TreeSanityInducingTranslator constFold;
            do {
                constFold = new TreeSanityInducingTranslator();
                tree.accept(constFold);
            } while (constFold.makingChanges());
        }
    }
}
