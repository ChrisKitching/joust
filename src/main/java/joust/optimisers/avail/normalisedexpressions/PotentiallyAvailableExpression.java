package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;

import java.util.HashSet;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A potentially available expression is an expression that can be made available at a point in the
 * flowgraph by extracting a local variable (Or is already so available through merit of being
 * assigned to some variable).
 * Expressions here are always single-operation. Either a unary or binary.
 */
public abstract class PotentiallyAvailableExpression {
    // A placeholder for the temporary value this expression is stored in. There may not actually
    public PossibleSymbol virtualSym = new PossibleSymbol();
    public PossibleSymbol concreteSym;

    // The dependencies of this and all expressions it depends on.
    public Set<PossibleSymbol> deps = new HashSet<>();

    // The node from which this PAE was deduced.
    public AJCExpression sourceNode;

    // A computed node, possibly equivalent to sourceNode, that properly represents this expression (After
    // coalescing its various virtual dependent expressions.)
    public AJCSymbolRefTree<VarSymbol> expressionNode;

    /**
     * Set the PossibleSymbol of the PAE to the concrete symbol, s.
     * @param s The concrete symbol this PAE is stored in.
     */
    public void setActualSymbol(VarSymbol s) {
        concreteSym = PossibleSymbol.getConcrete(s);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Expr(");
        sb.append(sourceNode);
        sb.append(':');
        if (sourceNode != null) {
            sb.append(sourceNode.getClass().getSimpleName());
        }
        sb.append(", V:");
        sb.append(virtualSym);
        sb.append(", C:");
        sb.append(concreteSym);
        sb.append(", Deps(");
        for (PossibleSymbol d : deps) {
            sb.append(d.toString());
            sb.append(", ");
        }
        sb.append("))\n");

        return sb.toString();
    }

    /**
     * Get the list of statements necessary to put this PAE into a temporary variable, set expressionNode
     * to a reference to the new temporary, and return the list of required statements.
     */
    public abstract List<AJCStatement> concretify(Symbol owningContext);
}
