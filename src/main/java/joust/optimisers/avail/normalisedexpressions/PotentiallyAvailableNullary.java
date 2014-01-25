package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.Optimiser.treeMaker;

/**
 * An ident, field access, or constant. Or something. Representative in some way of a thing which
 * refers to another thing. And stuff.
 */
public @Log4j2 class PotentiallyAvailableNullary extends PotentiallyAvailableExpression {
    JCExpression expr;

    public PotentiallyAvailableNullary(JCExpression e, VarSymbol s) {
        expr = e;

        if (e instanceof JCIdent) {
            JCIdent ident = (JCIdent) e;
            deps.add(PossibleSymbol.getConcrete((VarSymbol) ident.sym));
        } else if (e instanceof JCFieldAccess) {
            JCFieldAccess acc = (JCFieldAccess) e;
            deps.add(PossibleSymbol.getConcrete((VarSymbol) acc.sym));
        }

        if (s != null) {
            concreteSym = PossibleSymbol.getConcrete(s);
            virtualSym = concreteSym;
        }

        deps.add(virtualSym);
    }

    @Override
    public List<JCStatement> concretify(Symbol owningContext) {
        expressionNode = expr;

        return List.nil();
    }

    public PotentiallyAvailableNullary(JCExpression e) {
        this(e, null);
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

        VarSymbol mySym = TreeUtils.getTargetSymbolForExpression(expr);
        VarSymbol herSym = TreeUtils.getTargetSymbolForExpression(cast.expr);

        return mySym.equals(herSym);
    }
}
