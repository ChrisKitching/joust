package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.io.StringWriter;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.Optimiser.treeMaker;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A representation of a JCBinary allowing for the use of hypothetical symbols.
 */
public @Log4j2
class PotentiallyAvailableBinary extends PotentiallyAvailableExpression {
    public PotentiallyAvailableExpression lhs;
    public PotentiallyAvailableExpression rhs;
    public Tag opcode;

    public PotentiallyAvailableBinary(PotentiallyAvailableExpression l, PotentiallyAvailableExpression r, Tag op) {
        super();
        lhs = l;
        rhs = r;
        opcode = op;

        deps.add(virtualSym);
        // This expression depends on all the dependencies of the subexpressions.
        deps.addAll(l.deps);
        deps.addAll(r.deps);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Expr((");
        sb.append(lhs.sourceNode);
        sb.append(") ");
        sb.append((new Pretty(new StringWriter(), false)).operatorName(opcode));
        sb.append(" (");
        sb.append(rhs.sourceNode);
        sb.append("), ");
        sb.append(virtualSym.toString());
        sb.append(", Deps(");
        for (PossibleSymbol d : deps) {
            sb.append(d.toString());
            sb.append(", ");
        }
        sb.append("))\n");

        return sb.toString();
    }

    @Override
    public List<JCStatement> concretify(Symbol owningContext) {
        List<JCStatement> lhsStatements = lhs.concretify(owningContext);
        List<JCStatement> rhsStatements = rhs.concretify(owningContext);

        lhsStatements = lhsStatements.appendList(rhsStatements);

        log.debug("Concretifying {}\nlhs:{}\nrhs:{}", this, lhsStatements, rhsStatements);

        // lhsStatements now holds the list of statements setting up temporaries for the dependancies of this expression.
        // Our turn!

        // Create a node representing this binary expression.. (lhsTemp OP rhsTemp).
        JCBinary thisExpr = treeMaker.Binary(opcode, lhs.expressionNode, rhs.expressionNode);
        // Pilfer the operator and type from the source node...
        // This is safe only if the source node is removed from the tree.
        thisExpr.operator = ((JCBinary) sourceNode).operator;
        thisExpr.type = sourceNode.type;

        log.debug("thisExpr:{}", thisExpr);

        // Create a new temporary variable to hold this expression.
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, sourceNode.type, owningContext);
        concreteSym = PossibleSymbol.getConcrete(newSym);
        expressionNode = treeMaker.Ident(newSym);

        JCVariableDecl newDecl = treeMaker.VarDef(newSym, thisExpr);
        log.debug("newDecl:{}", newDecl);

        return lhsStatements.append(newDecl);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PotentiallyAvailableBinary)) {
            return false;
        }

        PotentiallyAvailableBinary cast = (PotentiallyAvailableBinary) obj;
        if (!opcode.equals(cast.opcode)) {
            return false;
        }

        // If the operands match, the expressions are the same.
        if (lhs.equals(cast.lhs) && rhs.equals(cast.rhs)) {
            return true;
        }

        // Otherwise, check for commutativity. We allow things either way round in such cases, of course.
        if (!TreeUtils.operatorIsCommutative(opcode)) {
            return false;
        }

        // If it commutes, check if things match up the other way around.
        return lhs.equals(cast.rhs) && rhs.equals(cast.lhs);
    }


}
