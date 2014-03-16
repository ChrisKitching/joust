package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * An ident, field access, or constant. Or something. Representative in some way of a thing which
 * refers to another thing. And stuff.
 */
@Log4j2
public class PotentiallyAvailableNullary extends PotentiallyAvailableExpression {
    AJCSymbolRefTree<VarSymbol> expr;

    /**
     * Construct with an ident or a field access.
     */
    public PotentiallyAvailableNullary(AJCSymbolRefTree<VarSymbol> e) {
        expr = e;

        concreteSym = PossibleSymbol.getConcrete(e.getTargetSymbol());
        virtualSym = concreteSym;

        deps.add(virtualSym);
    }

    @Override
    public List<AJCStatement> concretify(Symbol owningContext) {
        expressionNode = expr;

        return List.nil();
    }

    /**
     * Construct from a literal. We have no deps, no reference, nothing. Just a stub.
     */
    public PotentiallyAvailableNullary(AJCLiteral e) {
        sourceNode = e;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PotentiallyAvailableNullary)) {
            return false;
        }

        PotentiallyAvailableNullary cast = (PotentiallyAvailableNullary) obj;

        if (expr != null) {
            if (cast.expr == null) {
                return false;
            }

            return expr.getTargetSymbol().equals(cast.expr.getTargetSymbol());
        } else if (cast.expr != null) {
            return false;
        }

        log.warn("Unable to differentiate between PANs: {} or {}", this, cast);

        return false;
    }
}
