package joust.optimisers.unroll;

import com.sun.tools.javac.code.Symbol;
import joust.utils.tree.evaluation.Value;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A translator that takes the mappings from an ExecutionContext and replaces identifiers in the tree of interest
 * with known literal replacements where possible (according to the context mappings provided).
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@AllArgsConstructor
public class ContextInliningTranslator extends BaseTranslator {
    HashMap<VarSymbol, Value> currentAssignments;

    @Override
    public void visitIdent(AJCIdent tree) {
        super.visitIdent(tree);

        Symbol sym = tree.getTargetSymbol();
        if (!(sym instanceof VarSymbol)) {
            return;
        }

        Value knownValue = currentAssignments.get(sym);
        if (knownValue == null || knownValue == Value.UNKNOWN) {
            return;
        }

        AJCLiteral result = knownValue.toLiteral();
        tree.swapFor(result);
        log.debug("Replacing {} with {}", tree, result);
    }
}
