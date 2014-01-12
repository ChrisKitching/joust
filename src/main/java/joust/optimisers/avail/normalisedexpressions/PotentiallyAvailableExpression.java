package joust.optimisers.avail.normalisedexpressions;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A potentially available expression is an expression that can be made available at a point in the
 * flowgraph by extracting a local variable (Or is already so available through merit of being
 * assigned to some variable).
 * Expressions here are always single-operation. Either a unary or binary.
 */
public abstract class PotentiallyAvailableExpression {
    // A placeholder for the temporary value this expression is stored in. There may not actually
    public PossibleSymbol sym;

    // The dependencies of this and all expressions it depends on.
    public Set<PossibleSymbol> deps = new HashSet<>();

    public JCExpression node;

    public PotentiallyAvailableExpression() {
        sym = new PossibleSymbol();
    }

    /**
     * Set the PossibleSymbol of the PAE to the concrete symbol, s.
     * @param s The concrete symbol this PAE is stored in.
     */
    public void setActualSymbol(VarSymbol s) {
        PossibleSymbol candidate = PossibleSymbol.getConcrete(s);
        sym.setSym(s);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Expr(");
        sb.append(node);
        sb.append(':');
        if (node != null) {
            sb.append(node.getClass().getSimpleName());
        }
        sb.append(", ");
        sb.append(sym.toString());
        sb.append(", Deps(");
        for (PossibleSymbol d : deps) {
            sb.append(d.toString());
            sb.append(", ");
        }
        sb.append("))");

        return sb.toString();
    }
}
