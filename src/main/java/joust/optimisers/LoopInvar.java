package joust.optimisers;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.optimisers.translators.LoopInvarTranslator;
import joust.optimisers.translators.UnusedAssignmentStripper;
import joust.optimisers.utils.OptimisationRunnable;
import lombok.extern.log4j.Log4j2;

/**
 * Loop invariant code motion.
 */
public @Log4j2
class LoopInvar implements OptimisationRunnable {
    @Override
    public void run() {
        for (JCTree tree : Optimiser.elementTrees) {
            // While we're making progress, repeat.
            LoopInvarTranslator translator;

            do {
                // Loop invariant code motion...
                translator = new LoopInvarTranslator();
                tree.accept(translator);

                // Unused assignment stripping... (Bins some of the junk Invar produces).
                // (Note that a single pass of this is applied before we get to this point anyway.)
                UnusedAssignmentStripper stripper = new UnusedAssignmentStripper();
                tree.accept(stripper);
            } while (translator.makingChanges());
        }
    }
}
