package joust.optimisers.translators;

import com.sun.tools.javac.code.Symbol;
import joust.optimisers.evaluation.Value;
import joust.utils.TreeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A translator that takes the mappings from an ExecutionContext and replaces identifiers in the tree of interest
 * with known literal replacements where possible (according to the context mappings provided).
 */
public @AllArgsConstructor @Log4j2
class ContextInliningTranslator extends BaseTranslator {
    HashMap<VarSymbol, Value> currentAssignments;

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);
        log.info("Inliner visiting ident: {}", tree);

        Symbol sym = tree.sym;
        log.info("Symbol: {}", sym);
        if (!(sym instanceof VarSymbol)) {
            log.info("Abort: nonvar symbol");
            return;
        }
        log.info("Sym: {}", sym.hashCode());

        Value knownValue = currentAssignments.get(sym);
        log.info("knownValue: {}", knownValue);
        if (knownValue == null || knownValue == Value.UNKNOWN) {
            return;
        }

        result = knownValue.toLiteral();
        log.info("Replacing {} with {}", tree, result);
    }
}
