package joust.optimisers.avail.normalisedexpressions;

import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

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
            sym = PossibleSymbol.getConcrete(s);
        } else {
            sym = new PossibleSymbol();
        }

        deps.add(sym);
    }

    public PotentiallyAvailableNullary(JCExpression e) {
        this(e, null);
    }
}
