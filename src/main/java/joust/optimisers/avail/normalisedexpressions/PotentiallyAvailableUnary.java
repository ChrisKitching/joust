package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.Optimiser.treeMaker;

public class PotentiallyAvailableUnary extends PotentiallyAvailableExpression {
    public PotentiallyAvailableExpression operand;
    public Tag opcode;

    /**
     * Create a PotentiallyAvailableUnary from the given JCUnary.
     */
    public PotentiallyAvailableUnary(PotentiallyAvailableExpression target, Tag op) {
        super();
        operand = target;
        opcode = op;

        deps.add(virtualSym);
        deps.addAll(target.deps);
    }

    @Override
    public List<JCStatement> concretify(Symbol owningContext) {
        List<JCStatement> neededStatements = operand.concretify(owningContext);

        // Create a node representing this expression...
        JCUnary thisExpr = treeMaker.Unary(opcode, operand.expressionNode);
        thisExpr.operator = ((JCUnary) sourceNode).operator;
        thisExpr.type = sourceNode.type;

        // Create a new temporary variable to hold this expression.
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, sourceNode.type, owningContext);
        concreteSym = PossibleSymbol.getConcrete(newSym);
        expressionNode = treeMaker.Ident(newSym);

        JCVariableDecl newDecl = treeMaker.VarDef(newSym, thisExpr);

        return neededStatements.append(newDecl);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PotentiallyAvailableUnary)) {
            return false;
        }

        PotentiallyAvailableUnary cast = (PotentiallyAvailableUnary) obj;
        if (!opcode.equals(cast.opcode)) {
            return false;
        }

        return operand.equals(cast.operand);
    }
}
