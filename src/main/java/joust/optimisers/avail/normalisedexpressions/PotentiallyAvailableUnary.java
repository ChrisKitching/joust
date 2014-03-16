package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

public class PotentiallyAvailableUnary extends PotentiallyAvailableExpression {
    public PotentiallyAvailableExpression operand;
    public Tag opcode;

    /**
     * Create a PotentiallyAvailableUnary from the given JCUnary.
     */
    public PotentiallyAvailableUnary(PotentiallyAvailableExpression target, Tag op) {
        operand = target;
        opcode = op;

        deps.add(virtualSym);
        deps.addAll(target.deps);
    }

    @Override
    public List<AJCStatement> concretify(Symbol owningContext) {
        List<AJCStatement> neededStatements = operand.concretify(owningContext);

        // Create a node representing this expression...
        AJCUnary thisExpr = treeMaker.Unary(opcode, operand.expressionNode);
        // TODO: Hide this plumbing...
        thisExpr.getDecoratedTree().operator = ((AJCUnary) sourceNode).getDecoratedTree().operator;
        thisExpr.getDecoratedTree().type = ((AJCUnary) sourceNode).getDecoratedTree().type;

        // Create a new temporary variable to hold this expression.
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, sourceNode.getType(), owningContext);
        concreteSym = PossibleSymbol.getConcrete(newSym);
        expressionNode = treeMaker.Ident(newSym);

        //TODO
        AJCVariableDecl newDecl = null;//treeMaker.VarDef(newSym, thisExpr);

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
