package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.avail.Avail;
import joust.optimisers.utils.OptimisationRunnable;

/**
 * Perform available expressiona analysis phase.
 * // TODO: Do this on-demand. This runnable is really only just a bodge!
 */
public class AvailableExpr implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            Avail a  = new Avail();
            tree.accept(a);
        }
    }
}
